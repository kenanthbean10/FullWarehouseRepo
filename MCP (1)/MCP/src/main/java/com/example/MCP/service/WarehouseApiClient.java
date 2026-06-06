package com.example.MCP.service;


import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Component
public class WarehouseApiClient {

    private final RestClient restClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public WarehouseApiClient(@Value("${warehouse.api.base-url}") String baseUrl) {
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
    }

    // GET /api/assets/userpaged?offset=0&limit=20
    public List<String> listAssets(int offset, int limit) {
        String url = UriComponentsBuilder.fromPath("/api/assets/userpaged")
                .queryParam("offset", offset)
                .queryParam("limit", limit)
                .build().toUriString();
        try {
            String json = restClient.get().uri(url).retrieve().body(String.class);
            if (json == null || json.isBlank()) return Collections.emptyList();
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
    }

    public Map<String, Object> listDataSourcesPaginated(int page, int size) {
        String url = UriComponentsBuilder.fromPath("/api/sources/pagedresources")
                .queryParam("page", page)
                .queryParam("size", size)
                .build().toUriString();
        try {
            String json = restClient.get().uri(url).retrieve().body(String.class);
            if (json == null || json.isBlank()) {
                return Map.of("content", List.of(), "totalElements", 0, "hasNext", false);
            }
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            e.printStackTrace();
            return Map.of("content", List.of(), "totalElements", 0, "hasNext", false);
        }
    }

    // GET /api/assets/{id}/latest
    public Map<String, Object> getAssetDetails(String assetId) {
        String url = "/api/assets/" + assetId + "/latest";
        try {
            String json = restClient.get().uri(url).retrieve().body(String.class);
            if (json == null || json.isBlank()) return null;
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            return null;
        }
    }

    // GET /api/sources
    public List<String> listAllDataSourceIdentifiers() {
        try {
            String json = restClient.get().uri("/api/sources").retrieve().body(String.class);
            if (json == null || json.isBlank()) return Collections.emptyList();
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
    }

    // GET /api/sources/{id}
    public Map<String, Object> getDataSourceDetails(String sourceId) {
        String url = "/api/sources/" + sourceId;
        try {
            String json = restClient.get().uri(url).retrieve().body(String.class);
            if (json == null || json.isBlank()) return null;
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            return null;
        }
    }

    // GET /api/data with all query parameters
    public Map<String, Object> getTimeSeries(String assetId, String dataSourceId,
                                             LocalDate startDate, LocalDate endDate,
                                             boolean includeAttributes, int page, int size) {
        String url = UriComponentsBuilder.fromPath("/api/data")
                .queryParam("assetId", assetId)
                .queryParam("dataSourceId", dataSourceId)
                .queryParam("startBusinessDate", startDate.toString())
                .queryParam("endBusinessDate", endDate.toString())
                .queryParam("includeAttributes", includeAttributes)
                .queryParam("page", page)
                .queryParam("size", size)
                .build().toUriString();
        try {
            String json = restClient.get().uri(url).retrieve().body(String.class);
            if (json == null || json.isBlank()) return Map.of("records", List.of());
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            e.printStackTrace();
            return Map.of("records", List.of());
        }
    }
}