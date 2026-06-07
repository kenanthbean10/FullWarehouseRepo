package com.example.goldenhosewarehouse.dal.service.impl;

import com.example.goldenhosewarehouse.dal.domain.DataEntity;
import com.example.goldenhosewarehouse.dal.dto.TimeSeriesRecordDto;
import com.example.goldenhosewarehouse.dal.dto.TimeSeriesResponse;
import com.example.goldenhosewarehouse.dal.repository.DataKey;
import com.example.goldenhosewarehouse.dal.repository.DataRepository;
import com.example.goldenhosewarehouse.dal.service.DataService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Stream;

@Service
public class DataServiceImpl implements DataService {
private DataRepository dataRepository;
    private final ObjectMapper NdjsonobjectMapper;
    public DataServiceImpl(DataRepository dataRepository,ObjectMapper NdjsonobjectMapper)
    {
        this.dataRepository = dataRepository;
        this.NdjsonobjectMapper = NdjsonobjectMapper;

    }

    @Override
    public Iterable<DataEntity> getTimeSeries(String assetId, String sourceId) {
    //create the composite key required by the DAL
        DataKey key = new DataKey(assetId, sourceId);
        return dataRepository.findAll(key);

    }
//filtered time-series analysis
    //so we can generate reports for specific months or year
    @Override
    public Iterable<DataEntity> getTimeSeriesByRange(String assetId, String sourceId, LocalDate start, LocalDate end) {
        DataKey key = new DataKey(assetId, sourceId);
        return dataRepository.findByRange(key, start, end);
    }
/**
 * [Q5] Implementation to retrieve full historical data.
 * It encapsulates the creation of DataKey to keep the Controller clean.
 */
@Override
public DataEntity saveData(DataEntity dataRecord) {

    if (dataRecord.getSystemDate() == null) {
        dataRecord.setSystemDate(java.time.Instant.now());


    }
    // يمكنك إضافة Business Logic هنا (مثلاً: التحقق من صحة القيمة قبل الحفظ)
    return dataRepository.save(dataRecord);
}
    // الميثود الجديدة لتنفيذ الـ Pagination
    @Override
    public Slice<DataEntity> getAllDataPaged(int page, int size) {
        // نمرر طلب الصفحة إلى الـ Repository اليدوي الذي يحتوي على ميثود findAll(Pageable)
        return dataRepository.findAll(PageRequest.of(page, size));
    }
    //get Timeseries
//    @Override
//    public TimeSeriesResponse getTimeSeriesFormatted(
//            String assetId, String dataSourceId,
//            LocalDate startDate, LocalDate endDate,
//            boolean includeAttributes, int page, int size) {
//
//        // JOB 1: fetch one page from Cassandra
//        // PageRequest.of(page, size) = "give me page X with Y items"
//        Slice<DataEntity> slice = dataRepository.findByDateRange(
//                assetId, dataSourceId,
//                startDate, endDate,
//                PageRequest.of(page, size)
//        );
//
//        // JOB : deduplicate + build records
//        // Same logic as streaming — first row per date = newest
//        Set<LocalDate> seenDates = new HashSet<>();
//        List<TimeSeriesRecordDto> records = new ArrayList<>();
//        Set<String> attributeKeys = new LinkedHashSet<>(); // tracks all attribute names
//
//        slice.forEach(entity -> {
//            LocalDate bDate = entity.getBusinessDate();
//
//            // skip if we already processed this date
//            if (!seenDates.contains(bDate)) {
//                seenDates.add(bDate);
//
//                // merge all 3 value maps into one Map<String, Object>
//                Map<String, Object> merged = new LinkedHashMap<>();
//
//                if (entity.getValuesdouble() != null) {
//                    entity.getValuesdouble().forEach((k, v) -> {
//                        merged.put(k, v);
//                        attributeKeys.add(k); // track attribute name
//                    });
//                }
//                if (entity.getValuesInt() != null) {
//                    entity.getValuesInt().forEach((k, v) -> {
//                        merged.put(k, v);
//                        attributeKeys.add(k);
//                    });
//                }
//                if (entity.getValuesText() != null) {
//                    entity.getValuesText().forEach((k, v) -> {
//                        merged.put(k, v);
//                        attributeKeys.add(k);
//                    });
//                }
//
//                // create one record for this date
//                records.add(new TimeSeriesRecordDto(bDate, merged));
//            }
//        });
//
//        // JOB 3: build the response
//        TimeSeriesResponse.DataBlock dataBlock =
//                new TimeSeriesResponse.DataBlock(assetId, dataSourceId, records);
//
//        // only include attributes list if client asked for it
//        Set<String> attrs = includeAttributes ? attributeKeys : null;
//
//        return new TimeSeriesResponse(dataBlock, attrs);
//    }

