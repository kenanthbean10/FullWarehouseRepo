package com.example.goldenhosewarehouse.dal.persistence;

import com.example.goldenhosewarehouse.dal.domain.DataEntity;
import com.example.goldenhosewarehouse.dal.repository.DataKey;
import com.example.goldenhosewarehouse.dal.repository.DataRepository;
import com.example.goldenhosewarehouse.dal.repository.WarehouseRepository;
import org.springframework.data.cassandra.core.CassandraTemplate;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public class DataRepositoryImpl implements WarehouseRepository<DataEntity, DataKey> {
    private final CassandraTemplate cassandraTemplate;

    public DataRepositoryImpl(CassandraTemplate cassandraTemplate) {
        this.cassandraTemplate = cassandraTemplate;
    }

    @Override
    public Slice<DataEntity> findAll(Pageable pageable) {
        // نستخدم CassandraTemplate.slice لجلب بيانات مقسمة لصفحات
        return cassandraTemplate.slice(
                org.springframework.data.cassandra.core.query.Query.empty().pageRequest(pageable),
                DataEntity.class
        );
    }
    /**
     * [Q5] Returns the full history of time-series data for a specific asset
     * and data source using native CQL.
     */
    @Override
    public Iterable<DataEntity> findAll(DataKey key) {
        String cql = "SELECT * FROM datan WHERE asset_id = ? AND data_source_id = ?";

        // Use getCqlOperations().query to pass parameters (?) safely
        return cassandraTemplate.getCqlOperations().query(
                cql,
                (row, rowNum) -> cassandraTemplate.getConverter().read(DataEntity.class, row),
                key.assetId(), key.dataSourceId()
        );
    }

    /**
     * Time Series Range Query: Fetches financial data points between two dates.
     */

    public Iterable<DataEntity> findByRange(DataKey key, LocalDate startDate, LocalDate endDate) {
        String cql = "SELECT * FROM datan WHERE asset_id = ? AND data_source_id = ? " +
                "AND business_date >= ? AND business_date <= ?";

        return cassandraTemplate.getCqlOperations().query(
                cql,
                (row, rowNum) -> cassandraTemplate.getConverter().read(DataEntity.class, row),
                key.assetId(), key.dataSourceId(), startDate, endDate
        );
    }
    @Override
    public DataEntity save(DataEntity entity) {
        // Persists a new time-series data point into the warehouse
        return cassandraTemplate.insert(entity);
    }

    /**
     * Retrieves only the single most recent data point for the specified asset/source.
     * Uses 'LIMIT 1' to ensure high performance.
     */
    /**
     * Retrieves only the single most recent data point.
     */
    @Override
    public Optional<DataEntity> findLatest(DataKey key) {
        String cql = "SELECT * FROM datan WHERE asset_id = ? AND data_source_id = ? LIMIT 1";

        try {
            // Using queryForObject for a single result
            DataEntity result = cassandraTemplate.getCqlOperations().queryForObject(
                    cql,
                    (row, rowNum) -> cassandraTemplate.getConverter().read(DataEntity.class, row),
                    key.assetId(), key.dataSourceId()
            );
            return Optional.ofNullable(result);
        } catch (Exception e) {
            // Returns empty if record is not found
            return Optional.empty();
        }
    }

    @Override
    public void delete(DataEntity entity) {
        // Removes a specific time-series entry from the database
        cassandraTemplate.delete(entity);
    }
}