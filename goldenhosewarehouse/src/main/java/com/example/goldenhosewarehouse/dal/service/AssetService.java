package com.example.goldenhosewarehouse.dal.service;

import com.example.goldenhosewarehouse.dal.domain.AssetEntity;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface AssetService {
    // [Q1] Business logic to retrieve all asset IDs
    List<String> getAllAssetIdentifiers();

    public List<String> getAllAssetIdentifiersUserPaged(int offset, int limit) ;


    // [Q2] Business logic to get full details of a single asset
    AssetEntity getAssetById(String id);
    AssetEntity saveAsset(AssetEntity asset);

    List<AssetEntity> getAllHistoryById(String id);
    AssetEntity getLatestAssetById(String id);

    Slice<AssetEntity> getAllAssets(Pageable pageable);
    AssetEntity updateAsset(String id, AssetEntity updatedAsset);
}