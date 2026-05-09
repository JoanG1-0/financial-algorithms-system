'use client'

import { useState, useEffect, useCallback } from 'react'
import { GitCompare, ArrowRight, AlertTriangle, TrendingUp, Circle } from 'lucide-react'
import {
  LineChart,
  Line,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
  Legend,
  Brush,
} from 'recharts'
import { Button } from '@/components/ui/button'
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select'
import {
  Panel,
  ComplexityBadge,
  Skeleton,
  ErrorState,
  EmptyState,
} from '@/components/dashboard/shared'
import { api } from '@/lib/api/client'
import type { SimilarityRecord, Asset } from '@/lib/api/types'
import { cn } from '@/lib/utils'

export default function SimilarityPage() {
  const [symbols, setSymbols] = useState<string[]>([])
  const [tickerA, setTickerA] = useState<string>('')
  const [tickerB, setTickerB] = useState<string>('')
  const [similarity, setSimilarity] = useState<SimilarityRecord | null>(null)
  const [priceData, setPriceData] = useState<{ date: string; [key: string]: number | string }[]>([])
  const [loading, setLoading] = useState(true)
  const [comparing, setComparing] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const fetchSymbols = useCallback(async () => {
    try {
      const assets = await api.getAssets()
      const uniqueSymbols = assets.map((a: Asset) => a.symbol)
      setSymbols(uniqueSymbols)
      if (uniqueSymbols.length >= 2) {
        setTickerA(uniqueSymbols[0])
        setTickerB(uniqueSymbols[1])
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

  const handleCompare = async () => {
    if (!tickerA || !tickerB || tickerA === tickerB) return
    
    setComparing(true)
    try {
      const [similarityData, pricesARes, pricesBRes] = await Promise.all([
        api.getSimilarityMatrix(),
        api.getPriceHistory(tickerA, 0, 1300),
        api.getPriceHistory(tickerB, 0, 1300),
      ])

      // Find the similarity record for selected pair
      const record = similarityData.find(
        (s: SimilarityRecord) =>
          (s.tickerA === tickerA && s.tickerB === tickerB) ||
          (s.tickerA === tickerB && s.tickerB === tickerA)
      )
      setSimilarity(record || null)

      // Process price data for chart
      const pricesA = pricesARes.content.slice().reverse()
      const pricesB = pricesBRes.content.slice().reverse()

      const chartData = pricesA.map((pa, idx) => {
        const pb = pricesB[idx]
        return {
          date: pa.datetime,
          [tickerA]: pa.close,
          [tickerB]: pb?.close || 0,
        }
      })

      setPriceData(chartData)
    } catch {
      setError('Failed to compare assets')
    } finally {
      setComparing(false)
    }
  }

  if (loading) {
    return (
      <div className="space-y-6">
        <Skeleton className="h-20" />
        <div className="grid gap-6 lg:grid-cols-2">
          <Skeleton className="h-64" />
          <Skeleton className="h-64" />
        </div>
        <Skeleton className="h-96" />
      </div>
    )
  }

  if (error) {
    return <ErrorState message={error} onRetry={fetchSymbols} />
  }

  const getColorForValue = (
    value: number,
    type: 'euclidean' | 'pearson' | 'dtw' | 'cosine'
  ) => {
    if (type === 'euclidean' || type === 'dtw') {
      // Lower = more similar = green
      if (value < 50) return 'text-accent'
      if (value < 100) return 'text-warning'
      return 'text-destructive'
    }
    if (type === 'pearson') {
      if (value > 0.7) return 'text-accent'
      if (value > 0.3) return 'text-warning'
      return 'text-destructive'
    }
    // cosine
    if (value > 0.8) return 'text-accent'
    if (value > 0.5) return 'text-warning'
    return 'text-destructive'
  }

  return (
    <div className="space-y-6">
      {/* Asset Selectors */}
      <Panel title="Asset Comparison">
        <div className="flex flex-wrap items-center gap-4">
          <Select value={tickerA} onValueChange={setTickerA}>
            <SelectTrigger className="w-40 font-mono">
              <SelectValue placeholder="Select asset A" />
            </SelectTrigger>
            <SelectContent>
              {symbols.map((symbol) => (
                <SelectItem
                  key={symbol}
                  value={symbol}
                  disabled={symbol === tickerB}
                  className="font-mono"
                >
                  {symbol}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>

          <ArrowRight className="h-5 w-5 text-muted-foreground" />

          <Select value={tickerB} onValueChange={setTickerB}>
            <SelectTrigger className="w-40 font-mono">
              <SelectValue placeholder="Select asset B" />
            </SelectTrigger>
            <SelectContent>
              {symbols.map((symbol) => (
                <SelectItem
                  key={symbol}
                  value={symbol}
                  disabled={symbol === tickerA}
                  className="font-mono"
                >
                  {symbol}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>

          <Button
            onClick={handleCompare}
            disabled={comparing || !tickerA || !tickerB || tickerA === tickerB}
            className="gap-2"
          >
            <GitCompare className="h-4 w-4" />
            {comparing ? 'Comparing...' : 'Compare Assets'}
          </Button>
        </div>
      </Panel>

      {/* Similarity Results */}
      {similarity ? (
        <>
          <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
            {/* Euclidean Distance */}
            <div className="rounded-lg border border-border bg-card p-4 glow-border">
              <div className="flex items-start justify-between">
                <div>
                  <p className="font-mono text-xs uppercase tracking-wider text-muted-foreground">
                    Euclidean Distance
                  </p>
                  <p
                    className={cn(
                      'mt-2 font-mono text-3xl font-bold',
                      getColorForValue(similarity.euclidean, 'euclidean')
                    )}
                  >
                    {similarity.euclidean.toFixed(2)}
                  </p>
                </div>
                <ComplexityBadge complexity="O(n)" />
              </div>
              <p className="mt-3 text-xs text-muted-foreground">
                Measures absolute price difference point-to-point
              </p>
            </div>

            {/* Pearson Correlation */}
            <div className="rounded-lg border border-border bg-card p-4 glow-border">
              <div className="flex items-start justify-between">
                <div>
                  <p className="font-mono text-xs uppercase tracking-wider text-muted-foreground">
                    Pearson Correlation
                  </p>
                  <p
                    className={cn(
                      'mt-2 font-mono text-3xl font-bold',
                      getColorForValue(similarity.pearson, 'pearson')
                    )}
                  >
                    {(similarity.pearson * 100).toFixed(1)}%
                  </p>
                </div>
                <ComplexityBadge complexity="O(n)" />
              </div>
              <div className="mt-3 flex items-center gap-2">
                <div className="h-1.5 flex-1 overflow-hidden rounded-full bg-secondary">
                  <div
                    className={cn(
                      'h-full transition-all',
                      similarity.pearson > 0 ? 'bg-accent' : 'bg-destructive'
                    )}
                    style={{
                      width: `${Math.abs(similarity.pearson) * 50 + 50}%`,
                      marginLeft: similarity.pearson < 0 ? `${50 + similarity.pearson * 50}%` : '50%',
                    }}
                  />
                </div>
              </div>
            </div>

            {/* DTW */}
            <div className="rounded-lg border border-border bg-card p-4 glow-border">
              <div className="flex items-start justify-between">
                <div>
                  <p className="font-mono text-xs uppercase tracking-wider text-muted-foreground">
                    Dynamic Time Warping
                  </p>
                  <p
                    className={cn(
                      'mt-2 font-mono text-3xl font-bold',
                      getColorForValue(similarity.dtw, 'dtw')
                    )}
                  >
                    {similarity.dtw.toFixed(2)}
                  </p>
                </div>
                <ComplexityBadge complexity="O(n²)" warning />
              </div>
              <p className="mt-3 text-xs text-muted-foreground">
                Detects similarity with temporal displacement
              </p>
              <div className="mt-2 flex items-center gap-1.5 text-warning">
                <AlertTriangle className="h-3 w-3" />
                <span className="text-[10px]">High computational cost</span>
              </div>
            </div>

            {/* Cosine Similarity */}
            <div className="rounded-lg border border-border bg-card p-4 glow-border">
              <div className="flex items-start justify-between">
                <div>
                  <p className="font-mono text-xs uppercase tracking-wider text-muted-foreground">
                    Cosine Similarity
                  </p>
                  <p
                    className={cn(
                      'mt-2 font-mono text-3xl font-bold',
                      getColorForValue(similarity.cosine, 'cosine')
                    )}
                  >
                    {similarity.cosine.toFixed(4)}
                  </p>
                </div>
                <ComplexityBadge complexity="O(n)" />
              </div>
              <p className="mt-3 text-xs text-muted-foreground">
                Compares movement direction, ignoring magnitude
              </p>
              <div className="mt-2 flex items-center gap-2">
                <TrendingUp
                  className={cn(
                    'h-4 w-4 transition-transform',
                    similarity.cosine > 0.5 ? 'text-accent' : 'text-muted-foreground rotate-45'
                  )}
                />
              </div>
            </div>
          </div>

          {/* Price Chart */}
          {priceData.length > 0 && (
            <Panel title="Price History Comparison">
              <div className="h-80">
                <ResponsiveContainer width="100%" height="100%">
                  <LineChart data={priceData}>
                    <CartesianGrid
                      strokeDasharray="3 3"
                      stroke="rgba(0, 212, 255, 0.1)"
                    />
                    <XAxis
                      dataKey="date"
                      tick={{ fill: '#ffffff', fontSize: 10 }}
                      tickFormatter={(value) =>
                        new Date(value).toLocaleDateString('en-US', {
                          month: 'short',
                          day: 'numeric',
                        })
                      }
                    />
                    <YAxis tick={{ fill: '#ffffff', fontSize: 10 }} />
                    <Tooltip
                      contentStyle={{
                        backgroundColor: '#0D1117',
                        border: '1px solid #1E2433',
                        borderRadius: '8px',
                        fontFamily: 'JetBrains Mono, monospace',
                        fontSize: '12px',
                        color: '#ffffff',
                      }}
                      labelFormatter={(label) =>
                        new Date(label).toLocaleDateString('en-US', {
                          year: 'numeric',
                          month: 'long',
                          day: 'numeric',
                        })
                      }
                    />
                    <Legend
                      formatter={(value) => (
                        <span className="font-mono text-xs">{value}</span>
                      )}
                    />
                    <Line
                      type="monotone"
                      dataKey={tickerA}
                      stroke="#00D4FF"
                      strokeWidth={2}
                      dot={false}
                    />
                    <Line
                      type="monotone"
                      dataKey={tickerB}
                      stroke="#00FF94"
                      strokeWidth={2}
                      dot={false}
                    />
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
                  </LineChart>
                </ResponsiveContainer>
              </div>
              <div className="mt-4 flex items-center justify-center gap-6">
                <div className="flex items-center gap-2">
                  <Circle className="h-3 w-3 fill-primary text-primary" />
                  <span className="font-mono text-xs text-muted-foreground">{tickerA}</span>
                </div>
                <div className="flex items-center gap-2">
                  <Circle className="h-3 w-3 fill-accent text-accent" />
                  <span className="font-mono text-xs text-muted-foreground">{tickerB}</span>
                </div>
              </div>
            </Panel>
          )}
        </>
      ) : (
        <EmptyState
          title="No comparison data"
          description="Select two assets and click Compare to see similarity metrics."
          action={{
            label: 'Compare Assets',
            onClick: handleCompare,
          }}
        />
      )}
    </div>
  )
}
