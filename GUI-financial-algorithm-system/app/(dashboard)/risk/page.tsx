'use client'

import { useState, useEffect, useCallback, useMemo } from 'react'
import {
  Shield,
  ShieldAlert,
  ShieldCheck,
  ArrowUpDown,
  ArrowUp,
  ArrowDown,
  Search,
} from 'lucide-react'
import {
  BarChart,
  Bar,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
  Cell,
} from 'recharts'
import { Input } from '@/components/ui/input'
import {
  Panel,
  MetricCard,
  RiskBadge,
  Skeleton,
  ErrorState,
  EmptyState,
} from '@/components/dashboard/shared'
import { api } from '@/lib/api/client'
import type { RiskRecord } from '@/lib/api/types'
import { cn } from '@/lib/utils'

type SortField = 'ticker' | 'annualizedVolatility' | 'category'
type SortDirection = 'asc' | 'desc'

export default function RiskPage() {
  const [riskData, setRiskData] = useState<RiskRecord[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [sortField, setSortField] = useState<SortField>('annualizedVolatility')
  const [sortDirection, setSortDirection] = useState<SortDirection>('desc')
  const [searchQuery, setSearchQuery] = useState('')

  const fetchRiskData = useCallback(async () => {
    try {
      const data = await api.getRiskRanking()
      setRiskData(data)
      setError(null)
    } catch {
      setError('Failed to load risk data. Is the backend running?')
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    fetchRiskData()
  }, [fetchRiskData])

  const handleSort = (field: SortField) => {
    if (sortField === field) {
      setSortDirection(sortDirection === 'asc' ? 'desc' : 'asc')
    } else {
      setSortField(field)
      setSortDirection('desc')
    }
  }

  const filteredAndSortedData = useMemo(() => {
    let filtered = riskData.filter((r) =>
      r.ticker.toLowerCase().includes(searchQuery.toLowerCase())
    )

    return filtered.sort((a, b) => {
      let comparison = 0
      switch (sortField) {
        case 'ticker':
          comparison = a.ticker.localeCompare(b.ticker)
          break
        case 'annualizedVolatility':
          comparison = a.annualizedVolatility - b.annualizedVolatility
          break
        case 'category':
          const categoryOrder = { CONSERVATIVE: 0, MODERATE: 1, AGGRESSIVE: 2 }
          comparison = categoryOrder[a.category] - categoryOrder[b.category]
          break
      }
      return sortDirection === 'asc' ? comparison : -comparison
    })
  }, [riskData, sortField, sortDirection, searchQuery])

  const stats = useMemo(() => {
    return {
      conservative: riskData.filter((r) => r.category === 'CONSERVATIVE').length,
      moderate: riskData.filter((r) => r.category === 'MODERATE').length,
      aggressive: riskData.filter((r) => r.category === 'AGGRESSIVE').length,
    }
  }, [riskData])

  const chartData = useMemo(() => {
    return [...riskData]
      .sort((a, b) => b.annualizedVolatility - a.annualizedVolatility)
      .map((r) => ({
        ticker: r.ticker,
        volatility: r.annualizedVolatility * 100,
        category: r.category,
      }))
  }, [riskData])

  const getVolatilityColor = (volatility: number) => {
    const pct = volatility * 100
    if (pct < 15) return '#00FF94' // green
    if (pct < 30) return '#FFB800' // amber
    return '#FF3B5C' // red
  }

  const SortIcon = ({ field }: { field: SortField }) => {
    if (sortField !== field) {
      return <ArrowUpDown className="h-4 w-4 text-muted-foreground" />
    }
    return sortDirection === 'asc' ? (
      <ArrowUp className="h-4 w-4 text-primary" />
    ) : (
      <ArrowDown className="h-4 w-4 text-primary" />
    )
  }

  if (loading) {
    return (
      <div className="space-y-6">
        <div className="grid gap-4 md:grid-cols-3">
          <Skeleton className="h-24" />
          <Skeleton className="h-24" />
          <Skeleton className="h-24" />
        </div>
        <Skeleton className="h-96" />
        <Skeleton className="h-64" />
      </div>
    )
  }

  if (error) {
    return <ErrorState message={error} onRetry={fetchRiskData} />
  }

  if (riskData.length === 0) {
    return (
      <EmptyState
        title="No risk data available"
        description="Run the analysis pipeline to generate risk classifications."
      />
    )
  }

  return (
    <div className="space-y-6">
      {/* Summary Stats */}
      <div className="grid gap-4 md:grid-cols-3">
        <MetricCard
          label="Conservative"
          value={stats.conservative}
          color="green"
          icon={<ShieldCheck className="h-5 w-5" />}
        />
        <MetricCard
          label="Moderate"
          value={stats.moderate}
          color="amber"
          icon={<Shield className="h-5 w-5" />}
        />
        <MetricCard
          label="Aggressive"
          value={stats.aggressive}
          color="red"
          icon={<ShieldAlert className="h-5 w-5" />}
        />
      </div>

      {/* Risk Table */}
      <Panel
        title="Risk Classification Table"
        headerAction={
          <div className="relative">
            <Search className="absolute left-2.5 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
            <Input
              placeholder="Search ticker..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="h-8 w-40 pl-8 font-mono text-xs"
            />
          </div>
        }
      >
        <div className="overflow-x-auto">
          <table className="w-full">
            <thead>
              <tr className="border-b border-border">
                <th className="px-4 py-3 text-left font-mono text-xs font-medium uppercase tracking-wider text-muted-foreground">
                  Rank
                </th>
                <th
                  onClick={() => handleSort('ticker')}
                  className="cursor-pointer px-4 py-3 text-left font-mono text-xs font-medium uppercase tracking-wider text-muted-foreground transition-colors hover:text-foreground"
                >
                  <div className="flex items-center gap-2">
                    Ticker
                    <SortIcon field="ticker" />
                  </div>
                </th>
                <th
                  onClick={() => handleSort('annualizedVolatility')}
                  className="cursor-pointer px-4 py-3 text-left font-mono text-xs font-medium uppercase tracking-wider text-muted-foreground transition-colors hover:text-foreground"
                >
                  <div className="flex items-center gap-2">
                    Annualized Volatility
                    <SortIcon field="annualizedVolatility" />
                  </div>
                </th>
                <th
                  onClick={() => handleSort('category')}
                  className="cursor-pointer px-4 py-3 text-left font-mono text-xs font-medium uppercase tracking-wider text-muted-foreground transition-colors hover:text-foreground"
                >
                  <div className="flex items-center gap-2">
                    Risk Category
                    <SortIcon field="category" />
                  </div>
                </th>
                <th className="px-4 py-3 text-left font-mono text-xs font-medium uppercase tracking-wider text-muted-foreground">
                  Volatility Bar
                </th>
              </tr>
            </thead>
            <tbody>
              {filteredAndSortedData.map((record, index) => {
                const volatilityPct = record.annualizedVolatility * 100

                return (
                  <tr
                    key={record.id}
                    className="border-b border-border/50 transition-colors hover:bg-secondary/30"
                  >
                    <td className="px-4 py-3 font-mono text-sm text-muted-foreground">
                      {index + 1}
                    </td>
                    <td className="px-4 py-3 font-mono text-sm font-medium text-foreground">
                      {record.ticker}
                    </td>
                    <td
                      className="px-4 py-3 font-mono text-sm"
                      style={{ color: getVolatilityColor(record.annualizedVolatility) }}
                    >
                      {volatilityPct.toFixed(2)}%
                    </td>
                    <td className="px-4 py-3">
                      <RiskBadge category={record.category} />
                    </td>
                    <td className="px-4 py-3">
                      <div className="w-32">
                        <div className="h-2 overflow-hidden rounded-full bg-secondary">
                          <div
                            className="h-full transition-all duration-500"
                            style={{
                              width: `${Math.min(volatilityPct, 100)}%`,
                              backgroundColor: getVolatilityColor(
                                record.annualizedVolatility
                              ),
                            }}
                          />
                        </div>
                      </div>
                    </td>
                  </tr>
                )
              })}
            </tbody>
          </table>
        </div>
      </Panel>

      {/* Volatility Bar Chart */}
      <Panel title="Volatility Distribution">
        <div className="h-64">
          <ResponsiveContainer width="100%" height="100%">
            <BarChart data={chartData} layout="horizontal">
              <CartesianGrid
                strokeDasharray="3 3"
                stroke="rgba(0, 212, 255, 0.1)"
              />
              <XAxis
                dataKey="ticker"
                tick={{ fill: '#ffffff', fontSize: 10 }}
                angle={-45}
                textAnchor="end"
                height={60}
              />
              <YAxis
                tick={{ fill: '#ffffff', fontSize: 10 }}
                tickFormatter={(value) => `${value}%`}
              />
              <Tooltip
                contentStyle={{
                  backgroundColor: '#0D1117',
                  border: '1px solid #1E2433',
                  borderRadius: '8px',
                  fontFamily: 'JetBrains Mono, monospace',
                  fontSize: '12px',
                  color: '#ffffff',
                }}
                formatter={(value: number) => [`${value.toFixed(2)}%`, 'Volatility']}
              />
              <Bar dataKey="volatility" radius={[4, 4, 0, 0]}>
                {chartData.map((entry, index) => (
                  <Cell
                    key={`cell-${index}`}
                    fill={
                      entry.category === 'CONSERVATIVE'
                        ? '#00FF94'
                        : entry.category === 'MODERATE'
                        ? '#FFB800'
                        : '#FF3B5C'
                    }
                  />
                ))}
              </Bar>
            </BarChart>
          </ResponsiveContainer>
        </div>
      </Panel>
    </div>
  )
}
