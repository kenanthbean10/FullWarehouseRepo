package com.example.spark_analytics.services;

import org.apache.spark.ml.feature.Normalizer;
import org.apache.spark.ml.feature.VectorAssembler;
import org.apache.spark.ml.regression.LinearRegression;
import org.apache.spark.ml.regression.LinearRegressionModel;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SaveMode;
import org.apache.spark.sql.SparkSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.apache.spark.sql.functions.*;

@Service
public class PredictionService {

    private static final Logger log = LoggerFactory.getLogger(PredictionService.class);

    @Autowired
    private SparkSession spark;

    public void runPredictionWorkflow(String assetID) {
        String keyspace = "acme_productions";
        String tableName = "datan";

        log.info("=================================================");
        log.info("Starting ML Workflow for Asset ID: {}", assetID);
        log.info("Targeting Keyspace: {}, Table: {}", keyspace, tableName);
        log.info("=================================================");

        try {
            // 1. Read Raw Data from Cassandra
            log.info("STEP 1: Reading raw data from Cassandra table '{}.{}'", keyspace, tableName);
            Dataset<Row> rawData = spark.read()
                    .format("org.apache.spark.sql.cassandra")
                    .options(Map.of("table", tableName, "keyspace", keyspace))
                    .load();

            log.info("Raw data schema:");
            rawData.printSchema();
            log.info("Sample of raw data (first 2 rows):");
            rawData.show(2, false);

            long rawCount = rawData.count();
            log.info("Total rows in raw table: {}", rawCount);

            // 2. Filter by asset ID and prepare analytical dataset
            log.info("STEP 2: Filtering data for asset ID = '{}'", assetID);
            Dataset<Row> filtered = rawData.filter(col("asset_id").equalTo(assetID));
            long filteredCount = filtered.count();
            log.info("Rows after filtering by asset ID: {}", filteredCount);
            if (filteredCount == 0) {
                log.error("No data found for asset ID: {}. Check asset_id values in Cassandra.", assetID);
                return;
            }
            log.info("Sample of filtered data (first 2 rows):");
            filtered.show(2, false);

            log.info("Extracting map values (open, close, low, high) and converting date to seconds and bdate...");
            Dataset<Row> df = filtered
                    .select(
                            col("values_double").getItem("open").as("open"),
                            col("values_double").getItem("close").as("close"),
                            col("values_double").getItem("low").as("low"),
                            col("values_double").getItem("high").as("high"),
                            unix_timestamp(
                                    to_date(col("business_date"), "yyyy-MM-dd")
                            ).as("seconds"),
                            date_format(
                                    to_date(col("business_date"), "yyyy-MM-dd"), "yyyy-MM-dd"
                            ).as("bdate")  // StringType instead of DateType
                    )
                    .filter(col("open").isNotNull())
                    .filter(col("seconds").isNotNull())
                    .filter(col("bdate").isNotNull());

            long processedCount = df.count();
            log.info("Rows after extracting map values and filtering nulls: {}", processedCount);
            log.info("Processed data schema:");
            df.printSchema();
            log.info("Sample of processed data (first 5 rows):");
            df.show(5, false);

            if (processedCount == 0) {
                log.error("No valid data after extracting map values. Check if 'values_double' map contains keys 'open','close','low','high' and 'business_date' is in yyyy-MM-dd format.");
                return;
            }

            // Write extracted features to regression_data
            log.info("STEP 3: Writing extracted features to regression_data table...");
            Dataset<Row> regressionData = df.select(
                    col("bdate"),
                    col("seconds"),
                    col("open"),
                    col("close"),
                    col("low"),
                    col("high")
            );
            log.info("Regression data schema:");
            regressionData.printSchema();
            log.info("Sample of regression data (first 3 rows):");
            regressionData.show(3, false);

            regressionData.write()
                    .format("org.apache.spark.sql.cassandra")
                    .options(Map.of("table", "regression_data", "keyspace", keyspace))
                    .mode(SaveMode.Append)
                    .save();
            log.info("Successfully written {} rows to regression_data.", regressionData.count());

            // 4. Feature Engineering
            log.info("STEP 4: Assembling feature vector (seconds, close, low, high) -> 'features'");
            VectorAssembler assembler = new VectorAssembler()
                    .setInputCols(new String[]{"seconds", "close", "low", "high"})
                    .setOutputCol("features");

            Dataset<Row> assembledData = assembler.transform(df);
            log.info("Assembled data schema:");
            assembledData.printSchema();
            log.info("Sample of assembled data (first 3 rows):");
            assembledData.show(3, false);

            // 5. Normalization
            log.info("STEP 5: Normalizing feature vectors (L2 norm) -> 'normFeatures'");
            Normalizer normalizer = new Normalizer()
                    .setInputCol("features")
                    .setOutputCol("normFeatures")
                    .setP(2.0);

            Dataset<Row> normalizedData = normalizer.transform(assembledData);
            log.info("Normalized data schema:");
            normalizedData.printSchema();
            log.info("Sample of normalized data (first 3 rows):");
            normalizedData.show(3, false);

            // 6. Split data
            log.info("STEP 6: Splitting data into 70% training / 30% testing");
            Dataset<Row>[] splits = normalizedData.randomSplit(new double[]{0.7, 0.3});
            Dataset<Row> trainingData = splits[0];
            Dataset<Row> testData = splits[1];
            log.info("Training set rows: {}, Testing set rows: {}", trainingData.count(), testData.count());

            if (trainingData.isEmpty()) {
                log.error("Training dataset is empty. Not enough data for asset: {}", assetID);
                throw new RuntimeException("No training data");
            }

            // 7. Train Linear Regression model
            log.info("STEP 7: Training Linear Regression model (labelCol=open, featuresCol=normFeatures)");
            LinearRegression lr = new LinearRegression()
                    .setLabelCol("open")
                    .setFeaturesCol("normFeatures")
                    .setMaxIter(10)
                    .setRegParam(1.0)
                    .setElasticNetParam(1.0);

            LinearRegressionModel model = lr.fit(trainingData);
            log.info("Model training successful.");
            log.info("Model intercept: {}", model.intercept());
            log.info("Model coefficients: {}", model.coefficients());

            // 8. Generate predictions and save to regression_results
            log.info("STEP 8: Generating predictions on test data and saving to regression_results");
            Dataset<Row> predictionsRaw = model.transform(testData);
            log.info("Prediction schema:");
            predictionsRaw.printSchema();
            log.info("Sample of predictions (first 3 rows):");
            predictionsRaw.show(3, false);

            Dataset<Row> predictions = predictionsRaw.select(
                    lit(assetID).as("asset_id"),
                    col("seconds"),
                    col("open"),
                    col("prediction")
            );

            log.info("Predictions to be written (first 5 rows):");
            predictions.show(5, false);

            predictions.write()
                    .format("org.apache.spark.sql.cassandra")
                    .options(Map.of("table", "regression_results", "keyspace", keyspace))
                    .mode(SaveMode.Append)
                    .save();

            log.info("SUCCESS: ML Workflow complete for {}. Predictions persisted.", assetID);
            log.info("=================================================");

        } catch (Exception e) {
            log.error("FATAL ERROR in runPredictionWorkflow for {}: {}", assetID, e.getMessage(), e);
            log.error("Full stack trace:", e);
            throw new RuntimeException("Prediction workflow failed", e);
        }
    }

