package com.example.goldenhosewarehouse.dal.domain;


import org.springframework.data.cassandra.core.cql.Ordering;
import org.springframework.data.cassandra.core.cql.PrimaryKeyType;
import org.springframework.data.cassandra.core.mapping.PrimaryKeyColumn;
import org.springframework.data.cassandra.core.mapping.Table;

import java.time.Instant;
import java.util.Map;

@Table("asset")
public class AssetEntity {
    @PrimaryKeyColumn(name = "id", type = PrimaryKeyType.PARTITIONED)
    private String id;
    @PrimaryKeyColumn(name = "system_date", type = PrimaryKeyType.CLUSTERED, ordering = Ordering.DESCENDING)
    private Instant systemDate;

    private String name;
    private String description;
private Map<String,String> attributes;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Instant getSystemDate() {
        return systemDate;
    }

    public void setSystemDate(Instant systemDate) {
        this.systemDate = systemDate;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Map<String, String> getAttributes() {
        return attributes;
    }

    public void setAttributes(Map<String, String> attributes) {
        this.attributes = attributes;
    }
}
