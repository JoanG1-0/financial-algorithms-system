// API Client for FinAlgo Dashboard
// Base URL: http://localhost:8081 (API Gateway)
import type {
  DashboardSummary,
  Asset,
  PriceRecord,
  CleanedPriceRecord,
  TransformStatus,
  BatchDownloadLog,
  RiskRecord,
  SimilarityRecord,
  PatternRecord,
  SmaRecord,
  CorrelationEntry,
  TransformSummary,
  AnalysisSummary,
  CorrelationMatrixGenerated,
  PaginatedResponse,
} from './types'

const BASE_URL = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8081'

class ApiClient {
  private async fetch<T>(endpoint: string, options?: RequestInit): Promise<T> {
    const url = `${BASE_URL}${endpoint}`
    
    const response = await fetch(url, {
      ...options,
      headers: {
        'Content-Type': 'application/json',
        ...options?.headers,
      },
    })

    if (response.status === 204) {
      return {} as T
    }

    if (!response.ok) {
      const errorBody = await response.text()
      throw new Error(`API Error: ${response.status} ${response.statusText} - ${errorBody}`)
    }

    const text = await response.text()
    if (!text) return {} as T
    
    return JSON.parse(text) as T
  }

  // ============================================
  // 1. Dashboard Summary
  // ============================================
  async getDashboardSummary(): Promise<DashboardSummary> {
    return this.fetch<DashboardSummary>('/api/algorithm/dashboard-summary')
  }

  // ============================================
  // 2. Assets List
  // ============================================
  async getAssets(): Promise<Asset[]> {
    return this.fetch<Asset[]>('/api/etl/assets')
  }

  // ============================================
  // 3. Price History (paginated)
  // ============================================
  async getPriceHistory(
    symbol: string, 
    page: number = 0, 
    size: number = 30
  ): Promise<PaginatedResponse<PriceRecord>> {
    return this.fetch<PaginatedResponse<PriceRecord>>(
      `/api/etl/series/${symbol}/prices?page=${page}&size=${size}`
    )
  }

  // ============================================
  // 4. Cleaned Prices (paginated)
  // ============================================
  async getCleanedPrices(
    symbol: string, 
    page: number = 0, 
    size: number = 30
  ): Promise<PaginatedResponse<CleanedPriceRecord>> {
    return this.fetch<PaginatedResponse<CleanedPriceRecord>>(
      `/api/etl/series/${symbol}/cleaned?page=${page}&size=${size}`
    )
  }

  // ============================================
  // 5. Data Quality Status
  // ============================================
  async getTransformStatus(): Promise<TransformStatus> {
    return this.fetch<TransformStatus>('/api/etl/transform/status')
  }

  // ============================================
  // 6. Batch Download Status
  // ============================================
  async getBatchStatus(): Promise<BatchDownloadLog | null> {
    try {
      return await this.fetch<BatchDownloadLog>('/api/etl/batch/status')
    } catch (error) {
      // Returns 204 if no batch has been triggered
      if (error instanceof Error && error.message.includes('204')) {
        return null
      }
      throw error
    }
  }

  // ============================================
  // 7. Risk Ranking
  // ============================================
  async getRiskRanking(): Promise<RiskRecord[]> {
    return this.fetch<RiskRecord[]>('/api/algorithm/risk')
  }

  // ============================================
  // 8. Similarity Matrix
  // ============================================
  async getSimilarityMatrix(): Promise<SimilarityRecord[]> {
    return this.fetch<SimilarityRecord[]>('/api/algorithm/similarity')
  }

  // ============================================
  // 9. Pattern Detection — All Assets
  // ============================================
  async getAllPatterns(): Promise<PatternRecord[]> {
    return this.fetch<PatternRecord[]>('/api/algorithm/patterns')
  }

  // ============================================
  // 10. Pattern Detection — Single Asset
  // ============================================
  async getPatternsBySymbol(symbol: string): Promise<PatternRecord[]> {
    return this.fetch<PatternRecord[]>(`/api/algorithm/patterns/${symbol}`)
  }

  // ============================================
  // 11. SMA Indicator
  // ============================================
  async getSMA(symbol: string, window: number = 20): Promise<SmaRecord> {
    return this.fetch<SmaRecord>(`/api/algorithm/indicators/${symbol}/sma?window=${window}`)
  }

  // ============================================
  // 12. Correlation Matrix
  // ============================================
  async getCorrelationMatrix(): Promise<CorrelationEntry[]> {
    return this.fetch<CorrelationEntry[]>('/api/v1/reports/correlation-matrix')
  }

  // ============================================
  // 13. PDF Report Download
  // ============================================
  async downloadPDF(): Promise<Blob> {
    const url = `${BASE_URL}/api/v1/reports/export/pdf`
    const response = await fetch(url)
    
    if (!response.ok) {
      throw new Error(`API Error: ${response.status} ${response.statusText}`)
    }
    
    return response.blob()
  }

  getPDFUrl(): string {
    return `${BASE_URL}/api/v1/reports/export/pdf`
  }

  // ============================================
  // 14. Action Endpoints (triggers)
  // ============================================
  async triggerBatchDownload(): Promise<string> {
    const response = await fetch(`${BASE_URL}/api/etl/batch/trigger`, { method: 'POST' })
    if (!response.ok) {
      throw new Error(`API Error: ${response.status}`)
    }
    return response.text()
  }

  async runTransform(): Promise<TransformSummary> {
    return this.fetch<TransformSummary>('/api/etl/transform', { method: 'POST' })
  }

  async runFullAnalysis(): Promise<AnalysisSummary> {
    return this.fetch<AnalysisSummary>('/api/algorithm/run', { method: 'POST' })
  }

  async generateCorrelationMatrix(): Promise<CorrelationMatrixGenerated> {
    return this.fetch<CorrelationMatrixGenerated>(
      '/api/v1/reports/correlation-matrix/generate', 
      { method: 'POST' }
    )
  }
}

export const api = new ApiClient()
export { BASE_URL }
