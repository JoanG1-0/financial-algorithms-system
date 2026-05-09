"use client"

import { useState, useEffect } from "react"
import Link from "next/link"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Input } from "@/components/ui/input"
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
import { Search, ExternalLink, Database } from "lucide-react"
import { api } from "@/lib/api/client"
import type { Asset } from "@/lib/api/types"

export default function AssetsPage() {
  const [assets, setAssets] = useState<Asset[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [search, setSearch] = useState("")

  useEffect(() => {
    const fetchAssets = async () => {
      try {
        setLoading(true)
        const data = await api.getAssets()
        setAssets(data)
      } catch (err) {
        setError(err instanceof Error ? err.message : "Failed to fetch assets")
      } finally {
        setLoading(false)
      }
    }
    fetchAssets()
  }, [])

  const filteredAssets = assets.filter(asset => 
    asset.symbol.toLowerCase().includes(search.toLowerCase()) ||
    asset.exchange.toLowerCase().includes(search.toLowerCase()) ||
    asset.type.toLowerCase().includes(search.toLowerCase())
  )

  if (loading) {
    return (
      <div className="flex items-center justify-center h-[60vh]">
        <Spinner className="h-8 w-8" />
      </div>
    )
  }

  if (error) {
    return (
      <div className="flex items-center justify-center h-[60vh]">
        <div className="text-destructive">{error}</div>
      </div>
    )
  }

  return (
    <div className="space-y-6">
      {/* Header */}
      <div>
        <h1 className="text-2xl font-bold text-foreground">Assets</h1>
        <p className="text-muted-foreground text-sm">
          All tracked financial instruments
        </p>
      </div>

      <Card className="border-border/50 bg-card/50">
        <CardHeader className="pb-4">
          <div className="flex items-center justify-between">
            <CardTitle className="text-sm font-medium flex items-center gap-2">
              <Database className="h-4 w-4" />
              {assets.length} Assets Tracked
            </CardTitle>
            <div className="relative w-64">
              <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
              <Input
                placeholder="Search assets..."
                value={search}
                onChange={(e) => setSearch(e.target.value)}
                className="pl-9 bg-background/50"
              />
            </div>
          </div>
        </CardHeader>
        <CardContent>
          <div className="rounded-lg border border-border/50 overflow-hidden">
            <Table>
              <TableHeader>
                <TableRow className="bg-muted/30 hover:bg-muted/30">
                  <TableHead className="font-semibold">Symbol</TableHead>
                  <TableHead className="font-semibold">Type</TableHead>
                  <TableHead className="font-semibold">Exchange</TableHead>
                  <TableHead className="font-semibold">Currency</TableHead>
                  <TableHead className="font-semibold">Interval</TableHead>
                  <TableHead className="font-semibold">Loaded At</TableHead>
                  <TableHead className="font-semibold w-[50px]"></TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {filteredAssets.map((asset) => (
                  <TableRow 
                    key={asset.id} 
                    className="hover:bg-muted/20 cursor-pointer transition-colors"
                  >
                    <TableCell>
                      <Link 
                        href={`/assets/${asset.symbol}`}
                        className="font-mono font-bold text-accent hover:underline"
                      >
                        {asset.symbol}
                      </Link>
                    </TableCell>
                    <TableCell>
                      <Badge variant="outline" className="font-normal">
                        {asset.type}
                      </Badge>
                    </TableCell>
                    <TableCell className="text-muted-foreground">
                      {asset.exchange}
                    </TableCell>
                    <TableCell className="font-mono">
                      {asset.currency}
                    </TableCell>
                    <TableCell className="text-muted-foreground">
                      {asset.interval}
                    </TableCell>
                    <TableCell className="text-muted-foreground text-sm">
                      {new Date(asset.loadedAt).toLocaleString()}
                    </TableCell>
                    <TableCell>
                      <Link 
                        href={`/assets/${asset.symbol}`}
                        className="text-muted-foreground hover:text-foreground transition-colors"
                      >
                        <ExternalLink className="h-4 w-4" />
                      </Link>
                    </TableCell>
                  </TableRow>
                ))}
                {filteredAssets.length === 0 && (
                  <TableRow>
                    <TableCell colSpan={7} className="text-center py-8 text-muted-foreground">
                      No assets found matching &ldquo;{search}&rdquo;
                    </TableCell>
                  </TableRow>
                )}
              </TableBody>
            </Table>
          </div>
        </CardContent>
      </Card>
    </div>
  )
}
