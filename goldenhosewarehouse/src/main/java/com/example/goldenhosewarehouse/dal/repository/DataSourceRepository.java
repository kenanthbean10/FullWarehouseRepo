package com.example.goldenhosewarehouse.dal.repository;

import com.example.goldenhosewarehouse.dal.domain.DataSourceEntity;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DataSourceRepository extends WarehouseRepository<DataSourceEntity, String> {
    // : save, delete, findLatest(String id), findAll(String id)#
    // [Q3] Returns limited info (IDs) for all data sources
    List<String> findAllSourceIds();

    // [Q4] Returns all details of a data source knowing its identifier
    // Implementation: findLatest(String id)

    DataSourceEntity save(DataSourceEntity entity);
}