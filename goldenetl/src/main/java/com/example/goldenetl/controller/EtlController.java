package com.example.goldenetl.controller;


import com.example.goldenetl.service.EtlService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/etl")
public class EtlController {
    private final EtlService etlService;
    public EtlController(EtlService etlService) {
        this.etlService = etlService;
    }
@PostMapping
    public Object handleIngestion(@RequestBody Map<String, Object> rawData) {
        return etlService.transformAndLoad(rawData);
}

}
