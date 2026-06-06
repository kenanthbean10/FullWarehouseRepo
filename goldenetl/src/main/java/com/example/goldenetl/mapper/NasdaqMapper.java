package com.example.goldenetl.mapper;

import com.example.goldenetl.metrics.IngestionPulse;
import org.springframework.stereotype.Component;
import java.util.*;

@Component
public class NasdaqMapper {
    private final IngestionPulse ingestionPulse;

    public NasdaqMapper(IngestionPulse ingestionPulse) {
        this.ingestionPulse = ingestionPulse;
    }

    public Map<String, Object> mapDynamicRow(List<String> columnNames, List<Object> values) {
        Map<String, Object> record = new HashMap<>();



        // These match your Cassandra Map columns exactly
        Map<String, Double> valuesDouble = new HashMap<>();
        Map<String, Integer> valuesInt = new HashMap<>();
        Map<String, String> valuesText = new HashMap<>();

        for (int i = 0; i < columnNames.size(); i++) {
            String colName = columnNames.get(i);
            Object value = values.get(i);

            if (value == null) continue;

            // 1. Fixed Core Identity (The "Entity" part)
            if (colName.equalsIgnoreCase("ticker")) {
                record.put("assetId", value.toString().toUpperCase());
                continue;
            }
            if (colName.equalsIgnoreCase("date")) {
                record.put("businessDate", value);
                continue;
            }

            // 2. Dynamic Type Detection (The "Attribute" part)
            // This handles any NEW attributes Nasdaq adds in the future automatically
            if (value instanceof Double || value instanceof Float) {
                valuesDouble.put(colName, ((Number) value).doubleValue());
            }
            else if (value instanceof Integer || value instanceof Long) {
                valuesInt.put(colName, ((Number) value).intValue());
            }
            else {
                valuesText.put(colName, value.toString());
            }
        }

        record.put("valuesdouble", valuesDouble);
        record.put("valuesInt", valuesInt);
        record.put("valuesText", valuesText);

        // Metadata Enrichment
        record.put("dataSourceId", "NASDAQ_DYNAMIC_V1");
        record.put("etl_processed_at", java.time.Instant.now().toString());
        ingestionPulse.markAsTransformed();
        return record;
    }
}