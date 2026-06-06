package com.example.MCP.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

@Service
public class McpToolService {

    private final WarehouseApiClient warehouseClient;
    private static final int MAX_PAGE_LIMIT = 100;
    private static final int MAX_DAYS_INTERVAL = 365;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public McpToolService(WarehouseApiClient warehouseClient) {
        this.warehouseClient = warehouseClient;
    }

    // ---------- MCP methods ----------
    public Map<String, Object> initialize(Map<String, Object> params, Object id) {
        return Map.of(
                "jsonrpc", "2.0",
                "result", Map.of(
                        "protocolVersion", "0.1.0",
                        "serverInfo", Map.of("name", "goldenhose-mcp", "version", "1.0.0")
                ),
                "id", id
        );
    }

    public Map<String, Object> listTools(Object id) {
        List<Map<String, Object>> tools = List.of(
                toolDef("list_assets",
                        "Returns a paginated list of asset identifiers (e.g., ['AAPL','BTC']). Use offset and limit to page through results.",
                        Map.of(
                                "offset", Map.of("type", "integer", "default", 0, "minimum", 0),
                                "limit", Map.of("type", "integer", "default", 20, "minimum", 1, "maximum", 100)
                        ),
                        List.of()),
                toolDef("get_asset_details",
                        "Returns the latest known version of a single asset (name, type, creation date, etc.).",
                        Map.of("assetId", Map.of("type", "string", "description", "Unique asset identifier")),
                        List.of("assetId")),
                toolDef("list_data_sources",
                        "Returns a paginated list of data source identifiers (e.g., ['yahoo','fred']).",
                        Map.of(
                                "offset", Map.of("type", "integer", "default", 0),
                                "limit", Map.of("type", "integer", "default", 20, "maximum", 100)
                        ),
                        List.of()),
                toolDef("get_data_source_details",
                        "Returns metadata about a specific data source (provider, supported attributes, etc.).",
                        Map.of("dataSourceId", Map.of("type", "string")),
                        List.of("dataSourceId")),
                toolDef("get_time_series_data",
                        "Returns time-series records for an asset and data source within a bounded date range. Max interval 365 days.",
                        Map.of(
                                "assetId", Map.of("type", "string"),
                                "dataSourceId", Map.of("type", "string"),
                                "startBusinessDate", Map.of("type", "string", "format", "date", "description", "YYYY-MM-DD"),
                                "endBusinessDate", Map.of("type", "string", "format", "date"),
                                "includeAttributes", Map.of("type", "boolean", "default", false)
                        ),
                        List.of("assetId", "dataSourceId", "startBusinessDate", "endBusinessDate"))
        );
        return Map.of("jsonrpc", "2.0", "result", Map.of("tools", tools), "id", id);
    }

