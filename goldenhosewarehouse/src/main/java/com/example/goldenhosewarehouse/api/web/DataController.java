package com.example.goldenhosewarehouse.api.web;

import com.example.goldenhosewarehouse.dal.domain.DataEntity;
import com.example.goldenhosewarehouse.dal.dto.TimeSeriesResponse;
import com.example.goldenhosewarehouse.dal.service.DataService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.data.domain.Slice;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/data")
public class DataController {

private final DataService dataService;


    public DataController(DataService dataService) {
        this.dataService = dataService;
    }
/**
 * [Q5] Returns full time-series data for a specific asset and source.
 * Postman: GET http://localhost:8080/api/data/{assetId}/{sourceId}
 */
@GetMapping("/{assetId}/{sourceId}")
    public Iterable<DataEntity>getTimeSeries(

            @PathVariable String assetId,
            @PathVariable String sourceId
)
{
    return  dataService.getTimeSeries(assetId, sourceId);
}
@GetMapping("/paged")
public Slice<DataEntity> getPagedData(
        @RequestParam (defaultValue = "0") int page,@RequestParam(defaultValue = "10") int size)
{
    return dataService.getAllDataPaged(page, size);
}


/**
 * Analytical Query: Fetches time-series data with  date range.
 * Postman: GET http://localhost:8080/api/data/{assetId}/{sourceId}/range?start=2026-01-01&end=2026-03-31
 */

@GetMapping("/{assetId}/{sourceId}/range")
public Iterable<DataEntity> getByRange(
        @PathVariable String assetId,
        @PathVariable String sourceId,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {

    return dataService.getTimeSeriesByRange(assetId, sourceId, start, end);
}
    @PostMapping
    public DataEntity createDataRecord(@RequestBody DataEntity dataRecord) {
        // ميثود مساعدة داخلية لاستخراج القيم بأمان
        dataRecord.setSystemDate(Instant.now());

        System.out.println(">>> [WAREHOUSE] Data stored in dynamic maps for Asset: " + dataRecord.getAssetId());
        System.out.println("my values for me is "+dataRecord.getValuesdouble());

        return dataService.saveData(dataRecord);
    }

    @GetMapping("/stream")
    public void streamData(@RequestParam String assetId, @RequestParam String sourceId, HttpServletResponse response) throws IOException {
        response.setContentType("application/x-ndjson");
        response.setCharacterEncoding("UTF-8");
        dataService.streamTimeSeries(assetId, sourceId, response.getOutputStream());


    }


    // ميثود لمنع تكرار الكود والـ NullPointerException
    private Double extractDouble(Double flatValue, Map<String, Double> map, String key) {
        if (flatValue != null) return flatValue; // إذا كان مسطحاً جاهزاً
        if (map != null && map.containsKey(key)) return map.get(key); // إذا كان داخل الخريطة
        return 0.0; // القيمة الافتراضية إذا لم يرسله الـ Producer (مثل low حالياً)
    }
//    @PostMapping
//    public DataEntity createDataRecord(@RequestBody DataEntity dataRecord) {
//        // This will insert a new row in the 'data_new' table
//        return dataService.saveData(dataRecord);
//    }

//    @PostMapping
//    public DataEntity createDataRecord(@RequestBody DataEntity dataRecord) {
//        // سيقوم Spring بتحويل الـ JSON تلقائياً إلى DataEntity
//        System.out.println(">>> [WAREHOUSE] Ingesting new record for Asset: " + dataRecord.getAssetId());
//
//        System.out.println(">>> [WAREHOUSE] Data received! Asset ID: " + dataRecord.getAssetId()
//                + " | Price: " + dataRecord.getValuesdouble().get("price"));
//        return dataService.saveData(dataRecord);
//    }

    @GetMapping
    public ResponseEntity<TimeSeriesResponse>  getFormattedTimeSeries(
            @RequestParam String assetId,
            @RequestParam String dataSourceId,
            @RequestParam @DateTimeFormat (iso = DateTimeFormat.ISO.DATE) LocalDate startBusinessDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endBusinessDate,
            @RequestParam(defaultValue = "false") boolean includeAttributes,

            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size



            )
    {
        TimeSeriesResponse response = dataService.getTimeSeriesFormatted(
                assetId, dataSourceId,
                startBusinessDate, endBusinessDate,
                includeAttributes, page, size
        );
        return ResponseEntity.ok(response);
    }


}
