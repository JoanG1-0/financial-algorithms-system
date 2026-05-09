"use client"

import { useState, useEffect } from "react"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Button } from "@/components/ui/button"
import { Badge } from "@/components/ui/badge"
import { Spinner } from "@/components/ui/spinner"
import { 
  Activity, 
  TrendingUp, 
  TrendingDown, 
  Clock, 
  BarChart3, 
  AlertTriangle,
  CheckCircle2,
  XCircle,
  RefreshCw,
  Database,
  Cpu
} from "lucide-react"
import { 
  PieChart, 
  Pie, 
  Cell, 
  ResponsiveContainer, 
  Tooltip,
  Legend
} from "recharts"
import { api } from "@/lib/api/client"
import type { 
  DashboardSummary, 
  TransformStatus, 
  BatchDownloadLog 
} from "@/lib/api/types"
import { StatCard, StatusBadge } from "@/components/dashboard/shared"

const RISK_COLORS = {
  CONSERVATIVE: "#10B981",
  MODERATE: "#F59E0B", 
  AGGRESSIVE: "#EF4444"
}

const QUALITY_COLORS = {
  CLEAN: "#10B981",
  FORWARD_FILLED: "#F59E0B",
  ANOMALY_CORRECTED: "#F97316",
  ANOMALY_FLAGGED: "#EF4444"
}

