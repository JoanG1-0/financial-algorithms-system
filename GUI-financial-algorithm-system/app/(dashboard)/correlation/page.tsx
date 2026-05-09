"use client"

import { useState, useEffect, useCallback, useMemo } from "react"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Button } from "@/components/ui/button"
import { Badge } from "@/components/ui/badge"
import { Spinner } from "@/components/ui/spinner"
import { Switch } from "@/components/ui/switch"
import { Label } from "@/components/ui/label"
import { Grid3X3, RefreshCw, Info } from "lucide-react"
import { api } from "@/lib/api/client"
import type { CorrelationEntry } from "@/lib/api/types"
import { cn } from "@/lib/utils"

// Tracked symbols in order
const TICKERS = [
  "AAPL", "MSFT", "GOOGL", "AMZN", "META", 
  "TSLA", "NVDA", "JPM", "JNJ", "V",
  "NFLX", "AMD", "INTC", "DIS", "WMT", 
  "BAC", "KO", "PG", "IBM", "ORCL"
]

export default function CorrelationPage() {
  const [correlationData, setCorrelationData] = useState<CorrelationEntry[]>([])
  const [loading, setLoading] = useState(true)
  const [generating, setGenerating] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [hoveredCell, setHoveredCell] = useState<{ row: string; col: string } | null>(null)
  const [showClusters, setShowClusters] = useState(false)

  const fetchData = useCallback(async () => {
    try {
      setLoading(true)
      setError(null)
      const data = await api.getCorrelationMatrix()
      setCorrelationData(data)
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to fetch correlation data")
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    fetchData()
  }, [fetchData])

  const handleGenerate = async () => {
    try {
      setGenerating(true)
      await api.generateCorrelationMatrix()
      await fetchData()
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to generate correlation matrix")
    } finally {
      setGenerating(false)
    }
  }

  // Build correlation matrix
  const matrix = useMemo(() => {
    const m: Record<string, Record<string, number>> = {}
    
    // Initialize with zeros
    TICKERS.forEach(t1 => {
      m[t1] = {}
      TICKERS.forEach(t2 => {
        m[t1][t2] = t1 === t2 ? 1.0 : 0
      })
    })
    
    // Fill with actual data
    correlationData.forEach(entry => {
      if (m[entry.assetA] && m[entry.assetA][entry.assetB] !== undefined) {
        m[entry.assetA][entry.assetB] = entry.correlation
      }
    })
    
    return m
  }, [correlationData])

  // Get color for correlation value
  const getColor = (value: number) => {
    if (value >= 0.8) return "bg-red-500"
    if (value >= 0.6) return "bg-orange-500"
    if (value >= 0.4) return "bg-yellow-500"
    if (value >= 0.2) return "bg-green-400"
    if (value >= 0) return "bg-green-600"
    if (value >= -0.2) return "bg-cyan-400"
    if (value >= -0.4) return "bg-cyan-500"
    if (value >= -0.6) return "bg-blue-400"
    if (value >= -0.8) return "bg-blue-500"
    return "bg-blue-600"
  }

  const getOpacity = (value: number) => {
    return Math.abs(value) * 0.8 + 0.2
  }

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
          <h1 className="text-2xl font-bold text-foreground">Correlation Matrix</h1>
          <p className="text-muted-foreground text-sm">
            Pearson correlation between all asset pairs (20x20)
          </p>
        </div>
        <div className="flex items-center gap-3">
          <Button variant="outline" size="sm" onClick={fetchData}>
            <RefreshCw className="h-4 w-4 mr-2" />
            Refresh
          </Button>
          <Button 
            onClick={handleGenerate} 
            disabled={generating}
            className="bg-accent text-accent-foreground hover:bg-accent/90"
          >
            {generating ? (
              <>
                <Spinner className="h-4 w-4 mr-2" />
                Generating...
              </>
            ) : (
              <>
                <Grid3X3 className="h-4 w-4 mr-2" />
                Generate Matrix
              </>
            )}
          </Button>
        </div>
      </div>

      {error && (
        <div className="bg-destructive/10 border border-destructive/20 rounded-lg p-4">
          <span className="text-destructive">{error}</span>
        </div>
      )}

      {/* Controls */}
      <Card className="border-border/50 bg-card/50">
        <CardContent className="py-4">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-4">
              <div className="flex items-center gap-2">
                <Switch 
                  id="cluster-view" 
                  checked={showClusters}
                  onCheckedChange={setShowClusters}
                />
                <Label htmlFor="cluster-view" className="text-sm">Cluster View</Label>
              </div>
            </div>
            
            {/* Color Legend */}
            <div className="flex items-center gap-2">
              <span className="text-xs text-muted-foreground">-1.0</span>
              <div className="flex h-4 w-32 overflow-hidden rounded">
                <div className="flex-1 bg-blue-600" />
                <div className="flex-1 bg-blue-400" />
                <div className="flex-1 bg-cyan-400" />
                <div className="flex-1 bg-green-400" />
                <div className="flex-1 bg-yellow-400" />
                <div className="flex-1 bg-orange-400" />
                <div className="flex-1 bg-red-500" />
              </div>
              <span className="text-xs text-muted-foreground">+1.0</span>
            </div>
          </div>
        </CardContent>
      </Card>

      {/* Heatmap */}
      <Card className="border-border/50 bg-card/50">
        <CardHeader className="pb-2">
          <CardTitle className="text-sm font-medium flex items-center gap-2">
            <Grid3X3 className="h-4 w-4" />
            Correlation Heatmap
            <Badge variant="outline" className="ml-2">{correlationData.length} entries</Badge>
          </CardTitle>
        </CardHeader>
        <CardContent>
          {correlationData.length > 0 ? (
            <div className="overflow-x-auto">
              <div className="inline-block min-w-full">
                {/* Header Row */}
                <div className="flex">
                  <div className="w-16 h-8 flex-shrink-0" />
                  {TICKERS.map(ticker => (
                    <div 
                      key={ticker} 
                      className="w-10 h-8 flex items-center justify-center text-[9px] font-mono text-muted-foreground -rotate-45"
                    >
                      {ticker}
                    </div>
                  ))}
                </div>
                
                {/* Matrix Rows */}
                {TICKERS.map(rowTicker => (
                  <div key={rowTicker} className="flex">
                    <div className="w-16 h-10 flex items-center justify-end pr-2 text-[10px] font-mono text-muted-foreground flex-shrink-0">
                      {rowTicker}
                    </div>
                    {TICKERS.map(colTicker => {
                      const value = matrix[rowTicker]?.[colTicker] ?? 0
                      const isHovered = hoveredCell?.row === rowTicker && hoveredCell?.col === colTicker
                      const isDiagonal = rowTicker === colTicker
                      
                      return (
                        <div
                          key={`${rowTicker}-${colTicker}`}
                          className={cn(
                            "w-10 h-10 flex items-center justify-center text-[9px] font-mono cursor-pointer transition-all border border-transparent",
                            getColor(value),
                            isHovered && "ring-2 ring-white scale-110 z-10",
                            isDiagonal && "bg-muted"
                          )}
                          style={{ opacity: isDiagonal ? 0.3 : getOpacity(value) }}
                          onMouseEnter={() => setHoveredCell({ row: rowTicker, col: colTicker })}
                          onMouseLeave={() => setHoveredCell(null)}
                        >
                          {isHovered && !isDiagonal && (
                            <span className="text-white font-bold text-[10px]">
                              {value.toFixed(2)}
                            </span>
                          )}
                        </div>
                      )
                    })}
                  </div>
                ))}
              </div>
            </div>
          ) : (
            <div className="flex flex-col items-center justify-center py-12 text-center">
              <Info className="h-12 w-12 text-muted-foreground mb-4" />
              <h3 className="font-semibold text-foreground">No correlation data available</h3>
              <p className="text-sm text-muted-foreground mt-2">
                Click &ldquo;Generate Matrix&rdquo; to compute correlations between all asset pairs.
              </p>
            </div>
          )}
          
          {/* Hover Tooltip */}
          {hoveredCell && matrix[hoveredCell.row]?.[hoveredCell.col] !== undefined && (
            <div className="mt-4 p-3 rounded-lg bg-muted/50 border border-border">
              <div className="flex items-center justify-between">
                <span className="text-sm">
                  <span className="font-mono font-bold text-accent">{hoveredCell.row}</span>
                  <span className="text-muted-foreground mx-2">↔</span>
                  <span className="font-mono font-bold text-accent">{hoveredCell.col}</span>
                </span>
                <span className="font-mono text-lg font-bold">
                  {matrix[hoveredCell.row][hoveredCell.col].toFixed(4)}
                </span>
              </div>
            </div>
          )}
        </CardContent>
      </Card>
    </div>
  )
}
