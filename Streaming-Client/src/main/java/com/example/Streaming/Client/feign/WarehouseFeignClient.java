package com.example.Streaming.Client.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "warehouse-api", url = "http://localhost:8080")
public interface WarehouseFeignClient {


    @GetMapping(value = "/api/data/stream", produces = "application/x-ndjson")
    feign.Response getStream(
            @RequestParam("assetId") String assetId,
            @RequestParam("sourceId") String sourceId
    );
}

