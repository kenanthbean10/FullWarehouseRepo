package com.example.goldenhosewarehouse.api.web;

import com.example.goldenhosewarehouse.dal.service.impl.IngestionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/warehouse/ingest")
public class WarehouseIngestionController {
    private final IngestionService ingestionService;

    public WarehouseIngestionController(IngestionService ingestionService) {
        this.ingestionService = ingestionService;
    }
    @PostMapping
    public ResponseEntity<String> receiveDataFromETL(@RequestBody Map<String, Object> payload)
    {
try {
    String ticker =  (String) payload.get("assetId");
    if (ticker == null || ticker.isEmpty()) {
        return ResponseEntity.badRequest().body("Error: assetId is missing in the payload");
    }
    ingestionService.processIncomingData(ticker, payload);
    return ResponseEntity.ok().body("Success store"+ ticker);

}catch (Exception e) {
    return ResponseEntity.internalServerError()
            .body("Warehouse Error: Failed to store data -> " + e.getMessage());
}


    }
}
