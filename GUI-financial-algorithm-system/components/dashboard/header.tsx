'use client'

import { useState } from 'react'
import { usePathname } from 'next/navigation'
import { Play, Loader2, ChevronRight } from 'lucide-react'
import { toast } from 'sonner'
import { Button } from '@/components/ui/button'
import { api } from '@/lib/api/client'

const breadcrumbMap: Record<string, string> = {
  '/etl': 'Pipeline ETL',
  '/similarity': 'Similarity Analysis',
  '/patterns': 'Pattern Detection',
  '/risk': 'Risk Classification',
  '/heatmap': 'Correlation Heatmap',
  '/reports': 'Reports & Export',
}

interface HeaderProps {
  onAnalysisStart?: () => void
  onAnalysisEnd?: () => void
}

export function Header({ onAnalysisStart, onAnalysisEnd }: HeaderProps) {
  const pathname = usePathname()
  const [isRunning, setIsRunning] = useState(false)
  
  const currentPage = breadcrumbMap[pathname] || 'Dashboard'

  const handleRunAnalysis = async () => {
    setIsRunning(true)
    onAnalysisStart?.()
    
    try {
      await api.runFullAnalysis()
      toast.success('Full analysis completed successfully')
    } catch {
      toast.error('Failed to run analysis. Check if services are running.')
    } finally {
      setIsRunning(false)
      onAnalysisEnd?.()
    }
  }

  return (
    <header className="sticky top-0 z-30 flex h-16 items-center justify-between border-b border-border bg-background/95 px-6 backdrop-blur supports-[backdrop-filter]:bg-background/60">
      {/* Breadcrumb */}
      <nav className="flex items-center gap-2 text-sm">
        <span className="font-mono text-muted-foreground">FinAlgo</span>
        <ChevronRight className="h-4 w-4 text-muted-foreground" />
        <span className="font-medium text-foreground">{currentPage}</span>
      </nav>

      {/* Run Analysis Button */}
      <Button
        onClick={handleRunAnalysis}
        disabled={isRunning}
        className="gap-2 bg-primary text-primary-foreground hover:bg-primary/90"
      >
        {isRunning ? (
          <>
            <Loader2 className="h-4 w-4 animate-spin" />
            Running Analysis...
          </>
        ) : (
          <>
            <Play className="h-4 w-4" />
            Run Full Analysis
          </>
        )}
      </Button>
    </header>
  )
}
