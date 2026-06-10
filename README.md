# Financial Markets Data Warehouse

Multi-service platform for ingesting, storing, and analyzing financial time-series data.

**Technologies Used:**

* Apache Cassandra (temporal versioning)
* RabbitMQ (event-driven ingestion pipeline)
* Apache Spark (aggregation and machine learning)
* Spring Boot Microservices
* MCP (Model Context Protocol) Server for LLM integration

---

## System Architecture

```text
Nasdaq API
    |
    v
Producer Service
    |
RabbitMQ Queue
    |
    v
GoldenETTI (ETL)
    |
    v
Warehouse API
    |
    +--> Cassandra
    |
    +--> Spark Analytics
    |
    +--> MCP Server
```

---

## Prerequisites

* Java 17+
* Maven 3.8+
* Docker
* Apache Spark
* Cassandra
* RabbitMQ
* Postman (optional)

---

## Startup Guide

### 1. Start Cassandra

```bash
docker start d50403644315
```

### 2. Create Database Schema

Copy schema file:

```bash
docker cp queries.cql d50403644315:/tmp/queries.cql
```

Open Cassandra shell:

```bash
docker exec -it d50403644315 cqlsh
```

Execute schema:

```sql
SOURCE '/tmp/queries.cql';
```

Verify:

```sql
DESCRIBE KEYSPACES;
USE acme_productions;
DESCRIBE TABLES;
```

---

### 3. Start RabbitMQ

```bash
docker run -d \
--name rabbitmq \
-p 5672:5672 \
-p 15672:15672 \
rabbitmq:3-management
```

RabbitMQ Dashboard:

```
http://localhost:15672
```

Credentials:

```
guest / guest
```

---

### 4. Build All Services

```bash
cd producer && mvn clean package

cd ../goldenetti && mvn clean package

cd ../goldenhosewarehouse && mvn clean package

cd ../spark-analytics && mvn clean package

cd "../MCP (1)" && mvn clean package
```

---

### 5. Run Services

#### Warehouse API

```bash
cd goldenhosewarehouse
java -jar target/goldenhosewarehouse-*.jar
```

Runs on:

```
http://localhost:8080
```

#### Producer Service

```bash
cd producer
java -jar target/producer-*.jar
```

Runs on:

```
http://localhost:8081
```

#### GoldenETTI

```bash
cd goldenetti
java -jar target/goldenetti-*.jar
```

Runs on:

```
http://localhost:8082
```

#### MCP Server

```bash
cd "../MCP (1)"
java -jar target/mcp-*.jar
```

Runs on:

```
http://localhost:8099
```

---

## Data Ingestion Workflow

1. Producer fetches market data from Nasdaq API.
2. Producer publishes messages to RabbitMQ queue.
3. GoldenETTI consumes messages.
4. GoldenETTI transforms incoming records.
5. GoldenETTI sends data to Warehouse API.
6. Warehouse stores temporal versions in Cassandra.
7. Spark performs aggregation and prediction tasks.
8. MCP exposes warehouse data to LLM applications.

---

# REST API Examples

Base URL:

```bash
http://localhost:8080
```

## Assets

### Get All Assets

```bash
curl http://localhost:8080/api/assets
```

### Get Asset Details

```bash
curl http://localhost:8080/api/assets/AAPL
```

### Get Latest Asset Version

```bash
curl http://localhost:8080/api/assets/AAPL/latest
```

---

## Data Sources

### List Sources

```bash
curl "http://localhost:8080/api/sources?page=0&size=10"
```

### Get Source Details

```bash
curl http://localhost:8080/api/sources/NASDAQ_API
```

---

## Time-Series Data

### Range Query

```bash
curl "http://localhost:8080/api/data/AAPL/NASDAQ_API/range?start=2024-01-01&end=2024-01-31"
```

### Paginated Query

```bash
curl "http://localhost:8080/api/data?assetId=AAPL&dataSourceId=NASDAQ_API&startBusinessDate=2024-01-01&endBusinessDate=2024-01-31&page=0&size=10"
```

### Streaming Endpoint

```bash
curl -N "http://localhost:8080/api/data/stream?assetId=AAPL"
```

---

# Spark Analytics

## Trigger Aggregation

```bash
curl -X POST http://localhost:8089/api/spark/aggregate
```

## Trigger Prediction

```bash
curl -X POST http://localhost:8089/api/spark/predict
```

## View Predictions

```bash
curl http://localhost:8089/api/predictions/AAPL
```

---

# Spark Batch Execution

Aggregation:

```bash
spark-submit \
--class com.analytics.ComputeTotalService \
--master local[*] \
target/spark-analytics-*.jar
```

Prediction:

```bash
spark-submit \
--class com.analytics.PredictionService \
--master local[*] \
target/spark-analytics-*.jar
```

Results are stored in:

* totals
* regression_results

---

# MCP Server

JSON-RPC Example:

```bash
curl -X POST http://localhost:8099/jsonrpc \
-H "Content-Type: application/json" \
-d '{"jsonrpc":"2.0","method":"list_assets","id":1}'
```

---

# Running Tests

Run individual modules:

```bash
cd producer && mvn test

cd ../goldenetti && mvn test

cd ../goldenhosewarehouse && mvn test

cd ../spark-analytics && mvn test

cd "../MCP (1)" && mvn test
```

Run all tests:

```bash
mvn clean test
```

Reports:

```text
target/surefire-reports/
```

---

# Configuration

| Service         | Important Properties                 |
| --------------- | ------------------------------------ |
| Producer        | nasdaq.api.key, etl.queue.name       |
| GoldenETTI      | warehouse.url                        |
| Warehouse       | Cassandra host, keyspace, datacenter |
| Spark Analytics | Cassandra configuration              |
| MCP Server      | warehouse.api.base-url               |

RabbitMQ:

```text
Host: localhost
Port: 5672
User: guest
Password: guest
```

---

# Postman Collection

Import:

```text
GoldenHouse.postman_collection.json
```

Included requests:

* Assets
* Data Sources
* Time-Series Queries
* Streaming Endpoints
* Spark Analytics
* MCP Requests
* Telemetry

---



# Troubleshooting

| Issue                        | Solution                                      |
| ---------------------------- | --------------------------------------------- |
| Cassandra connection refused | Start Cassandra container and wait 10 seconds |
| RabbitMQ unavailable         | Restart RabbitMQ container                    |
| Producer unauthorized        | Verify Nasdaq API key                         |
| ETL cannot reach warehouse   | Verify Warehouse service on port 8080         |
| Spark class not found        | Rebuild project using mvn clean package       |
| Empty API results            | Verify ingestion completed successfully       |

---

# Project Status

✅ Event-driven ingestion pipeline implemented

✅ Temporal data versioning with Cassandra

✅ RabbitMQ integration completed

✅ Spark aggregation workflows implemented

✅ Machine learning prediction workflow implemented

✅ MCP server integration completed

✅ REST APIs documented

✅ Postman collection provided

✅ Unit tests added




