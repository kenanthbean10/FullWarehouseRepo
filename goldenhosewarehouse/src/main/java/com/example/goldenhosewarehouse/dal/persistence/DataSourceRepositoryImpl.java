package com.example.goldenhosewarehouse.dal.persistence;

import com.example.goldenhosewarehouse.dal.domain.DataSourceEntity;
import com.example.goldenhosewarehouse.dal.repository.DataSourceRepository;
import org.springframework.data.cassandra.core.CassandraTemplate;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public class DataSourceRepositoryImpl implements DataSourceRepository {

    private final CassandraTemplate cassandraTemplate;

    public DataSourceRepositoryImpl(CassandraTemplate cassandraTemplate) {
        this.cassandraTemplate = cassandraTemplate;
    }

    /**
     * [Q3] Returns limited info (IDs) for all data sources available in the warehouse.
     * Uses a Native CQL query to fetch only the 'id' column for efficiency.
     */
    @Override
    public List<String> findAllSourceIds() {
        String cql = "SELECT id FROM data_source";

        // We select the ID column and map it to a list of Strings
        return cassandraTemplate.select(cql, DataSourceEntity.class)
                .stream()
                .map(DataSourceEntity::getId)
                .distinct()
                .collect(Collectors.toList());
    }

    /**
     * [Q4] Returns all the details of a data source (e.g., Bloomberg, Yahoo Finance)
     * knowing its identifier.
     */
    @Override
    public Optional<DataSourceEntity> findLatest(String id) {
        // Native CQL using 'LIMIT 1' to get the most recent configuration
        String cql = "SELECT * FROM data_source WHERE id = ? LIMIT 1";

        try {
            // Using getCqlOperations to safely bind the 'id' parameter
            DataSourceEntity result = cassandraTemplate.getCqlOperations().queryForObject(
                    cql,
                    (row, rowNum) -> cassandraTemplate.getConverter().read(DataSourceEntity.class, row),
                    id
            );
            return Optional.ofNullable(result);
        } catch (org.springframework.dao.EmptyResultDataAccessException e) {
            // Returns empty if the specific data source ID does not exist
            return Optional.empty();
        }
    }

    @Override
    public DataSourceEntity save(DataSourceEntity entity) {
        //
        if (entity.getSystemDate() == null) {
            entity.setSystemDate(java.time.Instant.now());
        }
        return cassandraTemplate.insert(entity);
    }

    @Override
    public void delete(DataSourceEntity entity) {
        // Removes a data source provider from the warehouse
        cassandraTemplate.delete(entity);
    }

    @Override
    public Iterable<DataSourceEntity> findAll(String id) {
        // Retrieves the full audit history for a specific data source configuration
        String cql = "SELECT * FROM data_source WHERE id = ?";
        return cassandraTemplate.getCqlOperations().query(
                cql,
                (row, rowNum) -> cassandraTemplate.getConverter().read(DataSourceEntity.class, row),
                id
        );
    }

    @Override
    public Slice<DataSourceEntity> findAll(Pageable pageable) {

        // LINE 1: Write the raw CQL — LIMIT controls page size
        // pageable.getPageSize() returns whatever "size" the client sent
        // e.g. ?size=5 → LIMIT 5
        String cql = "SELECT * FROM data_source LIMIT " + pageable.getPageSize();

        // LINE 2: Execute the query and map each row to DataSourceEntity
        List<DataSourceEntity> content = cassandraTemplate
                .getCqlOperations()
                .query(
                        cql,
                        (row, rowNum) -> cassandraTemplate
                                .getConverter()
                                .read(DataSourceEntity.class, row)
                );

        // LINE 3: Build a Slice manually
        // params: data, pageable info, hasNext
        // hasNext = true if we got a full page (might be more)
        // hasNext = false if we got less than requested (last page)
        boolean hasNext = content.size() == pageable.getPageSize();

        return new SliceImpl<>(content, pageable, hasNext);
    }
}