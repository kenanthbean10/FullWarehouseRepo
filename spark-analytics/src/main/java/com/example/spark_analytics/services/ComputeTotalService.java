package com.example.spark_analytics.services;

import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SaveMode;
import org.apache.spark.sql.SparkSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;

import static org.apache.spark.sql.functions.*;

@Service
public class ComputeTotalService {

    private static final Logger log = LoggerFactory.getLogger(ComputeTotalService.class);

    @Autowired
    private SparkSession spark;

    public void runAggregation() {
        String keyspace = "acme_productions";

        log.info("Starting Aggregation Job for keyspace: {}", keyspace);

        try {
            // 1. Read Raw Data
            Dataset<Row> rawData = spark.read()
                    .format("org.apache.spark.sql.cassandra")
                    .options(Map.of("table", "datan", "keyspace", keyspace))
                    .load()
                    .select("asset_id", "data_source_id", "business_date");

            long totalInCassandra = rawData.count();
            log.info("Step 1: Total rows found in Cassandra 'datan' table: {}", totalInCassandra);

            // 2. Filter by Source
            Dataset<Row> filteredData = rawData.filter(col("data_source_id").equalTo("NASDAQ_API"));

            long filteredCount = filteredData.count();
            log.info("Step 2: Rows remaining after filtering for 'NASDAQ_API': {}", filteredCount);

            if (filteredCount == 0) {
                log.warn("STOPPING: No data found for 'NASDAQ-API'. Check if the source ID is correct (case-sensitive).");
                return;
            }

            // 3. Year Extraction & Grouping
            log.info("Step 3: Extracting year from business_date and grouping...");
            Dataset<Row> result = filteredData
                    .withColumn("business_date_year", year(col("business_date")))
                    .filter(col("business_date_year").isNotNull()) // Critical check
                    .groupBy("asset_id", "business_date_year")
                    .count()
                    .withColumnRenamed("count", "cnt");

            long resultCount = result.count();
            log.info("Step 4: Final aggregation results count: {}", resultCount);

            if (resultCount > 0) {
                log.info("Sample of results to be saved:");
                result.show(5); // Show first 5 rows in console for manual verification

                // 4. Writing to Cassandra
                log.info("Writing {} rows to 'totals' table...", resultCount);
                result.write()
                        .format("org.apache.spark.sql.cassandra")
                        .options(Map.of("table", "totals", "keyspace", keyspace))
                        .mode(SaveMode.Append)
                        .save();
                log.info("SUCCESS: Aggregation complete and persisted.");
            } else {
                log.error("FAILED: Aggregation produced 0 rows. Check if 'business_date' column has valid date values.");
            }

        } catch (Exception e) {
            log.error("CRITICAL ERROR during aggregation: {}", e.getMessage(), e);
        }
    }
}