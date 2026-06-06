package com.example.goldenhosewarehouse.dal.repository;

import com.example.goldenhosewarehouse.dal.domain.AssetEntity;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AssetRepository extends WarehouseRepository<AssetEntity, String> {

    // [Q1] Returns limited info (IDs) for all assets
    // Implementation will use: SELECT asset_id FROM asset
    List<String> findAllAssetIds();

    // [Q2] Returns all details of an asset knowing its identifier
    // Implementation: findLatest(String id)

    // لجلب كل السجلات مرتبة (التاريخ مدمج في الـ Primary Key)
    List<AssetEntity> findAllById(String id);

    // لجلب أحدث سجل فقط
    Optional<AssetEntity> findFirstById(String id);
}