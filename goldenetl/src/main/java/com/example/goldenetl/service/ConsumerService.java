package com.example.goldenetl.service;

import com.example.goldenetl.mapper.NasdaqMapper;
import com.example.goldenetl.metrics.IngestionPulse;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import lombok.extern.slf4j.Slf4j;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
public class ConsumerService {

    private final NasdaqMapper nasdaqMapper;
    private final RestTemplate restTemplate;
    private final IngestionPulse ingestionPulse;

    @Value("${warehouse.url}")
    private String warehouseUrl;

    public ConsumerService(NasdaqMapper nasdaqMapper, RestTemplate restTemplate, IngestionPulse ingestionPulse) {
        this.nasdaqMapper = nasdaqMapper;
        this.restTemplate = restTemplate;
        this.ingestionPulse = ingestionPulse;
    }

    /**
     * PROJECT REQ: Asynchronous Message Consumption
     * This method listens to RabbitMQ, extracts dynamic columns,
     * and processes rows one by one.
     */
    @RabbitListener(queues = "${etl.queue.name}")
    public void onMessageReceived(Map<String, Object> packet) {
        try {
            log.info(">>> [MQ] Received Nasdaq packet from Queue. Extracting Metadata...");

            // 1. Extraction: Get the Datatable object
            Map<String, Object> datatable = (Map<String, Object>) packet.get("datatable");
            if (datatable == null) return;

            // 2.: Get Column Names dynamically from the 'columns' array
            // This allows us to handle any future attributes added by Nasdaq
            List<Map<String, String>> columnMeta = (List<Map<String, String>>) datatable.get("columns");
            List<String> colNames = columnMeta.stream()
                    .map(c -> c.get("name"))
                    .collect(Collectors.toList());

            //  Row Extraction: Get the actual data rows
            List<List<Object>> rows = (List<List<Object>>) datatable.get("data");
            if (rows == null || rows.isEmpty()) return;
            ingestionPulse.AddFetched(rows.size());
            log.info(">>> [MQ] Processing batch of {} records with columns: {}", rows.size(), colNames);

            int successCount = 0;
            for (List<Object> row : rows) {
                //  Transformation: Use the dynamic mapper to sort into valuesDouble, valuesInt, valuesText
                Map<String, Object> readyToStore = nasdaqMapper.mapDynamicRow(colNames, row);

                //  checking if the record is valid to store in the warehouse
                if (isRecordValid(readyToStore)) {
                    //  Loading: POST to Warehouse (Port 8080)


                    restTemplate.postForEntity(warehouseUrl, readyToStore, String.class).getBody();
                    successCount++;
                    ingestionPulse.addStored();
                }else {
                    // السجل غير صالح (Validation Failed)
                    log.warn(">>> [VALIDATION] Invalid record skipped.");
                    ingestionPulse.addFailed();
                }
            }

            log.info(">>> [ETL SUCCESS] Successfully processed and stored {}/{} records.", successCount, rows.size());

        } catch (Exception e) {
            log.error(">>> [CONSUMER ERROR] Critical failure during dynamic mapping: {}", e.getMessage());
            ingestionPulse.addFailed();
        }
    }

    /**
     * Simple validation check before sending to the Warehouse.
     */
    private boolean isRecordValid(Map<String, Object> record) {
        try {
            Map<String, Object> vDouble = (Map<String, Object>) record.get("valuesdouble");
            if (vDouble != null && vDouble.containsKey("close")) {
                double price = Double.parseDouble(vDouble.get("close").toString());
                return price > 0;
            }
            return record.containsKey("assetId") && record.containsKey("businessDate");
        } catch (Exception e) {
            return false;
        }
    }
}