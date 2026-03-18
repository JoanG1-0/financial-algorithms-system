# Frontend API Guide — Financial Algorithms System

This document describes every available REST endpoint, the exact JSON shape it returns,
and the recommended UI component to display it.

**Base URL (single entry point):** `http://localhost:8081`
All requests go through the API Gateway. No CORS issues — the gateway handles it.

---

## Table of Contents

1. [Dashboard Summary](#1-dashboard-summary)
2. [Assets List](#2-assets-list)
3. [Price History (paginated)](#3-price-history-paginated)
4. [Cleaned Prices (paginated)](#4-cleaned-prices-paginated)
5. [Data Quality Status](#5-data-quality-status)
6. [Batch Download Status](#6-batch-download-status)
7. [Risk Ranking](#7-risk-ranking)
8. [Similarity Matrix](#8-similarity-matrix)
9. [Pattern Detection — All Assets](#9-pattern-detection--all-assets)
10. [Pattern Detection — Single Asset](#10-pattern-detection--single-asset)
11. [SMA Indicator](#11-sma-indicator)
12. [Correlation Matrix](#12-correlation-matrix)
13. [PDF Report Download](#13-pdf-report-download)
14. [Action Endpoints (triggers)](#14-action-endpoints-triggers)
15. [Pagination Shape](#15-pagination-shape)
16. [Suggested Page Layout](#16-suggested-page-layout)

---

## 1. Dashboard Summary

**`GET /api/algorithm/dashboard-summary`**

One call to power all dashboard cards. Call this on page load.

### Response

```json
{
  "totalAssets": 20,
  "totalSimilarityPairs": 190,
  "lastAnalysisAt": "2026-03-18T11:00:00",
  "riskDistribution": {
    "CONSERVATIVE": 7,
    "MODERATE": 9,
    "AGGRESSIVE": 4
  },
  "topRiskAsset": {
    "id": 3,
    "ticker": "TSLA",
    "annualizedVolatility": 0.58,
    "category": "AGGRESSIVE",
    "computedAt": "2026-03-18T11:00:00"
  },
  "lowestRiskAsset": {
    "id": 12,
    "ticker": "KO",
    "annualizedVolatility": 0.18,
    "category": "CONSERVATIVE",
    "computedAt": "2026-03-18T11:00:00"
  }
}
```

### UI Mapping

| Field | Component |
|-------|-----------|
| `totalAssets` | Stat card — "Total Assets" |
| `totalSimilarityPairs` | Stat card — "Similarity Pairs Analyzed" |
| `lastAnalysisAt` | Stat card — "Last Analysis" (format as date) |
| `riskDistribution` | Donut / Pie chart with 3 slices: CONSERVATIVE (green), MODERATE (yellow), AGGRESSIVE (red) |
| `topRiskAsset.ticker` + `annualizedVolatility` | Highlight card — "Highest Risk Asset" |
| `lowestRiskAsset.ticker` + `annualizedVolatility` | Highlight card — "Lowest Risk Asset" |

---

## 2. Assets List

**`GET /api/etl/assets`**

Returns all tracked assets with their metadata. No pagination — always 20 records.

### Response

```json
[
  {
    "id": 1,
    "symbol": "AAPL",
    "interval": "1day",
    "currency": "USD",
    "exchange": "NASDAQ",
    "exchangeTimezone": "America/New_York",
    "micCode": "XNAS",
    "type": "Common Stock",
    "loadedAt": "2026-03-18T10:05:00",
    "batchId": 1
  },
  {
    "id": 2,
    "symbol": "MSFT",
    "interval": "1day",
    "currency": "USD",
    "exchange": "NASDAQ",
    "exchangeTimezone": "America/New_York",
    "micCode": "XNAS",
    "type": "Common Stock",
    "loadedAt": "2026-03-18T10:05:10",
    "batchId": 1
  }
]
```

**Tracked symbols:** AAPL, MSFT, GOOGL, AMZN, META, TSLA, NVDA, JPM, JNJ, V, NFLX, AMD, INTC, DIS, WMT, BAC, KO, PG, IBM, ORCL

### UI Mapping

| Field | Component |
|-------|-----------|
| `symbol` | Table column — clickable, navigates to asset detail page |
| `exchange` | Table column |
| `currency` | Table column |
| `type` | Table column — badge |
| `loadedAt` | Table column — formatted date |

**Recommended:** Data table with a search/filter bar. Clicking a row opens the asset detail page.

---

## 3. Price History (paginated)

**`GET /api/etl/series/{symbol}/prices?page=0&size=30`**

Returns the raw OHLCV price history for one asset, ordered newest → oldest.
Use `symbol` from the assets list. Default page size: 30.

### Path Parameters

| Param | Example | Description |
|-------|---------|-------------|
| `symbol` | `AAPL` | Ticker symbol |

### Query Parameters

| Param | Default | Description |
|-------|---------|-------------|
| `page` | `0` | Page number (0-indexed) |
| `size` | `30` | Records per page |

### Response

```json
{
  "content": [
    {
      "id": 1260,
      "datetime": "2026-03-18",
      "open": 170.10,
      "high": 172.50,
      "low": 169.80,
      "close": 171.30,
      "volume": 52000000
    },
    {
      "id": 1259,
      "datetime": "2026-03-17",
      "open": 168.40,
      "high": 171.00,
      "low": 167.90,
      "close": 170.10,
      "volume": 48000000
    }
  ],
  "totalElements": 1260,
  "totalPages": 42,
  "number": 0,
  "size": 30,
  "first": true,
  "last": false
}
```

### UI Mapping

| Field | Component |
|-------|-----------|
| `content[].datetime` + `close` | Line chart (x = date, y = close price) |
| `content[]` all OHLC | Candlestick chart (open/high/low/close) |
| `content[].volume` | Bar chart below the price chart |
| `totalElements` | "Showing X records" label |
| `totalPages` / `number` | Pagination controls (prev/next, page numbers) |

**Tip:** For the chart, fetch all pages or increase `size` to 250 (1 year) or 1260 (5 years). For the table view, keep `size=30` with pagination controls.

---

## 4. Cleaned Prices (paginated)

**`GET /api/etl/series/{symbol}/cleaned?page=0&size=30`**

Same as price history but returns the cleaned/transformed version with a data quality label.
Useful to show users which data points were corrected or filled.

### Query Parameters

Same as endpoint 3: `page`, `size`.

### Response

```json
{
  "content": [
    {
      "id": 1260,
      "symbol": "AAPL",
      "date": "2026-03-18",
      "open": 170.10,
      "high": 172.50,
      "low": 169.80,
      "close": 171.30,
      "volume": 52000000,
      "dataQuality": "CLEAN",
      "anomalyType": null,
      "originalClose": null,
      "transformedAt": "2026-03-18T10:10:00"
    },
    {
      "id": 950,
      "symbol": "AAPL",
      "date": "2025-07-04",
      "open": 165.00,
      "high": 165.00,
      "low": 165.00,
      "close": 165.00,
      "volume": 0,
      "dataQuality": "FORWARD_FILLED",
      "anomalyType": null,
      "originalClose": null,
      "transformedAt": "2026-03-18T10:10:00"
    },
    {
      "id": 502,
      "symbol": "AAPL",
      "date": "2024-11-15",
      "open": 150.00,
      "high": 150.00,
      "low": 150.00,
      "close": 152.00,
      "volume": 45000000,
      "dataQuality": "ANOMALY_CORRECTED",
      "anomalyType": "Z_SCORE_SPIKE",
      "originalClose": 210.00,
      "transformedAt": "2026-03-18T10:10:00"
    }
  ],
  "totalElements": 1260,
  "totalPages": 42,
  "number": 0,
  "size": 30,
  "first": true,
  "last": false
}
```

### Data Quality Values

| Value | Meaning | Suggested color |
|-------|---------|-----------------|
| `CLEAN` | Normal record | Green |
| `FORWARD_FILLED` | Missing date filled with last known price | Yellow |
| `ANOMALY_CORRECTED` | Statistical outlier — value was corrected | Orange |
| `ANOMALY_FLAGGED` | Statistical outlier — value was flagged but kept | Red |

### UI Mapping

| Field | Component |
|-------|-----------|
| `dataQuality` | Badge / colored dot on each row |
| `anomalyType` | Tooltip on hover when not null |
| `originalClose` | Show original vs corrected value in a diff tooltip |
| Table rows | Color-coded by `dataQuality` |

---

## 5. Data Quality Status

**`GET /api/etl/transform/status`**

Returns the count of cleaned records grouped by quality category. No pagination.

### Response

```json
{
  "CLEAN": 24800,
  "FORWARD_FILLED": 300,
  "ANOMALY_CORRECTED": 80,
  "ANOMALY_FLAGGED": 20
}
```

### UI Mapping

| Data | Component |
|------|-----------|
| All 4 keys + values | Donut / Pie chart with color coding |
| All 4 keys + values | Horizontal stacked bar chart |
| Each key + value | Stat cards in a 2×2 grid |

---

## 6. Batch Download Status

**`GET /api/etl/batch/status`**

Returns the status of the last batch download.
Returns `204 No Content` if no batch has been triggered yet.

### Response

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

### Status Values

| Value | Meaning |
|-------|---------|
| `IN_PROGRESS` | Download is running |
| `COMPLETED` | All symbols downloaded successfully |
| `COMPLETED_WITH_ERRORS` | Some symbols failed |
| `FAILED` | Download failed entirely |

### UI Mapping

| Field | Component |
|-------|-----------|
| `status` | Status badge (green/yellow/red) |
| `downloadedSymbols` / `totalSymbols` | Progress bar: `downloadedSymbols / totalSymbols` |
| `failedSymbols` | Error count badge (red) |
| `startedAt` / `completedAt` | Duration calculation and display |

---

## 7. Risk Ranking

**`GET /api/algorithm/risk`**

Returns all 20 assets ranked by risk. No pagination.

### Response

```json
[
  {
    "id": 1,
    "ticker": "KO",
    "annualizedVolatility": 0.18,
    "category": "CONSERVATIVE",
    "computedAt": "2026-03-18T11:00:00"
  },
  {
    "id": 2,
    "ticker": "JPM",
    "annualizedVolatility": 0.28,
    "category": "MODERATE",
    "computedAt": "2026-03-18T11:00:00"
  },
  {
    "id": 3,
    "ticker": "TSLA",
    "annualizedVolatility": 0.58,
    "category": "AGGRESSIVE",
    "computedAt": "2026-03-18T11:00:00"
  }
]
```

### Category Values

| Value | Suggested color |
|-------|-----------------|
| `CONSERVATIVE` | Green |
| `MODERATE` | Yellow / Amber |
| `AGGRESSIVE` | Red |

### UI Mapping

| Field | Component |
|-------|-----------|
| `ticker` + `annualizedVolatility` | Horizontal bar chart sorted by volatility |
| `category` | Color-coded badge in table column |
| All records | Sortable data table with filter by category |
| `annualizedVolatility` | Format as percentage: `(0.28 * 100).toFixed(1) + "%"` |

---

## 8. Similarity Matrix

**`GET /api/algorithm/similarity`**

Returns all pairwise similarity scores (190 pairs for 20 assets). No pagination.

### Response

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
  },
  {
    "id": 2,
    "tickerA": "AAPL",
    "tickerB": "GOOGL",
    "euclidean": 62.10,
    "pearson": 0.74,
    "dtw": 55.40,
    "cosine": 0.89,
    "computedAt": "2026-03-18T11:00:00"
  }
]
```

### Metric Guide

| Metric | Range | Meaning |
|--------|-------|---------|
| `pearson` | -1.0 to 1.0 | Correlation: 1.0 = move together, -1.0 = opposite |
| `cosine` | 0.0 to 1.0 | Directional similarity: 1.0 = identical direction |
| `euclidean` | 0 to ∞ | Distance: lower = more similar |
| `dtw` | 0 to ∞ | Time-warped distance: lower = more similar |

### UI Mapping

| Data | Component |
|------|-----------|
| `pearson` values as 20×20 grid | **Heatmap** — best visualization. Color: blue (negative) → white (0) → red (positive). Note: the endpoint only returns one direction per pair (A→B), so fill B→A with the same value and diagonal with 1.0 |
| All records | Sortable table with columns: Asset A, Asset B, Pearson, Cosine, Euclidean, DTW |
| Top 10 most similar pairs by `pearson` | Ranked list with badges |

**Heatmap construction tip:** Build a 20×20 matrix. For each record, set `matrix[tickerA][tickerB] = pearson` and `matrix[tickerB][tickerA] = pearson`. Set diagonal to 1.0.

---

## 9. Pattern Detection — All Assets

**`GET /api/algorithm/patterns`**

Returns pattern detection results for all 20 assets (2 patterns per asset = 40 records).

### Response

```json
[
  {
    "id": 1,
    "symbol": "AAPL",
    "patternType": "CONSECUTIVE_UP",
    "occurrences": 38,
    "relativeFrequency": 0.30,
    "indicesJson": "[10, 45, 89, 134]",
    "computedAt": "2026-03-18T11:00:00"
  },
  {
    "id": 2,
    "symbol": "AAPL",
    "patternType": "MEAN_REVERSION",
    "occurrences": 42,
    "relativeFrequency": 0.33,
    "indicesJson": "[22, 67, 112, 198]",
    "computedAt": "2026-03-18T11:00:00"
  }
]
```

### Pattern Types

| Value | Meaning |
|-------|---------|
| `CONSECUTIVE_UP` | Asset closed higher than previous day for K consecutive days |
| `MEAN_REVERSION` | Asset price deviated significantly from its moving average then returned |

### UI Mapping

| Field | Component |
|-------|-----------|
| `symbol` + `occurrences` | Bar chart grouped by `patternType` |
| `relativeFrequency` | Format as `(0.33 * 100).toFixed(1) + "%"` |
| `patternType` | Filter tabs or toggle buttons |
| All records | Table grouped by symbol with pattern breakdown |

---

## 10. Pattern Detection — Single Asset

**`GET /api/algorithm/patterns/{symbol}`**

Returns patterns for a specific asset. Returns `404` if analysis has not been run.

### Path Parameters

| Param | Example |
|-------|---------|
| `symbol` | `AAPL` |

### Response

Same shape as endpoint 9 but filtered to the requested symbol (2 records).

```json
[
  {
    "id": 1,
    "symbol": "AAPL",
    "patternType": "CONSECUTIVE_UP",
    "occurrences": 38,
    "relativeFrequency": 0.30,
    "indicesJson": "[10, 45, 89, 134]",
    "computedAt": "2026-03-18T11:00:00"
  },
  {
    "id": 2,
    "symbol": "AAPL",
    "patternType": "MEAN_REVERSION",
    "occurrences": 42,
    "relativeFrequency": 0.33,
    "indicesJson": "[22, 67, 112, 198]",
    "computedAt": "2026-03-18T11:00:00"
  }
]
```

### UI Mapping

Used on the **asset detail page**. Display as two stat cards with a small bar or ring chart each.

---

## 11. SMA Indicator

**`GET /api/algorithm/indicators/{symbol}/sma?window=20`**

Returns the Simple Moving Average series for one asset.
Returns `404` if analysis has not been run for that symbol.

### Path Parameters

| Param | Example |
|-------|---------|
| `symbol` | `AAPL` |

### Query Parameters

| Param | Default | Description |
|-------|---------|-------------|
| `window` | `20` | SMA window in days |

### Response

```json
{
  "id": 1,
  "symbol": "AAPL",
  "window": 20,
  "valuesJson": "[148.5, 149.2, 150.1, 151.3, 150.8]",
  "computedAt": "2026-03-18T11:00:00"
}
```

> `valuesJson` is a JSON string — parse it with `JSON.parse(record.valuesJson)` to get a `number[]`.

### UI Mapping

| Field | Component |
|-------|-----------|
| `JSON.parse(valuesJson)` | Line overlay on the price chart (dashed line) |
| `window` | Label: "SMA-20" |

**Overlay tip:** Fetch price history and SMA together. Align by index (SMA array starts at index `window-1`, so offset accordingly). Plot SMA as a dashed line over the closing price line.

---

## 12. Correlation Matrix

**`GET /api/v1/reports/correlation-matrix`**

Returns all Pearson correlation entries including both directions (A→B and B→A) and diagonal (self = 1.0).
Total records: 20×20 = 400.

### Response

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
  },
  {
    "id": 3,
    "assetA": "AAPL",
    "assetB": "AAPL",
    "correlation": 1.0,
    "computedAt": "2026-03-18T12:00:00"
  }
]
```

### UI Mapping

| Data | Component |
|------|-----------|
| All 400 entries | **Heatmap** — this is already symmetric and includes diagonal, so you can directly map it to a 20×20 grid |
| Color scale | -1.0 = blue, 0.0 = white, 1.0 = red |
| Hover tooltip | Show "AAPL ↔ MSFT: 0.87" |

**Grid construction:**
```js
// Build ordered list of unique tickers
const tickers = [...new Set(data.map(e => e.assetA))].sort()

// Build matrix[row][col] = correlation
const matrix = {}
data.forEach(e => {
  if (!matrix[e.assetA]) matrix[e.assetA] = {}
  matrix[e.assetA][e.assetB] = e.correlation
})
```

---

## 13. PDF Report Download

**`GET /api/v1/reports/export/pdf`**

Downloads the full financial analysis report as a PDF file.

### Response

- Content-Type: `application/pdf`
- Content-Disposition: `attachment; filename=financial-report.pdf`
- Body: binary PDF

### UI Mapping

| Component | Implementation |
|-----------|----------------|
| Download button | `<a href="http://localhost:8081/api/v1/reports/export/pdf" download>Download Report</a>` |
| Or fetch in JS | `window.open('http://localhost:8081/api/v1/reports/export/pdf')` |

---

## 14. Action Endpoints (triggers)

These endpoints **start background processes**. Show a loading state while polling for results.

| Endpoint | Method | What it does | Response |
|----------|--------|--------------|----------|
| `POST /api/etl/batch/trigger` | POST | Downloads 20 assets from TwelveData (takes ~5 min) | `202` + message string |
| `POST /api/etl/transform` | POST | Runs the cleaning pipeline | `202` + TransformSummary |
| `POST /api/algorithm/run` | POST | Runs all algorithms | `202` + AnalysisSummary |
| `POST /api/v1/reports/correlation-matrix/generate` | POST | Builds correlation matrix | `202` + `{ "entriesCount": 400 }` |

### TransformSummary (response of POST /api/etl/transform)

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

### AnalysisSummary (response of POST /api/algorithm/run)

```json
{
  "similarityPairs": 190,
  "riskProfiles": 20,
  "patternRecords": 40,
  "smaRecords": 20
}
```

### UI Pattern for action buttons

```
1. User clicks "Run Analysis"
2. Show spinner / loading state
3. POST /api/algorithm/run
4. On 202 response → show success toast with summary data
5. Refetch dashboard summary to update cards
```

---

## 15. Pagination Shape

All paginated endpoints return this envelope:

```json
{
  "content": [],
  "totalElements": 1260,
  "totalPages": 42,
  "number": 0,
  "size": 30,
  "first": true,
  "last": false,
  "numberOfElements": 30,
  "empty": false
}
```

| Field | Use |
|-------|-----|
| `content` | The actual records for this page |
| `totalElements` | Show "1,260 records" label |
| `totalPages` | Total number of page buttons |
| `number` | Current page (0-indexed) |
| `first` / `last` | Disable prev/next buttons |

---

## 16. Suggested Page Layout

```
App
├── /                         → Dashboard
│     ├── 4 stat cards        (GET /api/algorithm/dashboard-summary)
│     ├── Donut chart         (riskDistribution from dashboard-summary)
│     ├── Donut chart         (GET /api/etl/transform/status)
│     └── Batch status bar    (GET /api/etl/batch/status)
│
├── /assets                   → Assets Table
│     └── Table of 20 assets  (GET /api/etl/assets)
│           └── Click row → /assets/:symbol
│
├── /assets/:symbol           → Asset Detail
│     ├── Price line chart    (GET /api/etl/series/:symbol/prices?size=250)
│     ├── SMA overlay         (GET /api/algorithm/indicators/:symbol/sma?window=20)
│     ├── Cleaned data table  (GET /api/etl/series/:symbol/cleaned?page=0&size=30)
│     └── Pattern cards       (GET /api/algorithm/patterns/:symbol)
│
├── /risk                     → Risk Ranking
│     └── Bar chart + table   (GET /api/algorithm/risk)
│
├── /similarity               → Similarity Analysis
│     └── Heatmap + table     (GET /api/algorithm/similarity)
│
├── /correlation              → Correlation Matrix
│     └── Heatmap 20×20       (GET /api/v1/reports/correlation-matrix)
│
├── /patterns                 → Pattern Detection
│     └── Table all assets    (GET /api/algorithm/patterns)
│
└── /reports                  → Reports
      └── Download PDF button (GET /api/v1/reports/export/pdf)
```

---

## Error Responses

All errors from the gateway return this JSON shape:

```json
{
  "status": 503,
  "error": "Service Unavailable",
  "path": "/api/algorithm/run"
}
```

| Status | Meaning | UI action |
|--------|---------|-----------|
| `404` | Resource not found (analysis not run yet) | Show empty state with "Run Analysis" button |
| `503` | A downstream service is down | Show error toast: "Service unavailable, try again later" |
| `500` | Internal error | Show error toast with the `path` field |