    public List<Map<String, Object>> getPredictionResults(String assetId) {
        log.info("Fetching prediction results for asset: {}", assetId);
        try {
            Dataset<Row> results = spark.read()
                    .format("org.apache.spark.sql.cassandra")
                    .options(Map.of(
                            "table", "regression_results",
                            "keyspace", "acme_productions",
                            "pushdown", "true"
                    ))
                    .load()
                    .filter(col("asset_id").equalTo(assetId))
                    .orderBy(col("seconds").asc());

            log.info("Query results schema:");
            results.printSchema();
            log.info("Sample results (first 3 rows):");
            results.show(3, false);

            long count = results.count();
            log.info("Total prediction rows for asset {}: {}", assetId, count);

            List<Map<String, Object>> list = results.collectAsList().stream()
                    .map(row -> Map.of(
                            "asset_id", row.getAs("asset_id"),
                            "time", row.getAs("seconds"),
                            "actual", row.getAs("open"),
                            "predicted", row.getAs("prediction")
                    ))
                    .collect(Collectors.toList());

            log.info("Returning {} prediction records", list.size());
            return list;
        } catch (Exception e) {
            log.error("Error fetching prediction results for asset {}: {}", assetId, e.getMessage(), e);
            throw new RuntimeException("Failed to fetch prediction results", e);
        }
    }
}