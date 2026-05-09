// API Response Types for FinAlgo Dashboard
// Based on: Frontend API Guide — Financial Algorithms System

// ============================================
// 1. Dashboard Summary
// GET /api/algorithm/dashboard-summary
// ============================================
export interface DashboardSummary {
  totalAssets: number
  totalSimilarityPairs: number
  lastAnalysisAt: string
  riskDistribution: {
    CONSERVATIVE: number
    MODERATE: number
    AGGRESSIVE: number
  }
  topRiskAsset: RiskRecord
  lowestRiskAsset: RiskRecord
}

// ============================================
// 2. Assets List
// GET /api/etl/assets
// ============================================
export interface Asset {
  id: number
  symbol: string
  interval: string
  currency: string
  exchange: string
  exchangeTimezone: string
  micCode: string
  type: string
  loadedAt: string
  batchId: number
}

// ============================================
// 3. Price History (paginated)
// GET /api/etl/series/{symbol}/prices?page=0&size=30
// ============================================
export interface PriceRecord {
  id: number
  datetime: string
  open: number
  high: number
  low: number
  close: number
  volume: number
}

// ============================================
// 4. Cleaned Prices (paginated)
// GET /api/etl/series/{symbol}/cleaned?page=0&size=30
// ============================================
export type DataQuality = 'CLEAN' | 'FORWARD_FILLED' | 'ANOMALY_CORRECTED' | 'ANOMALY_FLAGGED'
export type AnomalyType = 'Z_SCORE_SPIKE' | null

export interface CleanedPriceRecord {
  id: number
  symbol: string
  date: string
  open: number
  high: number
  low: number
  close: number
  volume: number
  dataQuality: DataQuality
  anomalyType: AnomalyType
  originalClose: number | null
  transformedAt: string
}

// ============================================
// 5. Data Quality Status
// GET /api/etl/transform/status
// ============================================
export interface TransformStatus {
  CLEAN: number
  FORWARD_FILLED: number
  ANOMALY_CORRECTED: number
  ANOMALY_FLAGGED: number
}

// ============================================
// 6. Batch Download Status
// GET /api/etl/batch/status
// ============================================
export type BatchStatus = 'IN_PROGRESS' | 'COMPLETED' | 'COMPLETED_WITH_ERRORS' | 'FAILED'

export interface BatchDownloadLog {
  id: number
  downloadDate: string
  status: BatchStatus
  startedAt: string
  completedAt: string | null
  totalSymbols: number
  downloadedSymbols: number
  failedSymbols: number
  errorMessage: string | null
}

// ============================================
// 7. Risk Ranking
// GET /api/algorithm/risk
// ============================================
export type RiskCategory = 'CONSERVATIVE' | 'MODERATE' | 'AGGRESSIVE'

export interface RiskRecord {
  id: number
  ticker: string
  annualizedVolatility: number
  category: RiskCategory
  computedAt: string
}

// ============================================
// 8. Similarity Matrix
// GET /api/algorithm/similarity
// ============================================
export interface SimilarityRecord {
  id: number
  tickerA: string
  tickerB: string
  euclidean: number
  pearson: number
  dtw: number
  cosine: number
  computedAt: string
}

// ============================================
// 9 & 10. Pattern Detection
// GET /api/algorithm/patterns
// GET /api/algorithm/patterns/{symbol}
// ============================================
export type PatternType = 'CONSECUTIVE_UP' | 'MEAN_REVERSION'

export interface PatternRecord {
  id: number
  symbol: string
  patternType: PatternType
  occurrences: number
  relativeFrequency: number
  indicesJson: string // JSON array of indices: "[10, 45, 89, 134]"
  computedAt: string
}

// ============================================
// 11. SMA Indicator
// GET /api/algorithm/indicators/{symbol}/sma?window=20
// ============================================
export interface SmaRecord {
  id: number
  symbol: string
  window: number
  valuesJson: string // JSON array of numbers: "[148.5, 149.2, 150.1]"
  computedAt: string
}

// ============================================
// 12. Correlation Matrix
// GET /api/v1/reports/correlation-matrix
// ============================================
export interface CorrelationEntry {
  id: number
  assetA: string
  assetB: string
  correlation: number
  computedAt: string
}

// ============================================
// 14. Action Endpoints Responses
// ============================================
export interface TransformSummary {
  symbolsProcessed: number
  calendarDays: number
  totalRecords: number
  cleanRecords: number
  forwardFilledRecords: number
  anomalyCorrectedRecords: number
  anomalyFlaggedRecords: number
  transformedAt: string
}

export interface AnalysisSummary {
  similarityPairs: number
  riskProfiles: number
  patternRecords: number
  smaRecords: number
}

export interface CorrelationMatrixGenerated {
  entriesCount: number
}

// ============================================
// 15. Pagination Shape
// ============================================
export interface PaginatedResponse<T> {
  content: T[]
  totalElements: number
  totalPages: number
  number: number
  size: number
  first: boolean
  last: boolean
  numberOfElements: number
  empty: boolean
}

// ============================================
// Error Response
// ============================================
export interface ApiError {
  status: number
  error: string
  path: string
}
