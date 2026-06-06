package com.example.Streaming.Client.runner;


import com.example.Streaming.Client.service.StockConsumerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class StreamRunner implements CommandLineRunner {

    @Autowired
    private StockConsumerService consumerService;

    @Override
    public void run(String... args) throws Exception {
        // This kicks off the process

    }
}