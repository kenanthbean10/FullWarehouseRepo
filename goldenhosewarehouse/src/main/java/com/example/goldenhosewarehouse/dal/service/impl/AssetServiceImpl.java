package com.example.goldenhosewarehouse.dal.service.impl;



import com.example.goldenhosewarehouse.dal.domain.AssetEntity;
import com.example.goldenhosewarehouse.dal.repository.AssetRepository;
import com.example.goldenhosewarehouse.dal.service.AssetService;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class AssetServiceImpl implements AssetService {


    private final AssetRepository assetRepository;

    // Constructor injection: Spring automatically provides the Repository
    public AssetServiceImpl(AssetRepository assetRepository) {
        this.assetRepository = assetRepository;
    }

    /**
     * [Q1] Implements the business requirement to list all available assets.
     * It calls the native query in the DAL to fetch limited info (IDs).
     */
    @Override
    public List<String> getAllAssetIdentifiers() {
        return assetRepository.findAllAssetIds();
    }



    /**
     * [Q2]to fetch full asset details.
     * If the asset is not found, it handles the error gracefully.
     */
    @Override
    public AssetEntity getAssetById(String id) {
        return assetRepository.findLatest(id)
                .orElseThrow(() -> new RuntimeException("Asset with ID " + id + " not found in the warehouse"));
    }
    @Override
    public AssetEntity saveAsset(AssetEntity asset) {
        // In Cassandra, save() acts as both "Insert" and "Update" (Upsert)
        if (asset.getSystemDate() == null) {
            asset.setSystemDate(java.time.Instant.now());
            System.out.println("hi");
        }
        return assetRepository.save(asset);
    }
    @Override
    public List<AssetEntity> getAllHistoryById(String id) {
        return assetRepository.findAllById(id);
    }

    @Override
    public AssetEntity getLatestAssetById(String id) {
        return assetRepository.findFirstById(id)
                .orElseThrow(() -> new RuntimeException("Asset not found with id: " + id));
    }

    @Override
    public Slice<AssetEntity> getAllAssets(Pageable pageable) {
        return assetRepository.findAll(pageable);
    }
    @Override
    public List<String> getAllAssetIdentifiersUserPaged(int offset, int limit) {
        return assetRepository.findAllAssetIds()
                .stream()
                .sorted()
                .skip(offset)       // skip the first `offset` items
                .limit(limit)       // then take only `limit` items
                .collect(Collectors.toList());
    }

    @Override
    public AssetEntity updateAsset(String id, AssetEntity updatedAsset) {
        // 1. Load existing asset
        AssetEntity existing = getAssetById(id); // throws if not found

        // 2. Preserve immutable fields
        updatedAsset.setId(existing.getId());   // ensure same ID

        // 3. Set systemDate to now
        updatedAsset.setSystemDate(java.time.Instant.now());

        // 4. Copy other updatable fields (name, type, etc.)
        //    (assuming updatedAsset contains the new values for those fields)
        //    No need to copy ID – it's already set.

        // 5. Save (Cassandra upsert)
        return assetRepository.save(updatedAsset);
    }

}