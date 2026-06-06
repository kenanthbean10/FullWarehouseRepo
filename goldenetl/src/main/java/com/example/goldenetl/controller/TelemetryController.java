package com.example.goldenetl.controller;

import com.example.goldenetl.metrics.IngestionPulse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/telemetry")
public class TelemetryController {
    private final IngestionPulse ingestionPulse;

    public TelemetryController(IngestionPulse ingestionPulse) {
        this.ingestionPulse = ingestionPulse;
    }
    @GetMapping("/pulse")
    public ResponseEntity<Map<String, Object>> getPipelinePulse(){
        Map<String, Object> overAllstatus = new LinkedHashMap<>();
        overAllstatus.put("status", "ACTIVE");
        overAllstatus.put("records_fetched_from_nasdaq", ingestionPulse.getRecordsFetched());
        overAllstatus.put("records_transformed_by_mapper", ingestionPulse.getRecordsTransformed());
        overAllstatus.put("records_successfully_stored", ingestionPulse.getRecordsStored());
        overAllstatus.put("records_failed_or_skipped", ingestionPulse.getRecordsFailed());

        return new ResponseEntity<>(overAllstatus, HttpStatus.OK);
    }
}
