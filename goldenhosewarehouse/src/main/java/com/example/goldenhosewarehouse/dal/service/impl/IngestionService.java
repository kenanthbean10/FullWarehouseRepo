package com.example.goldenhosewarehouse.dal.service.impl;

import com.example.goldenhosewarehouse.dal.domain.AssetEntity;
import com.example.goldenhosewarehouse.dal.domain.DataEntity;
import com.example.goldenhosewarehouse.dal.domain.DataSourceEntity;
import com.example.goldenhosewarehouse.dal.service.AssetService;
import com.example.goldenhosewarehouse.dal.service.DataService;
import com.example.goldenhosewarehouse.dal.service.DataSourceService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
@Slf4j
@Service
public class IngestionService {

    private final AssetService assetService;
    private final DataSourceService dataSourceService;
    private final DataService dataService;

    // حقن الخدمات التي قمت بإنشائها
    public IngestionService(AssetService assetService,
                            DataSourceService dataSourceService,
                            DataService dataService) {
        this.assetService = assetService;
        this.dataSourceService = dataSourceService;
        this.dataService = dataService;
    }

    public void processIncomingData(String ticker, Map<String, Object> rawMessage) {
        // [خطوة 3 و 5]: ضمان وجود المصدر (Provenance)
        // نستخدم ميثود getSourceById، وإذا فشلت (Exception) ننشئ واحداً جديداً
        DataSourceEntity source;
        try {
            source = dataSourceService.getSourceById("NASDAQ_API");
        } catch (RuntimeException e) {
            source = new DataSourceEntity();
            source.setId("NASDAQ_API");
            source.setName("Nasdaq Data Link");
            // استخراج الحقول ديناميكياً كما طلب المستند
            Map<String, String> attrs = new HashMap<>();
            rawMessage.keySet().forEach(k -> attrs.put(k, "inferred_type"));
            source.setAttributes(attrs);
            source = dataSourceService.createSource(source);
        }

        // [خطوة 5]: ضمان وجود الـ Asset
        AssetEntity asset;
        try {
            asset = assetService.getAssetById(ticker);
        } catch (RuntimeException e) {
            asset = new AssetEntity();
            asset.setId(ticker);
            asset.setName(ticker + " Instrument");
            asset = assetService.saveAsset(asset);
        }

        // [خطوة 4 و 6]: تحويل البيانات (Mapping) وحفظها
        DataEntity dataRecord = new DataEntity();
        dataRecord.setAssetId(asset.getId());
        dataRecord.setDataSourceId(source.getId());

        // تحويل التاريخ من الرسالة (Business Date)
        dataRecord.setBusinessDate(LocalDate.parse(rawMessage.get("businessDate").toString()));

        // تعبئة القيم (Values)
        if (rawMessage.get("valuesdouble") != null) {
            dataRecord.setValuesdouble((Map<String, Double>) rawMessage.get("valuesdouble"));
        }

        if (rawMessage.get("valuesInt") != null) {
            dataRecord.setValuesInt((Map<String, Integer>) rawMessage.get("valuesInt"));
        }

        if (rawMessage.get("valuesText") != null) {
            dataRecord.setValuesText((Map<String, String>) rawMessage.get("valuesText"));
        }

        // 3. الحفظ النهائي في Cassandra
        dataService.saveData(dataRecord);
        log.info(">>> [WAREHOUSE] Successfully stored dynamic record for {}", ticker);
        // حفظ البيانات عبر الـ DataService (الذي يضيف systemDate تلقائياً)
        dataService.saveData(dataRecord);
    }
}