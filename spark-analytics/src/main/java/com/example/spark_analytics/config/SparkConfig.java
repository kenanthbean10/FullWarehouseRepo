package com.example.spark_analytics.config;

import org.apache.spark.sql.SparkSession;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SparkConfig {

    @Bean
    public SparkSession sparkSession() {
        return SparkSession.builder()
                .appName("spartk-analytics")
                .master("local[2]")  // limit concurrency for local machine

                //  orrect Cassandra host (Docker container IP)
               // .config("spark.cassandra.connection.host", "172.17.0.1")
                .config("spark.cassandra.connection.host", "localhost")
.config("spark.cassandra.connection.port", "9042")

                //  spark driver binding (keep localhost)
                .config("spark.driver.bindAddress", "127.0.0.1")

                .config("spark.ui.enabled", "false")

                //  cassandra timeouts
                .config("spark.cassandra.read.timeoutMS", "120000")
                .config("spark.cassandra.connection.timeoutMS", "120000")

                //  reduce pressure on Cassandra
                .config("spark.cassandra.input.readsPerSec", "50")
                .config("spark.cassandra.input.fetch.sizeInRows", "500")
                .config("spark.cassandra.input.split.sizeInMB", "16")

                //  consistency level
                .config("spark.cassandra.input.consistency.level", "LOCAL_ONE")

                //  connection limits for local mode
                .config("spark.driver.host", "localhost")
                .config("spark.cassandra.connection.localConnectionsPerExecutor", "1")
                .config("spark.cassandra.connection.remoteConnectionsPerExecutor", "1")

                //  Retry transient failures
                .config("spark.task.maxFailures", "5")

                .getOrCreate();
    }
}
