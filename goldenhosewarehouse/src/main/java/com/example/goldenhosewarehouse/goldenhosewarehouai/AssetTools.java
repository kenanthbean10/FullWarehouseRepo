//package com.example.goldenhosewarehouse.goldenhosewarehouai;
//
//import com.example.goldenhosewarehouse.dal.domain.AssetEntity;
//import com.example.goldenhosewarehouse.dal.domain.DataEntity;
//import com.example.goldenhosewarehouse.dal.service.AssetService;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.context.annotation.Description;
//
//import java.util.function.Function;
//
//@Configuration
//public class AssetTools {
//
//    @Bean
//    @Description("Get the latest market data for a specific asset by its ID (e.g., BTC, TSLA)")
//    public Function<AssetRequest, AssetEntity> fetchAssetFromWarehouse(AssetService assetService) {
//        // نستخدم الـ request.assetId() لجلب القيمة التي أرسلها الـ LLM
//        return request -> assetService.getLatestAssetById(request.assetId().toUpperCase());
//    }
//    public record AssetRequest(String assetId){}
//}
