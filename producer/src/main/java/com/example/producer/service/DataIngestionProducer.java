package com.example.producer.service;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import lombok.extern.slf4j.Slf4j;


import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.*;
import java.util.stream.Collectors;
import jakarta.annotation.PostConstruct;
@Slf4j
@Component
public class DataIngestionProducer {

    private final RestTemplate restTemplate;
    private final RabbitTemplate rabbitTemplate;

    // PROJECT REQ: Externalized configuration to avoid Hard-coding
    @Value("${nasdaq.api.key}")
    private String apiKey;

    @Value("${etl.queue.name}")
    private String queueName;

    // PROJECT REQ: Read assets from an external file in resources
    @Value("classpath:assets.txt")
    private Resource assetsFile;

    // This list will be populated from the file during startup
    private List<String> assets = new ArrayList<>();

    public DataIngestionProducer(RestTemplate restTemplate, RabbitTemplate rabbitTemplate) {
        this.restTemplate = restTemplate;
        this.rabbitTemplate = rabbitTemplate;
        log.info("**************************************************");
        log.info("PRODUCER INITIALIZED: ASYNCHRONOUS MODE (RABBITMQ)");
        log.info("**************************************************");
    }

    /**
     * PROJECT REQ: Load tickers from the external file.
     * This runs once automatically after the service starts.
     */
    @PostConstruct
    public void loadAssetsFromFile() {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(assetsFile.getInputStream()))) {
            this.assets = reader.lines()
                    .map(String::trim)
                    .filter(line -> !line.isEmpty())
                    .collect(Collectors.toList());
            log.info(">>> [CONFIG] Successfully loaded {} assets from file.", assets.size());
        } catch (Exception e) {
            log.error(">>> [CRITICAL] Failed to load assets from file: {}", e.getMessage());
            // Fallback to avoid empty processing
            this.assets = Collections.singletonList("AAPL");
        }
    }

    /**
     * PROJECT REQ: Batch-style source execution.
     * This task runs periodically to check for new data.
     */
    @Scheduled(fixedRate = 60000, initialDelay = 1000)
    public void startIngestionProcess() {
        log.info(">>>>>> [EXTRACTION] Starting Scheduled Ingestion Task...");

        for (String ticker : assets) {
            log.info(">>> [PROCESS] Ingesting data for ticker: {}", ticker);
            processTickerExtraction(ticker);
        }
    }

    /**
     * The core logic for the EXTRACTION stage.
     */
    private void processTickerExtraction(String ticker) {
        // PROJECT REQ: Use Nasdaq Data Link (Table Data API preferred)
        // We limit per_page to handle data in manageable chunks
        String baseUrl = String.format(
                "https://data.nasdaq.com/api/v3/datatables/WIKI/PRICES.json?ticker=%s&api_key=%s&qopts.per_page=10",
                ticker, apiKey
        );

        String currentCursor = null;
        boolean hasMorePages = true;

        // PROJECT REQ: Pagination Support
        // Continue fetching until the 'next_cursor_id' is null
        while (hasMorePages) {
            String requestUrl = (currentCursor == null) ? baseUrl : baseUrl + "&qopts.cursor_id=" + currentCursor;

            try {
                log.info(">>> [REQUEST] Fetching page for: {}", ticker);

                // . EXTRACTION: Read raw data from the external provider
                Map<String, Object> rawResponse = restTemplate.getForObject(requestUrl, Map.class);

                if (rawResponse != null && rawResponse.containsKey("datatable")) {

                    // . PROJECT REQ: SEPARATION OF CONCERNS (Asynchronous Messaging)
                    // Instead of waiting for the ETL, we drop the RAW data into RabbitMQ.
                    // This makes the system SCALABLE and FAULT-TOLERANT.
                    rabbitTemplate.convertAndSend(queueName, rawResponse);
                    log.info(">>> [MESSAGING] Raw JSON chunk sent to RabbitMQ Queue: {}", queueName);

                    // 3. PROJECT REQ: Handle Pagination Metadata
                    Map<String, Object> meta = (Map<String, Object>) rawResponse.get("meta");
                    currentCursor = (String) meta.get("next_cursor_id");

                    // Check if we have reached the end of the dataset
                    if (currentCursor == null || currentCursor.isEmpty() || currentCursor.equals("null")) {
                        hasMorePages = false;
                        log.info(">>> [COMPLETE] All history fetched for ticker: {}", ticker);
                    }
                } else {
                    hasMorePages = false;
                }
            } catch (Exception e) {
                // PROJECT REQ: Resilience (One ticker failure doesn't stop the whole service)
                log.error(">>> [CRITICAL ERROR] Extraction failed for ticker {}: {}", ticker, e.getMessage());
                hasMorePages = false;
            }
        }
    }
}