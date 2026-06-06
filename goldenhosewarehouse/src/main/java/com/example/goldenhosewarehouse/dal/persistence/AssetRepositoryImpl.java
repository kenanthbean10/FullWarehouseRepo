package com.example.goldenhosewarehouse.dal.persistence;

import com.example.goldenhosewarehouse.dal.domain.AssetEntity;
 // تأكد من الـ import الصحيح
import com.example.goldenhosewarehouse.dal.repository.AssetRepository;
import org.springframework.data.cassandra.core.CassandraTemplate;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.springframework.data.cassandra.core.query.Criteria.where;
import static org.springframework.data.cassandra.core.query.Query.query;

@Repository
public class AssetRepositoryImpl implements AssetRepository { // 1. تم تصحيح الواجهة هنا

    private final CassandraTemplate cassandraTemplate;

    // 2. تم تصحيح اسم الـ Constructor ليتطابق مع اسم الكلاس
    public AssetRepositoryImpl(CassandraTemplate cassandraTemplate) {
        this.cassandraTemplate = cassandraTemplate;
    }

    @Override
    public AssetEntity save(AssetEntity entity) {
        return cassandraTemplate.insert(entity);
    }
    /**
     * [Q1] Returns limited info (IDs) using a Native CQL Query.
     * We use a raw string to ensure only the 'id' column is touched.
     */
    @Override
    public List<String> findAllAssetIds() {
        String cql = "SELECT id FROM asset";

        // Executing a native CQL query and mapping the result
        return cassandraTemplate.select(cql, AssetEntity.class)
                .stream()
                .map(AssetEntity::getId)
                .distinct()
                .collect(Collectors.toList());
    }

    /**
     * [Q2] Returns ALL details for an asset using a Native CQL Query.
     * Efficiency: 'LIMIT 1' ensures we only pull the most recent record from the partition.
     */
    @Override
    public Optional<AssetEntity> findLatest(String id) {
        // Native CQL string with a placeholder
        String cql = "SELECT * FROM asset WHERE id = ? LIMIT 1";

        // We use getCqlOperations to execute raw CQL and queryForObject to map it
        try {
            AssetEntity result = cassandraTemplate.getCqlOperations().queryForObject(
                    cql,
                    (row, rowNum) -> cassandraTemplate.getConverter().read(AssetEntity.class, row),
                    id
            );
            return Optional.ofNullable(result);
        } catch (Exception e) {
            // Returns empty if no record is found instead of throwing an exception
            return Optional.empty();
        }
    }


    @Override
    public void delete(AssetEntity entity) {
        cassandraTemplate.delete(entity);
    }



    @Override
    public Iterable<AssetEntity> findAll(String id) {
        return cassandraTemplate.select(query(where("id").is(id)), AssetEntity.class);
    }



    // AssetRepositoryImpl.java
    @Override
    public List<AssetEntity> findAllById(String id) {
        // استعلام لجلب كل النسخ لنفس الـ ID
        String cql = "SELECT * FROM asset WHERE id = ?";

        return cassandraTemplate.getCqlOperations().query(
                cql,
                (row, rowNum) -> cassandraTemplate.getConverter().read(AssetEntity.class, row),
                id
        );
    }

    @Override
    public Optional<AssetEntity> findFirstById(String id) {
        // LIMIT 1 تجلب أول سجل في الـ Partition (وهو الأحدث بسبب DESC)
        String cql = "SELECT * FROM asset WHERE id = ? LIMIT 1";

        try {
            AssetEntity result = cassandraTemplate.getCqlOperations().queryForObject(
                    cql,
                    (row, rowNum) -> cassandraTemplate.getConverter().read(AssetEntity.class, row),
                    id
            );
            return Optional.ofNullable(result);
        } catch (org.springframework.dao.EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }
    /**
     * تنفيذ الـ Pagination العام للأصول
     */

    public Slice<AssetEntity> findAll(Pageable pageable) {
      org.springframework.data.cassandra.core.query.Query query= org.springframework.data.cassandra.core.query.Query.empty().pageRequest(pageable);

      return  cassandraTemplate.slice(query,AssetEntity.class);
    }
}