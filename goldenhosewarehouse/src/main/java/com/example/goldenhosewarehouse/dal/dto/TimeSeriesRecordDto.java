package com.example.goldenhosewarehouse.dal.dto;

import java.time.LocalDate;
import java.util.Map;

public class TimeSeriesRecordDto {
    private LocalDate businessDate;
    private Map<String, Object> values;

    public TimeSeriesRecordDto(LocalDate businessDate, Map<String, Object> values) {
        this.businessDate = businessDate;
        this.values = values;
    }

    public LocalDate getBusinessDate() { return businessDate; }
    public Map<String, Object> getValues() { return values; }
}
