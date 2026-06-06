package com.example.goldenetl.service;

import org.springframework.beans.factory.annotation.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class EtlService {

    private final RestTemplate restTemplate;

    @Value("${warehouse.url}")
    private String warehouseurl;

    public EtlService(RestTemplate restTemplate,@Value("${warehouse.url}") String warehouseUrl) {
        this.restTemplate = restTemplate;
        this.warehouseurl = warehouseUrl;
    }


    //step 1 Trasnform:
    public Object transformAndLoad(Map<String, Object> rawData){



    //validation
        if(!isValid(rawData)){
            return "Filtered : Invalid Data";
        }
   //Transform the data into Flattening
        Map<String,Object> flattenedData =new HashMap<>();
        flattenedData.put("assetId",rawData.get("assetId").toString().toUpperCase());
        flattenedData.put("dataSourceId",rawData.get("dataSourceId"));
flattenedData.put("businessDate",rawData.get("businessDate"));
//extracting the values from the valuesDouble
   Map<String,Object> valuesDouble =(Map<String,Object>)rawData.get("valuesdouble");

   if(valuesDouble!=null){
       flattenedData.put("price",valuesDouble.get("price"));
       flattenedData.put("high",valuesDouble.get("high"));
       flattenedData.put("low",valuesDouble.get("low"));
   }
   flattenedData.put("valuesdouble",valuesDouble);
   flattenedData.put("valuesInt",rawData.get("valuesInt"));
   flattenedData.put("valuesText",rawData.get("valuesText"));
   //Enrich phase:we addthe time stamp when the etl processed started
        flattenedData.put("etl_processed_at",java.time.Instant.now().toString());
   flattenedData.put("batch_id","GOLDEN-"+java.util.UUID.randomUUID().toString());

   //(Load)sendign the clean and flttening data to the sotre
        try{
            log.info(">>>[ETL COMPLETE] Sending flattened record for :{}",flattenedData.get("assetId"));
return restTemplate.postForEntity(warehouseurl,flattenedData,String.class).getBody();
        }catch (Exception e){
            log.error(">>> [ETL ERROR] Failed to connect to Warehouse: {}", e.getMessage());
            throw new RuntimeException("Warehouse connection failed");
        }
        }
    /**
     * Validates and sanitizes the incoming raw data before forwarding to the Warehouse.
     * * @param data The raw data map received from the Producer.
     * @return true if the data is valid and safe to process; false otherwise.
     */
    private boolean isValid(Map<String, Object> data) {
        try {
            // 1. Mandatory Metadata Check: Ensure core identification fields exist
            if (isMissing(data, "assetId") || isMissing(data, "dataSourceId") || isMissing(data, "businessDate")) {
                log.error(">>> [VALIDATION] Rejected: Missing mandatory metadata fields (assetId, dataSourceId, or businessDate).");
                return false;
            }

            // 2. Numerical Integrity Check: Verify that 'valuesdouble' and 'price' are present
            @SuppressWarnings("unchecked")
            Map<String, Object> valuesDouble = (Map<String, Object>) data.get("valuesdouble");

            if (valuesDouble == null || !valuesDouble.containsKey("price")) {
                log.error(">>> [VALIDATION] Rejected: 'price' field is missing in valuesdouble nested map.");
                return false;
            }

            // 3. Logical Range Check: Ensure the price is a positive, realistic number
            double price = Double.parseDouble(valuesDouble.get("price").toString());
            if (price <= 0 || price > 1000000) {
                log.warn(">>> [VALIDATION] Rejected: Price {} is out of realistic range (0 - 1M).", price);
                return false;
            }

            // 4. Data Sanitization: Normalize fields to ensure consistency in the database
            // Example: Convert "tsla " -> "TSLA" to prevent duplicate entries with different casing
            String sanitizedAssetId = data.get("assetId").toString().toUpperCase().trim();
            data.put("assetId", sanitizedAssetId);

            return true;
        } catch (NumberFormatException e) {
            log.error(">>> [VALIDATION] Rejected: Price format is not a valid number.");
            return false;
        } catch (Exception e) {
            log.error(">>> [VALIDATION] Critical error during validation process: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Helper method to check if a specific key is missing, null, or contains only whitespace.
     */
    private boolean isMissing(Map<String, Object> data, String key) {
        return !data.containsKey(key)
                || data.get(key) == null
                || data.get(key).toString().trim().isEmpty();
    }

    }












