package com.example.goldenhosewarehouse.dal.repository;

import com.example.goldenhosewarehouse.dal.domain.DataEntity;
import org.springframework.data.cassandra.repository.CassandraRepository;
import org.springframework.data.cassandra.repository.Query;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;
import java.util.stream.Stream;

@Repository
// تأكد من وجود مسافة بين extends و CassandraRepository
public interface DataRepository extends CassandraRepository<DataEntity, DataKey>, WarehouseRepository<DataEntity, DataKey> {

    //choice 1 :Pagination
    Slice<DataEntity> findByAssetIdAndDataSourceId(String assetId, String dataSourceId, Pageable pageable);

    //choice 2 :Streaming
    //this all streaming and continous flow
    @Query("SELECT * FROM datan WHERE asset_id = ?0 AND data_source_id = ?1")
    Stream<DataEntity> streamByAssetIdAndDataSourceId(String assetId, String dataSourceId);


    /**
     * [Q5] Time Series Range Query
     * جلب البيانات ضمن نطاق زمني معين.
     */
    Iterable<DataEntity> findByRange(DataKey key, LocalDate startDate, LocalDate endDate);

    /**
     * الحفظ اليدوي (موروث من WarehouseRepository)
     */
    @Override
    DataEntity save(DataEntity entity);

    /**
     * تفعيل الـ Pagination (مطلوب في صفحة 2 من المستند)
     */
    @Override
    Slice<DataEntity> findAll(Pageable pageable);

    // LINE 1: @Query tells Spring to use this exact CQL
// ?0 = first param (assetId)
// ?1 = second param (dataSourceId)
// ?2 = startDate (included)
// ?3 = endDate (NOT included — we handle this in service)
// LINE 2: Slice = one page of results
    @Query("SELECT * FROM datan WHERE asset_id = ?0 " +
            "AND data_source_id = ?1 " +
            "AND business_date >= ?2 " +
            "AND business_date < ?3")
    Slice<DataEntity> findByDateRange(
            String assetId,
            String dataSourceId,
            LocalDate startDate,
            LocalDate endDate,
            Pageable pageable      // ← pagination goes here
    );
    Optional<DataEntity> findByAssetIdAndDataSourceIdAndBusinessDate(String assetId, String sourceId, LocalDate businessDate);
}