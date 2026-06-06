package com.example.Streaming.Client.service;

import com.example.Streaming.Client.domain.StockDataResponseDTo;
import com.example.Streaming.Client.feign.WarehouseFeignClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.io.BufferedReader;
import java.io.InputStreamReader;

@Service
@Slf4j
public class StockConsumerService {

    @Autowired
    private WarehouseFeignClient warehouseClient;

    @Autowired
    private ObjectMapper objectMapper;

    public void consumeStream(String assetId, String sourceId) {
        log.info("📡 Connecting to stream for {}...", assetId);

        try (feign.Response response = warehouseClient.getStream(assetId, sourceId)) {
            // Check if server is actually alive and sent a 200 OK
            if (response.status() != 200) {
                log.error(" Failed to connect. Status: {}", response.status());
                return;
            }

            // Read the body line by line (NDJSON)
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(response.body().asInputStream()))) {

                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.isBlank()) continue;

                    // Convert raw JSON line to our Java Object
                    StockDataResponseDTo data = objectMapper.readValue(line, StockDataResponseDTo.class);

                    // Enjoy the data!
                    log.info(" RECEIVED: {} | Date: {} | Close: {}",
                            data.getAssetId(),
                            data.getBusinessDate(),
                            data.getValuesdouble().get("close"));
                }
            }
        } catch (Exception e) {
            log.error(" Stream interrupted: {}", e.getMessage());
        }
    }
}