export default function DashboardPage() {
  const [summary, setSummary] = useState<DashboardSummary | null>(null)
  const [transformStatus, setTransformStatus] = useState<TransformStatus | null>(null)
  const [batchStatus, setBatchStatus] = useState<BatchDownloadLog | null>(null)
  const [loading, setLoading] = useState(true)
  const [runningAnalysis, setRunningAnalysis] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const fetchData = async () => {
    try {
      setLoading(true)
      setError(null)
      
      const [summaryData, transformData, batchData] = await Promise.all([
        api.getDashboardSummary().catch(() => null),
        api.getTransformStatus().catch(() => null),
        api.getBatchStatus().catch(() => null)
      ])
      
      setSummary(summaryData)
      setTransformStatus(transformData)
      setBatchStatus(batchData)
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to fetch data")
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    fetchData()
  }, [])

  const handleRunAnalysis = async () => {
    try {
      setRunningAnalysis(true)
      await api.runFullAnalysis()
      await fetchData()
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to run analysis")
    } finally {
      setRunningAnalysis(false)
    }
  }

  const riskChartData = summary ? [
    { name: "Conservative", value: summary.riskDistribution.CONSERVATIVE, color: RISK_COLORS.CONSERVATIVE },
    { name: "Moderate", value: summary.riskDistribution.MODERATE, color: RISK_COLORS.MODERATE },
    { name: "Aggressive", value: summary.riskDistribution.AGGRESSIVE, color: RISK_COLORS.AGGRESSIVE }
  ] : []

  const qualityChartData = transformStatus ? [
    { name: "Clean", value: transformStatus.CLEAN, color: QUALITY_COLORS.CLEAN },
    { name: "Forward Filled", value: transformStatus.FORWARD_FILLED, color: QUALITY_COLORS.FORWARD_FILLED },
    { name: "Anomaly Corrected", value: transformStatus.ANOMALY_CORRECTED, color: QUALITY_COLORS.ANOMALY_CORRECTED },
    { name: "Anomaly Flagged", value: transformStatus.ANOMALY_FLAGGED, color: QUALITY_COLORS.ANOMALY_FLAGGED }
  ] : []

  const totalQualityRecords = transformStatus 
    ? transformStatus.CLEAN + transformStatus.FORWARD_FILLED + transformStatus.ANOMALY_CORRECTED + transformStatus.ANOMALY_FLAGGED
    : 0

  if (loading) {
    return (
      <div className="flex items-center justify-center h-[60vh]">
        <Spinner className="h-8 w-8" />
      </div>
    )
  }

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-foreground">Dashboard</h1>
          <p className="text-muted-foreground text-sm">
            Financial algorithms analysis overview
          </p>
        </div>
        <div className="flex items-center gap-3">
          <Button variant="outline" size="sm" onClick={fetchData}>
            <RefreshCw className="h-4 w-4 mr-2" />
            Refresh
          </Button>
          <Button 
            onClick={handleRunAnalysis} 
            disabled={runningAnalysis}
            className="bg-accent text-accent-foreground hover:bg-accent/90"
          >
            {runningAnalysis ? (
              <>
                <Spinner className="h-4 w-4 mr-2" />
                Running...
              </>
            ) : (
              <>
                <Cpu className="h-4 w-4 mr-2" />
                Run Full Analysis
              </>
            )}
          </Button>
        </div>
      </div>

      {error && (
        <div className="bg-destructive/10 border border-destructive/20 rounded-lg p-4 flex items-center gap-3">
          <AlertTriangle className="h-5 w-5 text-destructive" />
          <span className="text-destructive">{error}</span>
        </div>
      )}

      {/* Stats Cards */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
        <StatCard
          title="Total Assets"
          value={summary?.totalAssets ?? 0}
          icon={<Database className="h-5 w-5" />}
          subtitle="Tracked symbols"
        />
        <StatCard
          title="Similarity Pairs"
          value={summary?.totalSimilarityPairs ?? 0}
          icon={<BarChart3 className="h-5 w-5" />}
          subtitle="Pairs analyzed"
        />
        <StatCard
          title="Last Analysis"
          value={summary?.lastAnalysisAt 
            ? new Date(summary.lastAnalysisAt).toLocaleDateString()
            : "Never"
          }
          icon={<Clock className="h-5 w-5" />}
          subtitle={summary?.lastAnalysisAt 
            ? new Date(summary.lastAnalysisAt).toLocaleTimeString()
            : "Run analysis to start"
          }
        />
        <StatCard
          title="Data Records"
          value={totalQualityRecords.toLocaleString()}
          icon={<Activity className="h-5 w-5" />}
          subtitle="Cleaned price records"
        />
      </div>

      {/* Risk Highlights */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        <Card className="border-border/50 bg-card/50">
          <CardHeader className="pb-2">
            <CardTitle className="text-sm font-medium flex items-center gap-2">
              <TrendingUp className="h-4 w-4 text-destructive" />
              Highest Risk Asset
            </CardTitle>
          </CardHeader>
          <CardContent>
            {summary?.topRiskAsset ? (
              <div className="flex items-center justify-between">
                <div>
                  <span className="text-2xl font-bold font-mono">{summary.topRiskAsset.ticker}</span>
                  <Badge variant="destructive" className="ml-3">{summary.topRiskAsset.category}</Badge>
                </div>
                <div className="text-right">
                  <div className="text-xl font-mono text-destructive">
                    {(summary.topRiskAsset.annualizedVolatility * 100).toFixed(1)}%
                  </div>
                  <div className="text-xs text-muted-foreground">Annualized Volatility</div>
                </div>
              </div>
            ) : (
              <span className="text-muted-foreground">No data available</span>
            )}
          </CardContent>
        </Card>

        <Card className="border-border/50 bg-card/50">
          <CardHeader className="pb-2">
            <CardTitle className="text-sm font-medium flex items-center gap-2">
              <TrendingDown className="h-4 w-4 text-green-500" />
              Lowest Risk Asset
            </CardTitle>
          </CardHeader>
          <CardContent>
            {summary?.lowestRiskAsset ? (
              <div className="flex items-center justify-between">
                <div>
                  <span className="text-2xl font-bold font-mono">{summary.lowestRiskAsset.ticker}</span>
                  <Badge className="ml-3 bg-green-500/20 text-green-400 border-green-500/30">
                    {summary.lowestRiskAsset.category}
                  </Badge>
                </div>
                <div className="text-right">
                  <div className="text-xl font-mono text-green-500">
                    {(summary.lowestRiskAsset.annualizedVolatility * 100).toFixed(1)}%
                  </div>
                  <div className="text-xs text-muted-foreground">Annualized Volatility</div>
                </div>
              </div>
            ) : (
              <span className="text-muted-foreground">No data available</span>
            )}
          </CardContent>
        </Card>
      </div>

      {/* Charts Row */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Risk Distribution */}
        <Card className="border-border/50 bg-card/50">
          <CardHeader>
            <CardTitle className="text-sm font-medium">Risk Distribution</CardTitle>
          </CardHeader>
          <CardContent>
            {riskChartData.length > 0 ? (
              <ResponsiveContainer width="100%" height={250}>
                <PieChart>
                  <Pie
                    data={riskChartData}
                    cx="50%"
                    cy="50%"
                    innerRadius={60}
                    outerRadius={90}
                    paddingAngle={2}
                    dataKey="value"
                  >
                    {riskChartData.map((entry, index) => (
                      <Cell key={`cell-${index}`} fill={entry.color} />
                    ))}
                  </Pie>
                  <Tooltip
                    contentStyle={{
                      backgroundColor: '#0D1117',
                      border: '1px solid #1E2433',
                      borderRadius: '8px',
                      color: '#ffffff',
                    }}
                  />
                  <Legend wrapperStyle={{ color: '#ffffff' }} />
                </PieChart>
              </ResponsiveContainer>
            ) : (
              <div className="h-[250px] flex items-center justify-center text-muted-foreground">
                No data available
              </div>
            )}
          </CardContent>
        </Card>

        {/* Data Quality */}
        <Card className="border-border/50 bg-card/50">
          <CardHeader>
            <CardTitle className="text-sm font-medium">Data Quality Status</CardTitle>
          </CardHeader>
          <CardContent>
            {qualityChartData.length > 0 ? (
              <ResponsiveContainer width="100%" height={250}>
                <PieChart>
                  <Pie
                    data={qualityChartData}
                    cx="50%"
                    cy="50%"
                    innerRadius={60}
                    outerRadius={90}
                    paddingAngle={2}
                    dataKey="value"
                  >
                    {qualityChartData.map((entry, index) => (
                      <Cell key={`cell-${index}`} fill={entry.color} />
                    ))}
                  </Pie>
                  <Tooltip
                    contentStyle={{
                      backgroundColor: '#0D1117',
                      border: '1px solid #1E2433',
                      borderRadius: '8px',
                      color: '#ffffff',
                    }}
                  />
                  <Legend wrapperStyle={{ color: '#ffffff' }} />
                </PieChart>
              </ResponsiveContainer>
            ) : (
              <div className="h-[250px] flex items-center justify-center text-muted-foreground">
                No data available
              </div>
            )}
          </CardContent>
        </Card>
      </div>

      {/* Batch Status */}
      <Card className="border-border/50 bg-card/50">
        <CardHeader>
          <CardTitle className="text-sm font-medium">Batch Download Status</CardTitle>
        </CardHeader>
        <CardContent>
          {batchStatus ? (
            <div className="space-y-4">
              <div className="flex items-center justify-between">
                <div className="flex items-center gap-3">
                  {batchStatus.status === 'COMPLETED' && (
                    <CheckCircle2 className="h-5 w-5 text-green-500" />
                  )}
                  {batchStatus.status === 'IN_PROGRESS' && (
                    <Spinner className="h-5 w-5" />
                  )}
                  {batchStatus.status === 'FAILED' && (
                    <XCircle className="h-5 w-5 text-destructive" />
                  )}
                  {batchStatus.status === 'COMPLETED_WITH_ERRORS' && (
                    <AlertTriangle className="h-5 w-5 text-yellow-500" />
                  )}
                  <StatusBadge status={batchStatus.status} />
                </div>
                <span className="text-sm text-muted-foreground">
                  {batchStatus.downloadDate}
                </span>
              </div>
              
              {/* Progress Bar */}
              <div className="space-y-2">
                <div className="flex justify-between text-sm">
                  <span className="text-muted-foreground">Progress</span>
                  <span className="font-mono">
                    {batchStatus.downloadedSymbols}/{batchStatus.totalSymbols}
                  </span>
                </div>
                <div className="h-2 bg-muted rounded-full overflow-hidden">
                  <div 
                    className="h-full bg-accent transition-all duration-300"
                    style={{ 
                      width: `${(batchStatus.downloadedSymbols / batchStatus.totalSymbols) * 100}%` 
                    }}
                  />
                </div>
              </div>

              {batchStatus.failedSymbols > 0 && (
                <div className="text-sm text-destructive">
                  {batchStatus.failedSymbols} symbols failed to download
                </div>
              )}

              {batchStatus.errorMessage && (
                <div className="text-sm text-destructive bg-destructive/10 p-2 rounded">
                  {batchStatus.errorMessage}
                </div>
              )}
            </div>
          ) : (
            <div className="text-muted-foreground text-center py-8">
              No batch download has been triggered yet.
            </div>
          )}
        </CardContent>
      </Card>
    </div>
  )
}
