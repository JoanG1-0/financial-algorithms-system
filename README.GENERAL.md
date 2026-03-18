# Financial Algorithms System

Multi-module microservices project for financial data extraction, transformation, algorithmic analysis, and report generation.

**Stack:** Java 17 · Spring Boot 3.2.5 · Spring Cloud 2023.0.1 · PostgreSQL · Docker · Maven

---

## Table of Contents

- [Architecture](#architecture)
- [Infrastructure](#infrastructure)
- [Quick Start](#quick-start)
- [Services](#services)
  - [Gateway Service](#gateway-service-port-8081)
  - [ETL Service](#etl-service-port-8082)
  - [Algorithm Service](#algorithm-service-port-8083)
  - [Report Service](#report-service-port-8084)
  - [Config Server](#config-server-port-8888)
- [API Reference](#api-reference)
  - [ETL Endpoints](#etl-endpoints)
  - [Algorithm Endpoints](#algorithm-endpoints)
  - [Report Endpoints](#report-endpoints)
- [Data Models](#data-models)
- [Environment Variables](#environment-variables)
- [Build & Test](#build--test)

---

## Architecture

```
Client
  │
  ▼
Gateway (8081)
  ├──► ETL Service (8082) ──► TwelveData API
  │         │
  │         ▼
  │      PostgreSQL (etl_db)
  │
  ├──► Algorithm Service (8083) ──► ETL Service (cleaned-prices)
  │         │
  │         ▼
  │      PostgreSQL (algorithm_db)
  │
  └──► Report Service (8084) ──► Algorithm Service (similarity/risk/patterns)
            │
            ▼
         PostgreSQL (report_db)
```

Each service owns its own PostgreSQL database. Communication between services is REST over HTTP.

---

## Infrastructure

| Container | Image | Port | Purpose |
|-----------|-------|------|---------|
| `financial-postgres` | postgres:16-alpine | 5432 | Shared PostgreSQL host (3 databases) |
| `financial-jenkins` | jenkins:lts-jdk17 | 8080, 50000 | CI/CD |
| `financial-sonarqube` | sonarqube:10.3.0-community | 9000 | Code quality |
| `financial-etl` | custom build | 8082 | ETL service |
| `financial-algorithm` | custom build | 8083 | Algorithm service |
| `financial-report` | custom build | 8084 | Report service |

**Databases:** `etl_db`, `algorithm_db`, `report_db` — all created by `docker/init-db/01-create-databases.sh`.

---

## Quick Start

```bash
# 1. Start infrastructure
docker-compose up -d

# 2. Build all modules (skip tests)
./mvnw.cmd clean install -DskipTests

# 3. Set required env var (for integration tests)
export TWELVE_DATA_API_KEY=your_api_key_here
```

**Recommended usage flow:**

```
1. POST /api/etl/batch/trigger       → download 20 assets (5 years of history)
2. GET  /api/etl/batch/status        → confirm download completed
3. POST /api/etl/transform           → clean & transform price data
4. GET  /api/etl/transform/status    → check quality counts
5. POST /api/algorithm/run           → run all algorithms
6. POST /api/v1/reports/correlation-matrix/generate → generate correlation matrix
7. GET  /api/v1/reports/export/pdf   → download full PDF report
```

---

## Services

### Gateway Service (Port 8081)

Spring Cloud Gateway — single entry point for all client requests. Routes traffic to the appropriate downstream service. No custom endpoints.

### ETL Service (Port 8082)

Responsible for:
- Extracting financial price data from the [TwelveData API](https://twelvedata.com/)
- Persisting raw `FinancialSeries` and `PriceRecord` entities
- Running a data transformation pipeline: trading calendar alignment, OHLC consistency checks, forward-fill of missing values, and Z-score anomaly detection
- Exposing cleaned price data to the Algorithm Service

**Tracked assets (batch):** AAPL, MSFT, GOOGL, AMZN, META, TSLA, NVDA, JPM, JNJ, V, NFLX, AMD, INTC, DIS, WMT, BAC, KO, PG, IBM, ORCL

### Algorithm Service (Port 8083)

Responsible for:
- Fetching cleaned prices from the ETL service
- Computing pairwise similarity metrics (Euclidean, Pearson, DTW, Cosine)
- Classifying risk profiles (annualized volatility → CONSERVATIVE / MODERATE / AGGRESSIVE)
- Detecting Mean Reversion patterns per asset
- Computing Simple Moving Averages (SMA) per asset

### Report Service (Port 8084)

Responsible for:
- Building a full symmetric Pearson correlation matrix from algorithm results
- Persisting `CorrelationEntry` records
- Generating a multi-section PDF report (OpenPDF 1.3.30) with similarities, risk profiles, patterns, and correlation data

### Config Server (Port 8888)

Spring Cloud Config Server (native/file-based). Centralized configuration lives in `config-server/src/main/resources/config-repo/`.

---

## API Reference

### ETL Endpoints

Base URL: `http://localhost:8082`

---

#### `POST /api/etl/extract`

Extracts and loads a single financial series from TwelveData.

**Query parameters:**

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `symbol` | String | Yes | Ticker symbol (e.g. `AAPL`) |

**Response:** `201 Created` — the persisted `FinancialSeries` entity.

**Example:**
```bash
curl -X POST "http://localhost:8082/api/etl/extract?symbol=AAPL"
```

---

#### `GET /api/etl/series/{symbol}`

Returns all series records for a given symbol.

**Path parameters:**

| Parameter | Type | Description |
|-----------|------|-------------|
| `symbol` | String | Ticker symbol |

**Response:** `200 OK` — `List<FinancialSeries>`

**Example:**
```bash
curl "http://localhost:8082/api/etl/series/AAPL"
```

---

#### `POST /api/etl/batch/trigger`

Triggers an asynchronous batch download for all 20 tracked assets (5 years of daily history).

**Response:** `202 Accepted` — confirmation message string.

**Example:**
```bash
curl -X POST "http://localhost:8082/api/etl/batch/trigger"
```

---

#### `GET /api/etl/batch/status`

Returns the status of the last batch download.

**Response:** `200 OK` — `BatchDownloadLog` | `204 No Content` (if no batch has run yet)

**Example:**
```bash
curl "http://localhost:8082/api/etl/batch/status"
```

**Response body example:**
```json
{
  "id": 1,
  "downloadDate": "2026-03-18",
  "status": "COMPLETED",
  "startedAt": "2026-03-18T10:00:00",
  "completedAt": "2026-03-18T10:05:30",
  "totalSymbols": 20,
  "downloadedSymbols": 20,
  "failedSymbols": 0,
  "errorMessage": null
}
```

---

#### `POST /api/etl/transform`

Triggers the full data transformation pipeline over all raw price records.

Pipeline steps:
1. **Trading Calendar** — aligns records to valid trading days
2. **OHLC Check** — flags inconsistent open/high/low/close values
3. **Forward-fill** — fills missing dates with last known value
4. **Z-score Anomaly Detection** — corrects or flags statistical outliers

**Response:** `202 Accepted` — `TransformSummary`

**Example:**
```bash
curl -X POST "http://localhost:8082/api/etl/transform"
```

**Response body example:**
```json
{
  "symbolsProcessed": 20,
  "calendarDays": 1260,
  "totalRecords": 25200,
  "cleanRecords": 24800,
  "forwardFilledRecords": 300,
  "anomalyCorrectedRecords": 80,
  "anomalyFlaggedRecords": 20,
  "transformedAt": "2026-03-18T10:10:00"
}
```

---

#### `GET /api/etl/transform/status`

Returns a count breakdown of cleaned records by data quality category.

**Response:** `200 OK` — `Map<String, Long>`

**Example:**
```bash
curl "http://localhost:8082/api/etl/transform/status"
```

**Response body example:**
```json
{
  "CLEAN": 24800,
  "FORWARD_FILLED": 300,
  "ANOMALY_CORRECTED": 80,
  "ANOMALY_FLAGGED": 20
}
```

---

#### `GET /api/etl/transform/cleaned-prices`

Returns the cleaned closing prices per ticker (consumed by Algorithm Service).

**Response:** `200 OK` — `Map<String, double[]>`

**Example:**
```bash
curl "http://localhost:8082/api/etl/transform/cleaned-prices"
```

**Response body example:**
```json
{
  "AAPL": [150.0, 151.3, 149.8, "..."],
  "MSFT": [310.5, 312.0, 308.7, "..."]
}
```

---

### Algorithm Endpoints

Base URL: `http://localhost:8083`

---

#### `POST /api/algorithm/run`

Triggers the full algorithmic analysis pipeline over cleaned prices fetched from the ETL service.

Computes: pairwise similarities, risk profiles, mean reversion patterns, and SMA for all assets.

**Response:** `202 Accepted` — `AnalysisSummary`

**Example:**
```bash
curl -X POST "http://localhost:8083/api/algorithm/run"
```

**Response body example:**
```json
{
  "similarityPairs": 190,
  "riskProfiles": 20,
  "patternRecords": 20,
  "smaRecords": 20
}
```

---

#### `GET /api/algorithm/similarity`

Returns all pairwise similarity results.

**Response:** `200 OK` — `List<SimilarityRecord>`

**Example:**
```bash
curl "http://localhost:8083/api/algorithm/similarity"
```

**Response body example:**
```json
[
  {
    "id": 1,
    "tickerA": "AAPL",
    "tickerB": "MSFT",
    "euclidean": 45.32,
    "pearson": 0.87,
    "dtw": 38.10,
    "cosine": 0.95,
    "computedAt": "2026-03-18T11:00:00"
  }
]
```

---

#### `GET /api/algorithm/risk`

Returns the risk ranking for all assets.

**Response:** `200 OK` — `List<RiskRecord>`

**Example:**
```bash
curl "http://localhost:8083/api/algorithm/risk"
```

**Response body example:**
```json
[
  {
    "id": 1,
    "ticker": "TSLA",
    "annualizedVolatility": 0.58,
    "category": "AGGRESSIVE",
    "computedAt": "2026-03-18T11:00:00"
  },
  {
    "id": 2,
    "ticker": "KO",
    "annualizedVolatility": 0.18,
    "category": "CONSERVATIVE",
    "computedAt": "2026-03-18T11:00:00"
  }
]
```

**Risk categories:**

| Category | Annualized Volatility |
|----------|-----------------------|
| `CONSERVATIVE` | Low |
| `MODERATE` | Medium |
| `AGGRESSIVE` | High |

---

#### `GET /api/algorithm/patterns`

Returns mean reversion pattern results for all assets.

**Response:** `200 OK` — `List<PatternRecord>`

**Example:**
```bash
curl "http://localhost:8083/api/algorithm/patterns"
```

**Response body example:**
```json
[
  {
    "id": 1,
    "symbol": "AAPL",
    "patternType": "MEAN_REVERSION",
    "occurrences": 42,
    "relativeFrequency": 0.33,
    "indicesJson": "[{\"startIndex\":10,\"price\":150.0,\"sma\":148.0,\"sigma\":2.1,\"direction\":\"ABOVE\"}]",
    "computedAt": "2026-03-18T11:00:00"
  }
]
```

---

#### `GET /api/algorithm/patterns/{symbol}`

Returns mean reversion pattern results for a specific asset.

**Path parameters:**

| Parameter | Type | Description |
|-----------|------|-------------|
| `symbol` | String | Ticker symbol (e.g. `AAPL`) |

**Response:** `200 OK` — `List<PatternRecord>` | `404 Not Found` (if no patterns computed for this symbol)

**Example:**
```bash
curl "http://localhost:8083/api/algorithm/patterns/AAPL"
```

---

#### `GET /api/algorithm/indicators/{symbol}/sma`

Returns the Simple Moving Average (SMA) series for a specific asset.

**Path parameters:**

| Parameter | Type | Description |
|-----------|------|-------------|
| `symbol` | String | Ticker symbol (e.g. `AAPL`) |

**Query parameters:**

| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| `window` | int | 20 | SMA window size in days |

**Response:** `200 OK` — `SmaRecord` | `404 Not Found`

**Example:**
```bash
curl "http://localhost:8083/api/algorithm/indicators/AAPL/sma?window=20"
```

**Response body example:**
```json
{
  "id": 1,
  "symbol": "AAPL",
  "window": 20,
  "valuesJson": "[148.5, 149.2, 150.1, \"...\"]",
  "computedAt": "2026-03-18T11:00:00"
}
```

---

### Report Endpoints

Base URL: `http://localhost:8084`

---

#### `POST /api/v1/reports/correlation-matrix/generate`

Generates and persists the full symmetric Pearson correlation matrix by fetching similarity data from the Algorithm Service.

The matrix includes both directions (A→B and B→A) plus diagonal entries (self-correlation = 1.0).

**Response:** `202 Accepted` — `CorrelationSummary`

**Example:**
```bash
curl -X POST "http://localhost:8084/api/v1/reports/correlation-matrix/generate"
```

**Response body example:**
```json
{
  "entriesCount": 420
}
```

---

#### `GET /api/v1/reports/correlation-matrix`

Returns all persisted correlation matrix entries.

**Response:** `200 OK` — `List<CorrelationEntry>`

**Example:**
```bash
curl "http://localhost:8084/api/v1/reports/correlation-matrix"
```

**Response body example:**
```json
[
  {
    "id": 1,
    "assetA": "AAPL",
    "assetB": "MSFT",
    "correlation": 0.87,
    "computedAt": "2026-03-18T12:00:00"
  },
  {
    "id": 2,
    "assetA": "MSFT",
    "assetB": "AAPL",
    "correlation": 0.87,
    "computedAt": "2026-03-18T12:00:00"
  }
]
```

---

#### `GET /api/v1/reports/export/pdf`

Generates and downloads a full PDF financial report.

The report includes 7 sections:
1. Cover / title
2. Similarity results table
3. Risk profiles table
4. Pattern detection results
5. Correlation matrix
6. SMA charts / data
7. Summary

**Response:** `200 OK` — `application/pdf` (attachment: `financial-report.pdf`)

**Example:**
```bash
curl "http://localhost:8084/api/v1/reports/export/pdf" \
  --output financial-report.pdf
```

---

## Data Models

### ETL Service

#### `FinancialSeries`

| Field | Type | Description |
|-------|------|-------------|
| `id` | Long | Primary key |
| `symbol` | String | Ticker symbol (unique) |
| `interval` | String | Data interval (e.g. `1day`) |
| `currency` | String | Trading currency |
| `exchange` | String | Exchange name |
| `exchangeTimezone` | String | Exchange timezone |
| `micCode` | String | MIC code |
| `type` | String | Asset type |
| `loadedAt` | LocalDateTime | Last load timestamp |
| `batchId` | Long | Associated batch ID |

#### `PriceRecord`

| Field | Type | Description |
|-------|------|-------------|
| `id` | Long | Primary key |
| `datetime` | LocalDate | Trading date |
| `open` | BigDecimal | Opening price |
| `high` | BigDecimal | Highest price |
| `low` | BigDecimal | Lowest price |
| `close` | BigDecimal | Closing price |
| `volume` | Long | Volume traded |
| `series` | FinancialSeries | Parent series (FK) |

#### `CleanedRecord`

| Field | Type | Description |
|-------|------|-------------|
| `id` | Long | Primary key |
| `symbol` | String | Ticker symbol |
| `date` | LocalDate | Trading date |
| `open` | BigDecimal | Cleaned opening price |
| `high` | BigDecimal | Cleaned highest price |
| `low` | BigDecimal | Cleaned lowest price |
| `close` | BigDecimal | Cleaned closing price |
| `volume` | Long | Volume traded |
| `dataQuality` | DataQuality | Quality label |
| `anomalyType` | String | Anomaly description (if flagged) |
| `originalClose` | BigDecimal | Original value before correction |
| `transformedAt` | LocalDateTime | Transformation timestamp |

**DataQuality enum:** `CLEAN` · `FORWARD_FILLED` · `ANOMALY_CORRECTED` · `ANOMALY_FLAGGED`

#### `BatchDownloadLog`

| Field | Type | Description |
|-------|------|-------------|
| `id` | Long | Primary key |
| `downloadDate` | LocalDate | Batch date |
| `status` | BatchStatus | Batch status |
| `startedAt` | LocalDateTime | Start timestamp |
| `completedAt` | LocalDateTime | End timestamp |
| `totalSymbols` | int | Total assets attempted |
| `downloadedSymbols` | int | Successfully downloaded |
| `failedSymbols` | int | Failed downloads |
| `errorMessage` | String | Error detail (if any) |

**BatchStatus enum:** `IN_PROGRESS` · `COMPLETED` · `COMPLETED_WITH_ERRORS` · `FAILED`

---

### Algorithm Service

#### `SimilarityRecord`

| Field | Type | Description |
|-------|------|-------------|
| `id` | Long | Primary key |
| `tickerA` | String | First asset |
| `tickerB` | String | Second asset |
| `euclidean` | double | Euclidean distance |
| `pearson` | double | Pearson correlation |
| `dtw` | double | Dynamic Time Warping distance |
| `cosine` | double | Cosine similarity |
| `computedAt` | LocalDateTime | Computation timestamp |

#### `RiskRecord`

| Field | Type | Description |
|-------|------|-------------|
| `id` | Long | Primary key |
| `ticker` | String | Asset ticker |
| `annualizedVolatility` | double | Annualized volatility (σ) |
| `category` | String | `CONSERVATIVE` / `MODERATE` / `AGGRESSIVE` |
| `computedAt` | LocalDateTime | Computation timestamp |

#### `PatternRecord`

| Field | Type | Description |
|-------|------|-------------|
| `id` | Long | Primary key |
| `symbol` | String | Asset ticker |
| `patternType` | String | Pattern type (e.g. `MEAN_REVERSION`) |
| `occurrences` | int | Number of pattern occurrences |
| `relativeFrequency` | double | Occurrence rate |
| `indicesJson` | String | Serialized pattern details (JSON) |
| `computedAt` | LocalDateTime | Computation timestamp |

#### `SmaRecord`

| Field | Type | Description |
|-------|------|-------------|
| `id` | Long | Primary key |
| `symbol` | String | Asset ticker |
| `window` | int | SMA window size |
| `valuesJson` | String | Serialized SMA values (JSON array) |
| `computedAt` | LocalDateTime | Computation timestamp |

---

### Report Service

#### `CorrelationEntry`

| Field | Type | Description |
|-------|------|-------------|
| `id` | Long | Primary key |
| `assetA` | String | First asset |
| `assetB` | String | Second asset |
| `correlation` | double | Pearson correlation coefficient |
| `computedAt` | LocalDateTime | Computation timestamp |

---

## Environment Variables

### ETL Service

| Variable | Default | Required |
|----------|---------|----------|
| `TWELVE_DATA_API_KEY` | — | **Yes** |
| `SERVER_PORT` | `8082` | No |
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://localhost:5432/etl_db` | No |
| `SPRING_DATASOURCE_USERNAME` | `postgres` | No |
| `SPRING_DATASOURCE_PASSWORD` | `postgres` | No |

### Algorithm Service

| Variable | Default | Required |
|----------|---------|----------|
| `SERVER_PORT` | `8083` | No |
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://localhost:5432/algorithm_db` | No |
| `SPRING_DATASOURCE_USERNAME` | `financial_user` | No |
| `SPRING_DATASOURCE_PASSWORD` | `financial_pass` | No |
| `ETL_SERVICE_URL` | `http://localhost:8082` | No |

### Report Service

| Variable | Default | Required |
|----------|---------|----------|
| `SERVER_PORT` | `8084` | No |
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://localhost:5432/report_db` | No |
| `SPRING_DATASOURCE_USERNAME` | `postgres` | No |
| `SPRING_DATASOURCE_PASSWORD` | `postgres` | No |
| `ALGORITHM_SERVICE_URL` | `http://localhost:8083` | No |

---

## Build & Test

```bash
# Build all modules (skip tests)
./mvnw.cmd clean install -DskipTests

# Run unit tests only
./mvnw.cmd test

# Run unit + integration tests (requires TWELVE_DATA_API_KEY)
./mvnw.cmd clean verify -Pintegration

# Run tests for a single module
./mvnw.cmd test -pl etl-service

# Run a single test class
./mvnw.cmd test -pl etl-service -Dtest=TwelveDataHttpClientTest

# SonarQube analysis
./mvnw.cmd clean verify sonar:sonar -Dsonar.coverage.jacoco.xmlReportPaths=**/target/site/jacoco/jacoco.xml

# Allure test report
./mvnw.cmd allure:serve
```

**Coverage:** JaCoCo — reports at `target/site/jacoco/jacoco.xml`.

SonarQube excludes `*Application.java`, `**/config/**`, and `**/exception/**` from coverage analysis.
