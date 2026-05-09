"use client"

import { useState, useEffect, use } from "react"
import Link from "next/link"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Button } from "@/components/ui/button"
import { Badge } from "@/components/ui/badge"
import { Spinner } from "@/components/ui/spinner"
import { 
  Table, 
  TableBody, 
  TableCell, 
  TableHead, 
  TableHeader, 
  TableRow 
} from "@/components/ui/table"
import {
  ArrowLeft, 
  TrendingUp, 
  Activity, 
  ChevronLeft, 
  ChevronRight,
  BarChart3
} from "lucide-react"
import { 
  LineChart, 
  Line, 
  XAxis, 
  YAxis, 
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
  ComposedChart,
  Bar,
  Brush,
} from "recharts"
import { api } from "@/lib/api/client"
import type {
  PriceRecord,
  CleanedPriceRecord,
  PatternRecord,
  SmaRecord,
  PaginatedResponse
} from "@/lib/api/types"
import { DataQualityBadge } from "@/components/dashboard/shared"

interface PageProps {
  params: Promise<{ symbol: string }>
}

export default function AssetDetailPage({ params }: PageProps) {
  const { symbol } = use(params)
  
  const [priceData, setPriceData] = useState<PaginatedResponse<PriceRecord> | null>(null)
  const [cleanedData, setCleanedData] = useState<PaginatedResponse<CleanedPriceRecord> | null>(null)
  const [patterns, setPatterns] = useState<PatternRecord[]>([])
  const [smaData20, setSmaData20] = useState<SmaRecord | null>(null)
  const [smaData50, setSmaData50] = useState<SmaRecord | null>(null)
  const [smaData100, setSmaData100] = useState<SmaRecord | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [pricePage, setPricePage] = useState(0)
  const [cleanedPage, setCleanedPage] = useState(0)

  useEffect(() => {
    const fetchData = async () => {
      try {
        setLoading(true)
        setError(null)
        
        const [prices, cleaned, patternData, sma20, sma50, sma100] = await Promise.all([
          api.getPriceHistory(symbol, 0, 1300).catch(() => null),
          api.getCleanedPrices(symbol, cleanedPage, 30).catch(() => null),
          api.getPatternsBySymbol(symbol).catch(() => []),
          api.getSMA(symbol, 20).catch(() => null),
          api.getSMA(symbol, 50).catch(() => null),
          api.getSMA(symbol, 100).catch(() => null),
        ])

        setPriceData(prices)
        setCleanedData(cleaned)
        setPatterns(patternData)
        setSmaData20(sma20)
        setSmaData50(sma50)
        setSmaData100(sma100)
      } catch (err) {
        setError(err instanceof Error ? err.message : "Failed to fetch data")
      } finally {
        setLoading(false)
      }
    }
    fetchData()
  }, [symbol, cleanedPage])

  // Prepare chart data
  const reversedPrices = priceData?.content?.slice().reverse() ?? []
  const parseSma = (rec: SmaRecord | null): (number | null)[] =>
    rec ? JSON.parse((rec.valuesJson || '[]').replace(/\bNaN\b/g, 'null')) : []
  const alignedSma20 = parseSma(smaData20).slice(-reversedPrices.length)
  const alignedSma50 = parseSma(smaData50).slice(-reversedPrices.length)
  const alignedSma100 = parseSma(smaData100).slice(-reversedPrices.length)
  const chartData = reversedPrices.map((price, index) => ({
    date: price.datetime,
    close: price.close,
    volume: price.volume,
    sma20: alignedSma20[index] ?? null,
    sma50: alignedSma50[index] ?? null,
    sma100: alignedSma100[index] ?? null,
  }))

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
      <div className="flex items-center gap-4">
        <Link href="/assets">
          <Button variant="ghost" size="icon">
            <ArrowLeft className="h-5 w-5" />
          </Button>
        </Link>
        <div>
          <h1 className="text-2xl font-bold text-foreground font-mono">{symbol}</h1>
          <p className="text-muted-foreground text-sm">
            Asset detail and price history
          </p>
        </div>
      </div>

      {error && (
        <div className="bg-destructive/10 border border-destructive/20 rounded-lg p-4">
          <span className="text-destructive">{error}</span>
        </div>
      )}

      {/* Price Chart */}
      <Card className="border-border/50 bg-card/50">
        <CardHeader className="pb-2">
          <div className="flex items-center justify-between">
            <CardTitle className="text-sm font-medium flex items-center gap-2">
              <TrendingUp className="h-4 w-4" />
              Price History
            </CardTitle>
            <span className="text-xs text-muted-foreground font-mono">SMA 20 / 50 / 100</span>
          </div>
        </CardHeader>
        <CardContent>
          <ResponsiveContainer width="100%" height={350}>
            <ComposedChart data={chartData}>
              <CartesianGrid strokeDasharray="3 3" stroke="hsl(var(--border))" opacity={0.3} />
              <XAxis
                dataKey="date"
                tick={{ fill: '#ffffff', fontSize: 10 }}
                tickFormatter={(value) => new Date(value).toLocaleDateString('en-US', { month: 'short', day: 'numeric' })}
              />
              <YAxis
                yAxisId="price"
                tick={{ fill: '#ffffff', fontSize: 10 }}
                domain={['auto', 'auto']}
              />
              <YAxis
                yAxisId="volume"
                orientation="right"
                tick={{ fill: '#ffffff', fontSize: 10 }}
                tickFormatter={(value) => `${(value / 1000000).toFixed(0)}M`}
              />
              <Tooltip
                contentStyle={{
                  backgroundColor: '#0D1117',
                  border: '1px solid #1E2433',
                  borderRadius: '8px',
                  color: '#ffffff',
                }}
                formatter={(value: number, name: string) => {
                  if (name === 'volume') return [`${(value / 1000000).toFixed(2)}M`, 'Volume']
                  const labels: Record<string, string> = { close: 'Close', sma20: 'SMA-20', sma50: 'SMA-50', sma100: 'SMA-100' }
                  return [`$${value.toFixed(2)}`, labels[name] ?? name]
                }}
              />
              <Bar 
                yAxisId="volume" 
                dataKey="volume" 
                fill="hsl(var(--muted))" 
                opacity={0.3}
              />
              <Line 
                yAxisId="price"
                type="monotone" 
                dataKey="close" 
                stroke="hsl(var(--accent))" 
                strokeWidth={2}
                dot={false}
              />
              {smaData20 && (
                <Line yAxisId="price" type="monotone" dataKey="sma20"
                  stroke="#F59E0B" strokeWidth={1.5} strokeDasharray="5 5" dot={false} />
              )}
              {smaData50 && (
                <Line yAxisId="price" type="monotone" dataKey="sma50"
                  stroke="#10B981" strokeWidth={1.5} strokeDasharray="5 5" dot={false} />
              )}
              {smaData100 && (
                <Line yAxisId="price" type="monotone" dataKey="sma100"
                  stroke="#EF4444" strokeWidth={1.5} strokeDasharray="5 5" dot={false} />
              )}
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
          <div className="flex items-center justify-center gap-6 mt-4 text-sm flex-wrap">
            <div className="flex items-center gap-2">
              <div className="w-4 h-0.5 bg-accent" />
              <span className="text-muted-foreground">Close Price</span>
            </div>
            <div className="flex items-center gap-2">
              <div className="w-4 h-0.5" style={{ backgroundColor: '#F59E0B', borderTop: '1.5px dashed #F59E0B' }} />
              <span className="text-muted-foreground">SMA-20</span>
            </div>
            <div className="flex items-center gap-2">
              <div className="w-4 h-0.5" style={{ backgroundColor: '#10B981', borderTop: '1.5px dashed #10B981' }} />
              <span className="text-muted-foreground">SMA-50</span>
            </div>
            <div className="flex items-center gap-2">
              <div className="w-4 h-0.5" style={{ backgroundColor: '#EF4444', borderTop: '1.5px dashed #EF4444' }} />
              <span className="text-muted-foreground">SMA-100</span>
            </div>
          </div>
        </CardContent>
      </Card>

      {/* Pattern Cards */}
      {patterns.length > 0 && (
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          {patterns.map((pattern) => (
            <Card key={pattern.id} className="border-border/50 bg-card/50">
              <CardHeader className="pb-2">
                <CardTitle className="text-sm font-medium flex items-center gap-2">
                  <Activity className="h-4 w-4" />
                  {pattern.patternType.replace('_', ' ')}
                </CardTitle>
              </CardHeader>
              <CardContent>
                <div className="flex items-center justify-between">
                  <div>
                    <div className="text-3xl font-bold font-mono">{pattern.occurrences}</div>
                    <div className="text-xs text-muted-foreground">Occurrences</div>
                  </div>
                  <div className="text-right">
                    <div className="text-xl font-mono text-accent">
                      {(pattern.relativeFrequency * 100).toFixed(1)}%
                    </div>
                    <div className="text-xs text-muted-foreground">Relative Frequency</div>
                  </div>
                </div>
              </CardContent>
            </Card>
          ))}
        </div>
      )}

      {/* Cleaned Data Table */}
      <Card className="border-border/50 bg-card/50">
        <CardHeader className="pb-2">
          <div className="flex items-center justify-between">
            <CardTitle className="text-sm font-medium flex items-center gap-2">
              <BarChart3 className="h-4 w-4" />
              Cleaned Price Data
            </CardTitle>
            {cleanedData && (
              <span className="text-sm text-muted-foreground">
                {cleanedData.totalElements.toLocaleString()} records
              </span>
            )}
          </div>
        </CardHeader>
        <CardContent>
          <div className="rounded-lg border border-border/50 overflow-hidden">
            <Table>
              <TableHeader>
                <TableRow className="bg-muted/30 hover:bg-muted/30">
                  <TableHead className="font-semibold">Date</TableHead>
                  <TableHead className="font-semibold text-right">Open</TableHead>
                  <TableHead className="font-semibold text-right">High</TableHead>
                  <TableHead className="font-semibold text-right">Low</TableHead>
                  <TableHead className="font-semibold text-right">Close</TableHead>
                  <TableHead className="font-semibold text-right">Volume</TableHead>
                  <TableHead className="font-semibold">Quality</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {cleanedData?.content.map((record) => (
                  <TableRow key={record.id} className="hover:bg-muted/20">
                    <TableCell className="font-mono text-sm">{record.date}</TableCell>
                    <TableCell className="text-right font-mono">${record.open.toFixed(2)}</TableCell>
                    <TableCell className="text-right font-mono">${record.high.toFixed(2)}</TableCell>
                    <TableCell className="text-right font-mono">${record.low.toFixed(2)}</TableCell>
                    <TableCell className="text-right font-mono">${record.close.toFixed(2)}</TableCell>
                    <TableCell className="text-right font-mono text-muted-foreground">
                      {(record.volume / 1000000).toFixed(2)}M
                    </TableCell>
                    <TableCell>
                      <DataQualityBadge quality={record.dataQuality} />
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </div>
          
          {/* Pagination */}
          {cleanedData && cleanedData.totalPages > 1 && (
            <div className="flex items-center justify-between mt-4">
              <span className="text-sm text-muted-foreground">
                Page {cleanedData.number + 1} of {cleanedData.totalPages}
              </span>
              <div className="flex items-center gap-2">
                <Button 
                  variant="outline" 
                  size="sm"
                  disabled={cleanedData.first}
                  onClick={() => setCleanedPage(p => p - 1)}
                >
                  <ChevronLeft className="h-4 w-4" />
                </Button>
                <Button 
                  variant="outline" 
                  size="sm"
                  disabled={cleanedData.last}
                  onClick={() => setCleanedPage(p => p + 1)}
                >
                  <ChevronRight className="h-4 w-4" />
                </Button>
              </div>
            </div>
          )}
        </CardContent>
      </Card>
    </div>
  )
}
