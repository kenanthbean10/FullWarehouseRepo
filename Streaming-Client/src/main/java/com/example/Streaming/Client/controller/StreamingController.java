package com.example.Streaming.Client.controller;


import com.example.Streaming.Client.service.StockConsumerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/client/ingestion")
public class StreamingController {
    @Autowired
    private StockConsumerService consumerService;

    // Trigger this with: http://localhost:8081/client/ingestion/start?asset=AAPL
    @GetMapping("/start")
    public String startIngestion(@RequestParam String assetId, @RequestParam String sourceId) {
        // Run in a new thread so the web request doesn't "hang"
        new Thread(() -> {
            consumerService.consumeStream(assetId, sourceId);
        }).start();

        return " Ingestion started for: " + sourceId;
    }


}
