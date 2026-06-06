package com.example.goldenhosewarehouse.dal.dto;


import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;
import java.util.Set;
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TimeSeriesResponse {
    private DataBlock data;
    private Set<String> attributes;

    public TimeSeriesResponse(DataBlock data, Set<String> attributes) {
        this.data = data;
        this.attributes = attributes;
    }

    public DataBlock getData() { return data; }
    public Set<String> getAttributes() { return attributes; }

    public static class DataBlock {
        private String assetId;
        private String datasourceId;
        private List<TimeSeriesRecordDto> records;

        public DataBlock(String assetId, String datasourceId, List<TimeSeriesRecordDto> records) {
            this.assetId = assetId;
            this.datasourceId = datasourceId;
            this.records = records;
        }

        public String getAssetId() { return assetId; }
        public String getDatasourceId() { return datasourceId; }
        public List<TimeSeriesRecordDto> getRecords() { return records; }
    }
}