    private Map<String, Object> toolDef(String name, String description,
                                        Map<String, Object> properties,
                                        List<String> required) {
        return Map.of(
                "name", name,
                "description", description,
                "inputSchema", Map.of(
                        "type", "object",
                        "properties", properties,
                        "required", required
                )
        );
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> callTool(Map<String, Object> params, Object id) {
        String toolName = (String) params.get("name");
        Map<String, Object> args = (Map<String, Object>) params.get("arguments");
        if (args == null) args = Map.of();

        try {
            Object result = switch (toolName) {
                case "list_assets" -> listAssets(args);
                case "get_asset_details" -> getAssetDetails(args);
                case "list_data_sources" -> listDataSources(args);
                case "get_data_source_details" -> getDataSourceDetails(args);
                case "get_time_series_data" -> getTimeSeriesData(args);
                default -> throw new IllegalArgumentException("Unknown tool: " + toolName);
            };
            return Map.of(
                    "jsonrpc", "2.0",
                    "result", Map.of(
                            "content", List.of(Map.of("type", "text", "text", resultToString(result)))
                    ),
                    "id", id
            );
        } catch (IllegalArgumentException e) {
            return Map.of(
                    "jsonrpc", "2.0",
                    "error", Map.of("code", -32000, "message", e.getMessage()),
                    "id", id
            );
        } catch (Exception e) {
            e.printStackTrace();
            return Map.of(
                    "jsonrpc", "2.0",
                    "error", Map.of("code", -32000, "message", "Internal error: " + e.getClass().getSimpleName() + " - " + e.getMessage()),
                    "id", id
            );
        }
    }

    private String resultToString(Object result) {
        if (result == null) return "null";
        if (result instanceof Map || result instanceof List) {
            try {
                return objectMapper.writeValueAsString(result);
            } catch (JsonProcessingException e) {
                return result.toString();
            }
        }
        return result.toString();
    }

    // ---------- Tool implementations with null safety ----------
    private Map<String, Object> listAssets(Map<String, Object> args) {
        int offset = asInt(args.get("offset"), 0);
        int limit = asInt(args.get("limit"), 20);
        if (limit > MAX_PAGE_LIMIT)
            throw new IllegalArgumentException("limit cannot exceed " + MAX_PAGE_LIMIT);

        List<String> assets = warehouseClient.listAssets(offset, limit);
        // FIX: if the client returns null, treat as empty list
        if (assets == null) assets = List.of();

        return Map.of(
                "offset", offset,
                "limit", limit,
                "assets", assets
        );
    }

    private Map<String, Object> getAssetDetails(Map<String, Object> args) {
        String assetId = getRequiredString(args, "assetId");
        Map<String, Object> details = warehouseClient.getAssetDetails(assetId);
        if (details == null)
            throw new IllegalArgumentException("Asset not found: " + assetId);
        return details;
    }

    private Map<String, Object> listDataSources(Map<String, Object> args) {
        int offset = asInt(args.get("offset"), 0);
        int limit = asInt(args.get("limit"), 20);
        if (limit > MAX_PAGE_LIMIT)
            throw new IllegalArgumentException("limit cannot exceed " + MAX_PAGE_LIMIT);

        // Translate: page = offset / limit   (if limit is page size)
        int page = offset / limit;
        Map<String, Object> slice = warehouseClient.listDataSourcesPaginated(page, limit);

        // Extract source IDs from the "content" list (each item has "id")
        List<?> content = (List<?>) slice.getOrDefault("content", List.of());
        List<String> sourceIds = content.stream()
                .map(item -> (Map<String, Object>) item)
                .map(map -> (String) map.get("id"))
                .toList();

        boolean hasNext = (boolean) slice.getOrDefault("hasNext", false);
        int totalElements = (int) slice.getOrDefault("totalElements", -1);

        Map<String, Object> result = new HashMap<>();
        result.put("offset", offset);
        result.put("limit", limit);
        result.put("dataSources", sourceIds);
        result.put("nextOffset", hasNext ? offset + limit : null);
        if (totalElements > 0) result.put("totalCount", totalElements);
        return result;
    }
    private Map<String, Object> getDataSourceDetails(Map<String, Object> args) {
        String sourceId = getRequiredString(args, "dataSourceId");
        Map<String, Object> details = warehouseClient.getDataSourceDetails(sourceId);
        if (details == null)
            throw new IllegalArgumentException("Data source not found: " + sourceId);
        return details;
    }

    private Map<String, Object> getTimeSeriesData(Map<String, Object> args) {
        String assetId = getRequiredString(args, "assetId");
        String sourceId = getRequiredString(args, "dataSourceId");
        String startStr = getRequiredString(args, "startBusinessDate");
        String endStr = getRequiredString(args, "endBusinessDate");
        boolean includeAttributes = asBoolean(args.get("includeAttributes"), false);
        int page = asInt(args.get("page"), 0);
        int size = asInt(args.get("size"), 20);

        // Validate date format and interval
        LocalDate start, end;
        try {
            start = LocalDate.parse(startStr, DateTimeFormatter.ISO_DATE);
            end = LocalDate.parse(endStr, DateTimeFormatter.ISO_DATE);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Invalid date format. Use YYYY-MM-DD");
        }
        if (start.isAfter(end))
            throw new IllegalArgumentException("start date must be before end date");
        if (start.until(end).getDays() > MAX_DAYS_INTERVAL)
            throw new IllegalArgumentException("Date interval exceeds " + MAX_DAYS_INTERVAL + " days");
        if (size > MAX_PAGE_LIMIT)
            throw new IllegalArgumentException("Page size too large, max " + MAX_PAGE_LIMIT);

        Map<String, Object> response = warehouseClient.getTimeSeries(
                assetId, sourceId, start, end, includeAttributes, page, size
        );
        return response != null ? response : Map.of("records", List.of());
    }

    // Helper methods
    private String getRequiredString(Map<String, Object> args, String key) {
        Object val = args.get(key);
        if (val == null || val.toString().isBlank())
            throw new IllegalArgumentException(key + " is required");
        return val.toString();
    }

    private int asInt(Object val, int defaultValue) {
        if (val == null) return defaultValue;
        if (val instanceof Number) return ((Number) val).intValue();
        try {
            return Integer.parseInt(val.toString());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private boolean asBoolean(Object val, boolean defaultValue) {
        if (val == null) return defaultValue;
        if (val instanceof Boolean) return (Boolean) val;
        return Boolean.parseBoolean(val.toString());
    }
}