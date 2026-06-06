package com.example.spark_analytics.controller;

import com.example.spark_analytics.services.ComputeTotalService;
import com.example.spark_analytics.services.PredictionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
// If you keep the Spark logic in the controller, you also need:
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;

import java.util.List;
import java.util.Map;

import static org.apache.spark.sql.functions.col;



@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {
    @Autowired
    private ComputeTotalService aggregationService;
    @Autowired
    private PredictionService predictionService;
    @PostMapping("/run")
    public ResponseEntity<String> triggerAggregation() {
        try {
            aggregationService.runAggregation();
            return ResponseEntity.ok("Spark job completed successfully and saved to Cassandra.");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error running Spark job: " + e.getMessage());
        }
    }
    @PostMapping("/predict")
    public ResponseEntity<String> runPrediction(@RequestParam String assetId) {
        try {
            // This calls  ML workflow for the specific asset
            predictionService.runPredictionWorkflow(assetId);
            return ResponseEntity.ok("ML Prediction workflow completed for: " + assetId);
        } catch (Exception e) {
            // Useful for catching "No training data found" or Spark errors
            return ResponseEntity.internalServerError().body("ML Error: " + e.getMessage());
        }
    }
    @GetMapping("/prediction_results/{assetId}")
    public ResponseEntity<List<Map<String, Object>>> getPredictionResults(@PathVariable String assetId) {
        try {
            List<Map<String, Object>> results = predictionService.getPredictionResults(assetId);

            if (results.isEmpty()) {
                return ResponseEntity.noContent().build();
            }
            return ResponseEntity.ok(results);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

}
