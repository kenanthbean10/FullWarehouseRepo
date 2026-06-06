package com.example.goldenhosewarehouse.dal.domain;

import org.springframework.data.cassandra.core.cql.Ordering;
import org.springframework.data.cassandra.core.cql.PrimaryKeyType;
import org.springframework.data.cassandra.core.mapping.Column;
import org.springframework.data.cassandra.core.mapping.PrimaryKey;
import org.springframework.data.cassandra.core.mapping.PrimaryKeyColumn;
import org.springframework.data.cassandra.core.mapping.Table;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;

@Table("datan")
public class DataEntity {

    @PrimaryKeyColumn(name = "asset_id",type = PrimaryKeyType.PARTITIONED,ordinal = 0)
    private String assetId;

    @PrimaryKeyColumn(name = "data_source_id",type = PrimaryKeyType.PARTITIONED,ordinal = 1)
    private String dataSourceId;
    @PrimaryKeyColumn(name = "business_date", type = PrimaryKeyType.CLUSTERED, ordinal = 2, ordering = Ordering.DESCENDING)
    private LocalDate businessDate;

@PrimaryKeyColumn(name = "system_date", type = PrimaryKeyType.CLUSTERED, ordinal = 3, ordering = Ordering.DESCENDING)

    private Instant systemDate;
    @Column("values_double")
private Map<String,Double> valuesdouble ;
    @Column("values_int")
    private Map<String, Integer> valuesInt;
    @Column("values_text")
    private Map<String, String> valuesText;













    public String getAssetId() {
        return assetId;
    }

    public void setAssetId(String assetId) {
        this.assetId = assetId;
    }

    public String getDataSourceId() {
        return dataSourceId;
    }

    public void setDataSourceId(String dataSourceId) {
        this.dataSourceId = dataSourceId;
    }

    public LocalDate getBusinessDate() {
        return businessDate;
    }

    public void setBusinessDate(LocalDate businessDate) {
        this.businessDate = businessDate;
    }

    public Instant getSystemDate() {
        return systemDate;
    }

    public void setSystemDate(Instant systemDate) {
        this.systemDate = systemDate;
    }

    public Map<String, Double> getValuesdouble() {
        return valuesdouble;
    }

    public void setValuesdouble(Map<String, Double> valuesdouble) {
        this.valuesdouble = valuesdouble;
    }

    public Map<String, Integer> getValuesInt() {
        return valuesInt;
    }

    public void setValuesInt(Map<String, Integer> valuesInt) {
        this.valuesInt = valuesInt;
    }

    public Map<String, String> getValuesText() {
        return valuesText;
    }

    public void setValuesText(Map<String, String> valuesText) {
        this.valuesText = valuesText;
    }
}
