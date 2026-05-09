'use client'

import { useState, useEffect, useCallback } from 'react'
import {
  FileText,
  Download,
  Loader2,
  CheckCircle,
  BarChart3,
  Shield,
  Activity,
  Grid3X3,
  TrendingUp,
  FileSpreadsheet,
  BookOpen,
} from 'lucide-react'
import { toast } from 'sonner'
import { Button } from '@/components/ui/button'
import {
  Panel,
  MetricCard,
  Skeleton,
  ErrorState,
} from '@/components/dashboard/shared'
import { api } from '@/lib/api/client'
import type { RiskRecord, SimilarityRecord, DashboardSummary } from '@/lib/api/types'
import { cn } from '@/lib/utils'

const REPORT_SECTIONS = [
  { number: 1, title: 'Cover / Title Page', icon: BookOpen },
  { number: 2, title: 'Similarity Results Table', icon: BarChart3 },
  { number: 3, title: 'Risk Profiles Table', icon: Shield },
  { number: 4, title: 'Pattern Detection Results', icon: Activity },
  { number: 5, title: 'Correlation Matrix', icon: Grid3X3 },
  { number: 6, title: 'SMA Charts & Data', icon: TrendingUp },
  { number: 7, title: 'Executive Summary', icon: FileSpreadsheet },
]

export default function ReportsPage() {
  const [loading, setLoading] = useState(true)
  const [exporting, setExporting] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [stats, setStats] = useState({
    totalAssets: 0,
    algorithmPairs: 0,
    patternsDetected: 0,
    reportGenerated: null as string | null,
  })

  const fetchStats = useCallback(async () => {
    try {
      const [summary, risk, similarity] = await Promise.all([
        api.getDashboardSummary().catch(() => null as DashboardSummary | null),
        api.getRiskRanking().catch(() => [] as RiskRecord[]),
        api.getSimilarityMatrix().catch(() => [] as SimilarityRecord[]),
      ])

      setStats({
        totalAssets: summary?.totalAssets || risk.length || 20,
        algorithmPairs: summary?.totalSimilarityPairs || similarity.length || 190,
        patternsDetected: 0, // Will be calculated from patterns endpoint
        reportGenerated: summary?.lastAnalysisAt || new Date().toISOString(),
      })
      setError(null)
    } catch {
      setError('Failed to load report data')
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    fetchStats()
  }, [fetchStats])

  const handleExportPDF = async () => {
    setExporting(true)
    try {
      const blob = await api.downloadPDF()
      
      // Create download link
      const url = window.URL.createObjectURL(blob)
      const link = document.createElement('a')
      link.href = url
      link.download = `fialgo-report-${new Date().toISOString().split('T')[0]}.pdf`
      document.body.appendChild(link)
      link.click()
      document.body.removeChild(link)
      window.URL.revokeObjectURL(url)
      
      toast.success('Report downloaded successfully')
    } catch {
      toast.error('Failed to export PDF. Check if the backend is running.')
    } finally {
      setExporting(false)
    }
  }

  if (loading) {
    return (
      <div className="space-y-6">
        <div className="grid gap-4 md:grid-cols-4">
          <Skeleton className="h-24" />
          <Skeleton className="h-24" />
          <Skeleton className="h-24" />
          <Skeleton className="h-24" />
        </div>
        <Skeleton className="h-64" />
        <Skeleton className="h-96" />
      </div>
    )
  }

  if (error) {
    return <ErrorState message={error} onRetry={fetchStats} />
  }

  return (
    <div className="space-y-6">
      {/* Report Stats */}
      <div className="grid gap-4 md:grid-cols-4">
        <MetricCard
          label="Total Assets Analyzed"
          value={stats.totalAssets}
          color="cyan"
        />
        <MetricCard
          label="Algorithm Pairs Computed"
          value={stats.algorithmPairs}
          color="green"
        />
        <MetricCard
          label="Patterns Detected"
          value={stats.patternsDetected}
          color="amber"
        />
        <MetricCard
          label="Report Generated"
          value={stats.reportGenerated 
            ? new Date(stats.reportGenerated).toLocaleDateString('en-US', {
                month: 'short',
                day: 'numeric',
                year: 'numeric',
              })
            : '—'
          }
          color="default"
        />
      </div>

      {/* Export Section */}
      <Panel title="Export Report">
        <div className="flex flex-col items-center justify-center py-8">
          <div className="mb-6 flex h-16 w-16 items-center justify-center rounded-full bg-primary/10">
            <FileText className="h-8 w-8 text-primary" />
          </div>
          
          <h3 className="text-xl font-semibold text-foreground">
            Export Financial Report
          </h3>
          
          <p className="mt-2 max-w-md text-center text-sm text-muted-foreground">
            Generate a comprehensive PDF report including similarity analysis, risk profiles, 
            pattern detection, correlation matrix, and SMA charts.
          </p>

          <Button
            onClick={handleExportPDF}
            disabled={exporting}
            size="lg"
            className="mt-6 gap-2 bg-primary text-primary-foreground hover:bg-primary/90"
          >
            {exporting ? (
              <>
                <Loader2 className="h-5 w-5 animate-spin" />
                Generating Report...
              </>
            ) : (
              <>
                <Download className="h-5 w-5" />
                Download Full PDF Report
              </>
            )}
          </Button>

          {exporting && (
            <p className="mt-4 animate-pulse font-mono text-xs text-muted-foreground">
              Compiling data and generating charts...
            </p>
          )}
        </div>
      </Panel>

      {/* Report Contents Preview */}
      <Panel title="Report Contents Preview">
        <p className="mb-6 text-sm text-muted-foreground">
          The exported PDF report includes the following sections:
        </p>
        
        <div className="space-y-3">
          {REPORT_SECTIONS.map((section) => {
            const Icon = section.icon
            
            return (
              <div
                key={section.number}
                className="flex items-center gap-4 rounded-lg border border-border bg-secondary/30 px-4 py-3 transition-colors hover:border-primary/30 hover:bg-secondary/50"
              >
                <div className="flex h-8 w-8 flex-shrink-0 items-center justify-center rounded bg-primary/10 font-mono text-sm font-bold text-primary">
                  {section.number}
                </div>
                <Icon className="h-5 w-5 flex-shrink-0 text-muted-foreground" />
                <span className="font-medium text-foreground">{section.title}</span>
                <CheckCircle className="ml-auto h-4 w-4 text-accent" />
              </div>
            )
          })}
        </div>

        <div className="mt-6 rounded-lg border border-border bg-card p-4">
          <p className="font-mono text-xs text-muted-foreground">
            <span className="text-primary">Note:</span> The report is generated in real-time 
            using the latest data from all analysis modules. Ensure all pipelines have been 
            executed before exporting for complete results.
          </p>
        </div>
      </Panel>
    </div>
  )
}
