package com.example.goldenhosewarehouse.api.web;

import com.example.goldenhosewarehouse.dal.domain.AssetEntity;
import com.example.goldenhosewarehouse.dal.service.AssetService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/assets")


public class AssetController {

private final AssetService assetService;


    public AssetController(AssetService assetService) {
        this.assetService = assetService;
    }

/*
 * Q1 endpoint to list all asset IDs
 * Usage: GET /api/assets
 */

@GetMapping
    public List<String> getAllSourceIdentifiers(){
    return assetService.getAllAssetIdentifiers();
}

//q2 get details of a specifc asset
    //Get : /api/assets/{id}
//    @GetMapping("{id}")
//    public AssetEntity getAssetById(@PathVariable String id){
//    return assetService.getAssetById(id);
//    }


    @PostMapping("/create")
    public AssetEntity createAsset(@RequestBody AssetEntity asset) {
        // Receives JSON and saves it as a new row in Cassandra
        return assetService.saveAsset(asset);
    }

    @GetMapping("/{id}")
    public List<AssetEntity> getAssetHistory(@PathVariable String id) {
        return assetService.getAllHistoryById(id);
    }
    //q2 get details of a specifc asset
    // 2. مسار أحدث نسخة فقط: /api/assets/AAPL/latest
    @GetMapping("/{id}/latest")
    public AssetEntity getLatestAsset(@PathVariable String id) {
        return assetService.getLatestAssetById(id);
    }
    @GetMapping("/paged")
    public ResponseEntity<Slice<AssetEntity>> getAllPaged(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    )
    {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(assetService.getAllAssets(pageable));
    }

    @GetMapping("/userpaged")
    public List<String> getAllAssetIdentifiersUserDefiendPagination(
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(defaultValue = "20") int limit
    ) {
        return assetService.getAllAssetIdentifiersUserPaged(offset, limit);
    }

    @PutMapping("/{id}")
    public ResponseEntity<AssetEntity> updateAsset(@PathVariable String id, @RequestBody AssetEntity asset) {
        AssetEntity updated = assetService.updateAsset(id, asset);
        return ResponseEntity.ok(updated);
    }

}
