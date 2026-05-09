'use client'

import { useState, useEffect, useCallback } from 'react'
import { ChevronDown, ChevronUp, TrendingUp, TrendingDown } from 'lucide-react'
import {
  ComposedChart,
  Bar,
  Line,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
  Cell,
  Brush,
} from 'recharts'
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select'
import {
  Panel,
  ProgressBar,
  Skeleton,
  ErrorState,
  EmptyState,
} from '@/components/dashboard/shared'
import { api } from '@/lib/api/client'
import type { PatternRecord, Asset } from '@/lib/api/types'

interface ChartPrice {
  date: string
  open: number
  high: number
  low: number
  close: number
}

export default function PatternsPage() {
  const [symbols, setSymbols] = useState<string[]>([])
  const [selectedSymbol, setSelectedSymbol] = useState<string>('')
  const [patterns, setPatterns] = useState<PatternRecord[]>([])
  const [smaData20, setSmaData20] = useState<number[]>([])
  const [smaData50, setSmaData50] = useState<number[]>([])
  const [smaData100, setSmaData100] = useState<number[]>([])
  const [priceData, setPriceData] = useState<ChartPrice[]>([])
  const [loading, setLoading] = useState(true)
  const [fetching, setFetching] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [expandedPatterns, setExpandedPatterns] = useState<Set<number>>(new Set())

  const fetchSymbols = useCallback(async () => {
    try {
      const assets = await api.getAssets()
      const uniqueSymbols = assets.map((a: Asset) => a.symbol)
      setSymbols(uniqueSymbols)
      if (uniqueSymbols.length > 0) {
        setSelectedSymbol(uniqueSymbols[0])
      }
      setError(null)
    } catch {
      setError('Failed to load symbols. Is the backend running?')
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    fetchSymbols()
  }, [fetchSymbols])

  const fetchPatternData = useCallback(async () => {
    if (!selectedSymbol) return

    setFetching(true)

    // Patterns — show empty if not yet computed (404)
    try {
      const patternsRes = await api.getPatternsBySymbol(selectedSymbol)
      setPatterns(patternsRes)
    } catch {
      setPatterns([])
    }

    const parseSmaValues = (json: string): number[] =>
      JSON.parse((json || '[]').replace(/\bNaN\b/g, 'null'))

    try {
      const res = await api.getSMA(selectedSymbol, 20)
      setSmaData20(parseSmaValues(res.valuesJson))
    } catch { setSmaData20([]) }

    try {
      const res = await api.getSMA(selectedSymbol, 50)
      setSmaData50(parseSmaValues(res.valuesJson))
    } catch { setSmaData50([]) }

    try {
      const res = await api.getSMA(selectedSymbol, 100)
      setSmaData100(parseSmaValues(res.valuesJson))
    } catch { setSmaData100([]) }

    // Prices — try cleaned first, fall back to raw price history
    try {
      const cleanedRes = await api.getCleanedPrices(selectedSymbol, 0, 1300)
      if (cleanedRes.content.length > 0) {
        setPriceData(
          cleanedRes.content
            .map((r) => ({ date: r.date, open: r.open, high: r.high, low: r.low, close: r.close }))
            .reverse()
        )
      } else {
        const rawRes = await api.getPriceHistory(selectedSymbol, 0, 1300)
        setPriceData(
          rawRes.content
            .map((r) => ({ date: r.datetime, open: r.open, high: r.high, low: r.low, close: r.close }))
            .reverse()
        )
      }
    } catch {
      try {
        const rawRes = await api.getPriceHistory(selectedSymbol, 0, 1300)
        setPriceData(
          rawRes.content
            .map((r) => ({ date: r.datetime, open: r.open, high: r.high, low: r.low, close: r.close }))
            .reverse()
        )
      } catch {
        setPriceData([])
      }
    }

    setFetching(false)
  }, [selectedSymbol])

  useEffect(() => {
    if (selectedSymbol) {
      fetchPatternData()
    }
  }, [selectedSymbol, fetchPatternData])

  const toggleExpanded = (id: number) => {
    const newExpanded = new Set(expandedPatterns)
    if (newExpanded.has(id)) {
      newExpanded.delete(id)
    } else {
      newExpanded.add(id)
    }
    setExpandedPatterns(newExpanded)
  }

  if (loading) {
    return (
      <div className="space-y-6">
        <Skeleton className="h-16" />
        <div className="grid gap-6 lg:grid-cols-3">
          <Skeleton className="col-span-2 h-96" />
          <Skeleton className="h-96" />
        </div>
      </div>
    )
  }

  if (error) {
    return <ErrorState message={error} onRetry={fetchSymbols} />
  }

  const alignedSma20 = smaData20.slice(-priceData.length)
  const alignedSma50 = smaData50.slice(-priceData.length)
  const alignedSma100 = smaData100.slice(-priceData.length)

  const chartData = priceData.map((price, index) => ({
    ...price,
    sma20: alignedSma20[index] ?? null,
    sma50: alignedSma50[index] ?? null,
    sma100: alignedSma100[index] ?? null,
    bullish: price.close > price.open,
  }))

  return (
    <div className="space-y-6">
      {/* Asset Selector */}
      <Panel title="Pattern Analysis">
        <div className="flex flex-wrap items-center gap-4">
          <Select value={selectedSymbol} onValueChange={setSelectedSymbol}>
            <SelectTrigger className="w-40 font-mono">
              <SelectValue placeholder="Select asset" />
            </SelectTrigger>
            <SelectContent>
              {symbols.map((symbol) => (
                <SelectItem key={symbol} value={symbol} className="font-mono">
                  {symbol}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>

          <span className="font-mono text-xs text-muted-foreground">SMA 20 / 50 / 100</span>

          {fetching && (
            <span className="font-mono text-xs text-muted-foreground animate-pulse">
              Loading...
            </span>
          )}
        </div>
      </Panel>

      <div className="grid gap-6 lg:grid-cols-3">
        {/* Candlestick Chart */}
        <Panel title={`${selectedSymbol} Price Chart`} className="lg:col-span-2">
          {chartData.length > 0 ? (
            <div className="h-80">
              <ResponsiveContainer width="100%" height="100%">
                <ComposedChart data={chartData}>
                  <CartesianGrid
                    strokeDasharray="3 3"
                    stroke="rgba(0, 212, 255, 0.1)"
                  />
                  <XAxis
                    dataKey="date"
                    stroke="#ffffff"
                    tick={{ fill: '#ffffff', fontSize: 10 }}
                    tickFormatter={(value) =>
                      new Date(value).toLocaleDateString('en-US', {
                        month: 'short',
                        day: 'numeric',
                      })
                    }
                  />
                  <YAxis stroke="#ffffff" tick={{ fill: '#ffffff', fontSize: 10 }} domain={['auto', 'auto']} />
                  <Tooltip
                    contentStyle={{
                      backgroundColor: '#0D1117',
                      border: '1px solid #1E2433',
                      borderRadius: '8px',
                      fontFamily: 'JetBrains Mono, monospace',
                      fontSize: '12px',
                      color: '#ffffff',
                    }}
                    labelStyle={{ color: '#ffffff' }}
                    itemStyle={{ color: '#ffffff' }}
                    labelFormatter={(label) =>
                      new Date(label).toLocaleDateString('en-US', {
                        year: 'numeric',
                        month: 'long',
                        day: 'numeric',
                      })
                    }
                    formatter={(value: number, name: string) => [
                      `$${value.toFixed(2)}`,
                      name.toUpperCase(),
                    ]}
                  />
                  {/* Candlestick bars */}
                  <Bar dataKey="high" fill="transparent" legendType="none" />
                  <Bar dataKey="open" barSize={4} name="OPEN">
                    {chartData.map((entry, index) => (
                      <Cell
                        key={`open-${index}`}
                        fill={entry.bullish ? 'rgba(0,255,148,0.4)' : 'rgba(255,59,92,0.4)'}
                      />
                    ))}
                  </Bar>
                  <Bar dataKey="close" barSize={4} name="CLOSE">
                    {chartData.map((entry, index) => (
                      <Cell
                        key={`close-${index}`}
                        fill={entry.bullish ? '#00FF94' : '#FF3B5C'}
                      />
                    ))}
                  </Bar>
                  <Line type="monotone" dataKey="sma20"
                    stroke="#F59E0B" strokeWidth={2} dot={false} name="SMA(20)" />
                  <Line type="monotone" dataKey="sma50"
                    stroke="#10B981" strokeWidth={2} dot={false} name="SMA(50)" />
                  <Line type="monotone" dataKey="sma100"
                    stroke="#EF4444" strokeWidth={2} dot={false} name="SMA(100)" />
                  <Brush
                    dataKey="date"
                    height={25}
                    stroke="#00D4FF"
                    fill="#0D1117"
                    travellerWidth={8}
                    tickFormatter={(value) =>
                      new Date(value).toLocaleDateString('en-US', { month: 'short', year: '2-digit' })
                    }
                  />
                </ComposedChart>
              </ResponsiveContainer>
            </div>
          ) : (
            <EmptyState
              title="No price data"
              description="Select an asset to view its price chart."
            />
          )}
        </Panel>

        {/* Pattern Results */}
        <Panel title="Pattern Results">
          {patterns.length > 0 ? (
            <div className="space-y-4">
              {patterns.map((pattern) => {
                const isExpanded = expandedPatterns.has(pattern.id)
                const indices: number[] = JSON.parse(pattern.indicesJson || '[]')

                return (
                  <div
                    key={pattern.id}
                    className="rounded-lg border border-border bg-secondary/30 p-4"
                  >
                    <div className="flex items-start justify-between">
                      <div>
                        <span className="inline-flex items-center rounded bg-primary/20 px-2 py-0.5 font-mono text-xs text-primary">
                          {pattern.patternType}
                        </span>
                        <p className="mt-2 font-mono text-2xl font-bold text-foreground">
                          {pattern.occurrences}
                        </p>
                        <p className="font-mono text-xs text-muted-foreground">
                          occurrences detected
                        </p>
                      </div>
                    </div>

                    <div className="mt-4">
                      <ProgressBar
                        value={pattern.relativeFrequency * 100}
                        max={100}
                        color="cyan"
                        showLabel={false}
                      />
                      <p className="mt-1 text-right font-mono text-xs text-muted-foreground">
                        {(pattern.relativeFrequency * 100).toFixed(1)}% relative frequency
                      </p>
                    </div>

                    {/* Expandable Indices */}
                    <button
                      onClick={() => toggleExpanded(pattern.id)}
                      className="mt-4 flex w-full items-center justify-between rounded border border-border bg-card px-3 py-2 text-left text-xs text-muted-foreground transition-colors hover:border-primary/50"
                    >
                      <span>View Indices ({indices.length})</span>
                      {isExpanded ? (
                        <ChevronUp className="h-4 w-4" />
                      ) : (
                        <ChevronDown className="h-4 w-4" />
                      )}
                    </button>

                    {isExpanded && indices.length > 0 && (
                      <div className="mt-2 max-h-48 overflow-y-auto rounded border border-border bg-card p-2">
                        <div className="flex flex-wrap gap-1">
                          {indices.slice(0, 20).map((idx, i) => (
                            <span
                              key={i}
                              className="rounded bg-secondary/50 px-2 py-1 font-mono text-xs text-muted-foreground"
                            >
                              #{idx}
                            </span>
                          ))}
                          {indices.length > 20 && (
                            <span className="rounded bg-muted px-2 py-1 font-mono text-xs text-muted-foreground">
                              +{indices.length - 20} more
                            </span>
                          )}
                        </div>
                      </div>
                    )}
                  </div>
                )
              })}
            </div>
          ) : (
            <EmptyState
              title="No patterns detected"
              description="Run the analysis pipeline to detect patterns."
            />
          )}
        </Panel>
      </div>
    </div>
  )
}
