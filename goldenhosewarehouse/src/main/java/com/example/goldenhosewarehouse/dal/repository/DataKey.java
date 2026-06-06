package com.example.goldenhosewarehouse.dal.repository;

import org.springframework.data.cassandra.core.cql.PrimaryKeyType;
import org.springframework.data.cassandra.core.mapping.PrimaryKeyClass;
import org.springframework.data.cassandra.core.mapping.PrimaryKeyColumn;
import java.io.Serializable;
import java.util.Objects;

@PrimaryKeyClass
public record DataKey(
        @PrimaryKeyColumn(name = "asset_id", ordinal = 0, type = PrimaryKeyType.PARTITIONED)
        String assetId,

        @PrimaryKeyColumn(name = "data_source_id", ordinal = 1, type = PrimaryKeyType.PARTITIONED)
        String dataSourceId
) implements Serializable {
    public DataKey {
        Objects.requireNonNull(assetId);
        Objects.requireNonNull(dataSourceId);
    }
}