    @Override
    public TimeSeriesResponse getTimeSeriesFormatted(
            String assetId, String dataSourceId,
            LocalDate startDate, LocalDate endDate,
            boolean includeAttributes, int page, int size) {

        // Step 1: Collect all distinct records using safe sequential paging
        List<TimeSeriesRecordDto> allDistinct = new ArrayList<>();
        Set<LocalDate> seenDates = new HashSet<>();
        Set<String> attributeKeys = new LinkedHashSet<>();

        Pageable pageable = PageRequest.of(0, 5000); // initial request, page 0, large chunk
        Slice<DataEntity> slice;
        do {
            slice = dataRepository.findByDateRange(assetId, dataSourceId, startDate, endDate, pageable);
            for (DataEntity entity : slice.getContent()) {
                LocalDate bDate = entity.getBusinessDate();
                if (!seenDates.contains(bDate)) {
                    seenDates.add(bDate);

                    Map<String, Object> merged = new LinkedHashMap<>();
                    if (entity.getValuesdouble() != null) {
                        entity.getValuesdouble().forEach((k, v) -> { merged.put(k, v); attributeKeys.add(k); });
                    }
                    if (entity.getValuesInt() != null) {
                        entity.getValuesInt().forEach((k, v) -> { merged.put(k, v); attributeKeys.add(k); });
                    }
                    if (entity.getValuesText() != null) {
                        entity.getValuesText().forEach((k, v) -> { merged.put(k, v); attributeKeys.add(k); });
                    }
                    allDistinct.add(new TimeSeriesRecordDto(bDate, merged));
                }
            }
            // Use the paging state from the slice for the next request
            pageable = slice.hasNext() ? slice.nextPageable() : null;
        } while (pageable != null);

        // Step 2: In‑memory pagination (slice the already‑collected list)
        int fromIndex = page * size;
        int toIndex = Math.min(fromIndex + size, allDistinct.size());
        List<TimeSeriesRecordDto> pagedRecords = (fromIndex < allDistinct.size())
                ? allDistinct.subList(fromIndex, toIndex)
                : Collections.emptyList();

        // Step 3: Build the response
        TimeSeriesResponse.DataBlock dataBlock = new TimeSeriesResponse.DataBlock(assetId, dataSourceId, pagedRecords);
        Set<String> attrs = includeAttributes ? attributeKeys : null;
        return new TimeSeriesResponse(dataBlock, attrs);
    }

    // CHOICE 1: Specific Pagination (Asset & Source)
    public Slice<DataEntity> getPaginatedData(String assetId, String sourceId, int page, int size) {
        return dataRepository.findByAssetIdAndDataSourceId(assetId, sourceId, PageRequest.of(page, size));
    }
    public void streamTimeSeries(String assetId, String sourceId, OutputStream out) {
        try (Stream<DataEntity> dataStream = dataRepository
                .streamByAssetIdAndDataSourceId(assetId, sourceId)) {

            Set<LocalDate> seenDates = new HashSet<>();

            dataStream.forEach(entity -> {
                LocalDate businessDate = entity.getBusinessDate(); //

                if (!seenDates.contains(businessDate)) {
                    try {                                                    //
                        String jsonLine = NdjsonobjectMapper
                                .writeValueAsString(entity) + "\n";
                        out.write(jsonLine.getBytes(StandardCharsets.UTF_8));
                        out.flush();
                        seenDates.add(businessDate);
                    } catch (IOException e) {
                        throw new RuntimeException("Client disconnected", e);
                    }
                }
            });

        } catch (RuntimeException e) {
            System.out.println("Stream ended: " + e.getMessage());
        }
    }
    @Override
    public DataEntity updateDataRecord(String assetId, String sourceId, LocalDate businessDate, DataEntity updatedData) {
        // 1. Load existing record using repository method
        DataEntity existing = dataRepository
                .findByAssetIdAndDataSourceIdAndBusinessDate(assetId, sourceId, businessDate)
                .orElseThrow(() -> new RuntimeException("Data record not found"));

        // 2. Preserve immutable fields
        updatedData.setAssetId(existing.getAssetId());
        updatedData.setDataSourceId(existing.getDataSourceId());
        updatedData.setBusinessDate(existing.getBusinessDate());

        // 3. Update systemDate
        updatedData.setSystemDate(java.time.Instant.now());

        // 4. Save (Cassandra upsert)
        return dataRepository.save(updatedData);
    }
}
