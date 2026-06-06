package com.example.goldenhosewarehouse.dal.service;

import com.example.goldenhosewarehouse.dal.domain.AssetEntity;
import com.example.goldenhosewarehouse.dal.domain.DataEntity;
import com.example.goldenhosewarehouse.dal.dto.TimeSeriesResponse;
import org.springframework.data.domain.Slice;

import java.io.IOException;
import java.io.OutputStream;
import java.time.LocalDate;

public interface DataService {

    // [Q5] Business logic to get all time-series data for an asset and source

    Iterable<DataEntity> getTimeSeries(String assetId, String sourceId);

    // Business logic for analytical range queries
    Iterable<DataEntity> getTimeSeriesByRange(String assetId, String sourceId, LocalDate start, LocalDate end);
    DataEntity saveData(DataEntity dataRecord);
    Slice<DataEntity> getAllDataPaged(int page, int size);
    // Add this to your DataService interface
    void streamTimeSeries(String assetId, String sourceId, OutputStream out) throws IOException;
//

    //  method for the formatted /data endpoint for data time series date range
    TimeSeriesResponse getTimeSeriesFormatted(
            String assetId,
            String dataSourceId,
            LocalDate startDate,
            LocalDate endDate,
            boolean includeAttributes,
            int page,
            int size
    );

    DataEntity updateDataRecord(String assetId, String sourceId, LocalDate businessDate, DataEntity updatedData);
}


