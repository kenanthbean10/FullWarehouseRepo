package com.example.Streaming.Client.domain;



import lombok.Data;
import java.time.LocalDate;
import java.util.Map;

@Data
public class StockDataResponseDTo {
    private String assetId;
    private String dataSourceId;
    private LocalDate businessDate;

    // Use String or Instant for systemDate to handle the timestamp format
    private String systemDate;

    private Map<String, Double> valuesdouble;

    // Since these are null in your example, Object or Map works fine
    private Map<String, Integer> valuesInt;
    private Map<String, String> valuesText; // Included to match your JSON nulls
}