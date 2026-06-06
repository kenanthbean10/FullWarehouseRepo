package com.example.MCP.service;



import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Map;

@Service
public class McpToolHandlerService {

    /**
     * Requirement: list_assets with default pagination mapping.
     */
    public String handleListAssets(Map<String, Object> arguments) {
        int offset = (arguments != null && arguments.containsKey("offset")) ? (int) arguments.get("offset") : 0;
        int limit = (arguments != null && arguments.containsKey("limit")) ? (int) arguments.get("limit") : 10;

        // Returns deterministic structured data
        return String.format(
                "{\"metadata\": {\"offset\": %d, \"limit\": %d, \"hasNextPage\": false}, \"assets\": [{\"assetId\": \"BTC\", \"name\": \"Bitcoin\"}]}",
                offset, limit
        );
    }

    /**
     * Requirement: list_data_sources mapping.
     */
    public String handleListDataSources(Map<String, Object> arguments) {
        int offset = (arguments != null && arguments.containsKey("offset")) ? (int) arguments.get("offset") : 0;
        int limit = (arguments != null && arguments.containsKey("limit")) ? (int) arguments.get("limit") : 10;

        return String.format(
                "{\"metadata\": {\"offset\": %d, \"limit\": %d}, \"dataSources\": [{\"dataSourceId\": \"NASDAQ_API\", \"type\": \"REST\"}]}",
                offset, limit
        );
    }

    /**
     * Requirement: get_time_series_data with bounds checking.
     */
    public String handleTimeSeriesData(Map<String, Object> arguments) {
        // Enforce required inputs
        if (arguments == null || !arguments.containsKey("assetId") || !arguments.containsKey("dataSourceId") ||
                !arguments.containsKey("startBusinessDate") || !arguments.containsKey("endBusinessDate")) {
            throw new IllegalArgumentException("Missing parameters: assetId, dataSourceId, startBusinessDate, and endBusinessDate are required.");
        }

        String assetId = (String) arguments.get("assetId");
        String dataSourceId = (String) arguments.get("dataSourceId");
        String startStr = (String) arguments.get("startBusinessDate");
        String endStr = (String) arguments.get("endBusinessDate");

        // Validate YYYY-MM-DD input syntax
        if (!startStr.matches("\\d{4}-\\d{2}-\\d{2}") || !endStr.matches("\\d{4}-\\d{2}-\\d{2}")) {
            throw new IllegalArgumentException("Invalid date format. Expected syntax: YYYY-MM-DD.");
        }

        LocalDate start = LocalDate.parse(startStr);
        LocalDate end = LocalDate.parse(endStr);

        // Reject unbounded intervals or chronological errors
        long daysInterval = ChronoUnit.DAYS.between(start, end);
        if (daysInterval < 0) {
            throw new IllegalArgumentException("Chronological error: Start date cannot occur after end date.");
        }
        if (daysInterval > 90) {
            throw new IllegalStateException("Data boundary error: Requested interval window exceeds the 90-day limit.");
        }

        return String.format(
                "{\"queryMetadata\": {\"assetId\": \"%s\", \"dataSourceId\": \"%s\", \"recordsReturned\": 1}, \"records\": [{\"businessDate\": \"%s\", \"closePrice\": 65200.50}]}",
                assetId, dataSourceId, startStr
        );
    }
}