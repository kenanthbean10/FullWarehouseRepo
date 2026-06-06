package com.example.goldenhosewarehouse.dal.service;

import com.example.goldenhosewarehouse.dal.domain.DataSourceEntity;
import org.springframework.data.domain.Slice;

import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

public interface DataSourceService {

    // [Q3] Returns a list of all available data source identifiers
    List<String> getAllSourceIdentifiers();

    // [Q4] Returns full configuration details for a specific data source
    DataSourceEntity getSourceById(String id);

    // DataSourceService.java (Interface)
    DataSourceEntity createSource(DataSourceEntity source);
    Slice<DataSourceEntity> getAllSourcesPaged(Pageable pageable);
    DataSourceEntity updateDataSource(String id, DataSourceEntity updatedSource);
}
