'use client'

import { ReactNode } from 'react'
import { AlertCircle, RefreshCw, Inbox } from 'lucide-react'
import { cn } from '@/lib/utils'
import { Button } from '@/components/ui/button'

// Skeleton Loader with shimmer effect
export function Skeleton({ className }: { className?: string }) {
  return <div className={cn('shimmer rounded', className)} />
}

// Metric Card
interface MetricCardProps {
  label: string
  value: string | number
  subtext?: string
  color?: 'cyan' | 'green' | 'amber' | 'red' | 'default'
  icon?: ReactNode
}

export function MetricCard({
  label,
  value,
  subtext,
  color = 'default',
  icon,
}: MetricCardProps) {
  const colorClasses = {
    cyan: 'border-primary/30 bg-primary/5',
    green: 'border-accent/30 bg-accent/5',
    amber: 'border-warning/30 bg-warning/5',
    red: 'border-destructive/30 bg-destructive/5',
    default: 'border-border bg-card',
  }

  const textClasses = {
    cyan: 'text-primary',
    green: 'text-accent',
    amber: 'text-warning',
    red: 'text-destructive',
    default: 'text-foreground',
  }

  return (
    <div
      className={cn(
        'rounded-lg border p-4 transition-all hover:glow-border',
        colorClasses[color]
      )}
    >
      <div className="flex items-start justify-between">
        <div>
          <p className="font-mono text-xs uppercase tracking-wider text-muted-foreground">
            {label}
          </p>
          <p className={cn('mt-1 font-mono text-2xl font-bold', textClasses[color])}>
            {value}
          </p>
          {subtext && (
            <p className="mt-1 font-mono text-xs text-muted-foreground">{subtext}</p>
          )}
        </div>
        {icon && <div className={cn('opacity-60', textClasses[color])}>{icon}</div>}
      </div>
    </div>
  )
}

// Status Badge
interface StatusBadgeProps {
  status: 'IN_PROGRESS' | 'COMPLETED' | 'FAILED' | string
  size?: 'sm' | 'md'
}

export function StatusBadge({ status, size = 'md' }: StatusBadgeProps) {
  const statusConfig = {
    IN_PROGRESS: {
      bg: 'bg-warning/20',
      text: 'text-warning',
      label: 'In Progress',
      pulse: true,
    },
    COMPLETED: {
      bg: 'bg-accent/20',
      text: 'text-accent',
      label: 'Completed',
      pulse: false,
    },
    FAILED: {
      bg: 'bg-destructive/20',
      text: 'text-destructive',
      label: 'Failed',
      pulse: false,
    },
  }

  const config = statusConfig[status as keyof typeof statusConfig] || {
    bg: 'bg-secondary',
    text: 'text-muted-foreground',
    label: status,
    pulse: false,
  }

  return (
    <span
      className={cn(
        'inline-flex items-center gap-1.5 rounded-full px-2.5 py-1 font-mono text-xs font-medium',
        config.bg,
        config.text,
        config.pulse && 'pulse-amber',
        size === 'sm' && 'px-2 py-0.5 text-[10px]'
      )}
    >
      <span
        className={cn(
          'h-1.5 w-1.5 rounded-full',
          status === 'COMPLETED' && 'bg-accent',
          status === 'IN_PROGRESS' && 'bg-warning',
          status === 'FAILED' && 'bg-destructive'
        )}
      />
      {config.label}
    </span>
  )
}

// Risk Category Badge
interface RiskBadgeProps {
  category: 'CONSERVATIVE' | 'MODERATE' | 'AGGRESSIVE'
}

export function RiskBadge({ category }: RiskBadgeProps) {
  const config = {
    CONSERVATIVE: {
      bg: 'bg-accent/20',
      text: 'text-accent',
      pulse: false,
    },
    MODERATE: {
      bg: 'bg-warning/20',
      text: 'text-warning',
      pulse: false,
    },
    AGGRESSIVE: {
      bg: 'bg-destructive/20',
      text: 'text-destructive',
      pulse: true,
    },
  }

  const { bg, text, pulse } = config[category]

  return (
    <span
      className={cn(
        'inline-flex items-center rounded-full px-2.5 py-1 font-mono text-xs font-medium',
        bg,
        text,
        pulse && 'pulse-aggressive'
      )}
    >
      {category}
    </span>
  )
}

// Complexity Badge
interface ComplexityBadgeProps {
  complexity: string
  warning?: boolean
}

export function ComplexityBadge({ complexity, warning }: ComplexityBadgeProps) {
  return (
    <span
      className={cn(
        'inline-flex items-center rounded px-1.5 py-0.5 font-mono text-[10px]',
        warning
          ? 'bg-warning/20 text-warning'
          : 'bg-secondary text-muted-foreground'
      )}
    >
      {complexity}
    </span>
  )
}

// Error State
interface ErrorStateProps {
  message: string
  onRetry?: () => void
}

export function ErrorState({ message, onRetry }: ErrorStateProps) {
  return (
    <div className="flex flex-col items-center justify-center rounded-lg border border-destructive/30 bg-destructive/5 p-8 text-center">
      <AlertCircle className="h-10 w-10 text-destructive" />
      <p className="mt-4 font-mono text-sm text-destructive">{message}</p>
      {onRetry && (
        <Button
          variant="outline"
          size="sm"
          onClick={onRetry}
          className="mt-4 gap-2"
        >
          <RefreshCw className="h-4 w-4" />
          Retry
        </Button>
      )}
    </div>
  )
}

// Empty State
interface EmptyStateProps {
  title: string
  description: string
  action?: {
    label: string
    onClick: () => void
  }
}

export function EmptyState({ title, description, action }: EmptyStateProps) {
  return (
    <div className="flex flex-col items-center justify-center rounded-lg border border-border bg-card p-12 text-center">
      <Inbox className="h-12 w-12 text-muted-foreground" />
      <h3 className="mt-4 font-semibold text-foreground">{title}</h3>
      <p className="mt-2 max-w-sm font-mono text-sm text-muted-foreground">
        {description}
      </p>
      {action && (
        <Button onClick={action.onClick} className="mt-6 gap-2">
          {action.label}
        </Button>
      )}
    </div>
  )
}

// Panel Container
interface PanelProps {
  title: string
  children: ReactNode
  className?: string
  headerAction?: ReactNode
}

export function Panel({ title, children, className, headerAction }: PanelProps) {
  return (
    <div
      className={cn(
        'rounded-lg border border-border bg-card scanline-overlay',
        className
      )}
    >
      <div className="flex items-center justify-between border-b border-border px-4 py-3">
        <h2 className="font-mono text-sm font-semibold uppercase tracking-wider text-foreground">
          {title}
        </h2>
        {headerAction}
      </div>
      <div className="p-4">{children}</div>
    </div>
  )
}

// Progress Bar
interface ProgressBarProps {
  value: number
  max: number
  color?: 'cyan' | 'green' | 'amber' | 'red'
  showLabel?: boolean
}

export function ProgressBar({
  value,
  max,
  color = 'cyan',
  showLabel = true,
}: ProgressBarProps) {
  const percentage = max > 0 ? (value / max) * 100 : 0

  const colorClasses = {
    cyan: 'bg-primary',
    green: 'bg-accent',
    amber: 'bg-warning',
    red: 'bg-destructive',
  }

  return (
    <div className="space-y-1">
      <div className="h-2 overflow-hidden rounded-full bg-secondary">
        <div
          className={cn('h-full transition-all duration-500', colorClasses[color])}
          style={{ width: `${percentage}%` }}
        />
      </div>
      {showLabel && (
        <div className="flex justify-between font-mono text-xs text-muted-foreground">
          <span>
            {value} / {max}
          </span>
          <span>{percentage.toFixed(1)}%</span>
        </div>
      )}
    </div>
  )
}

// Stat Card for Dashboard
interface StatCardProps {
  title: string
  value: string | number
  icon?: ReactNode
  subtitle?: string
}

export function StatCard({ title, value, icon, subtitle }: StatCardProps) {
  return (
    <div className="rounded-lg border border-border/50 bg-card/50 p-4">
      <div className="flex items-start justify-between">
        <div>
          <p className="text-xs font-medium text-muted-foreground uppercase tracking-wider">
            {title}
          </p>
          <p className="mt-1 text-2xl font-bold font-mono text-foreground">
            {value}
          </p>
          {subtitle && (
            <p className="mt-0.5 text-xs text-muted-foreground">{subtitle}</p>
          )}
        </div>
        {icon && (
          <div className="text-muted-foreground">{icon}</div>
        )}
      </div>
    </div>
  )
}

// Data Quality Badge
interface DataQualityBadgeProps {
  quality: 'CLEAN' | 'FORWARD_FILLED' | 'ANOMALY_CORRECTED' | 'ANOMALY_FLAGGED'
}

export function DataQualityBadge({ quality }: DataQualityBadgeProps) {
  const config = {
    CLEAN: {
      bg: 'bg-green-500/20',
      text: 'text-green-400',
      label: 'Clean',
    },
    FORWARD_FILLED: {
      bg: 'bg-yellow-500/20',
      text: 'text-yellow-400',
      label: 'Filled',
    },
    ANOMALY_CORRECTED: {
      bg: 'bg-orange-500/20',
      text: 'text-orange-400',
      label: 'Corrected',
    },
    ANOMALY_FLAGGED: {
      bg: 'bg-red-500/20',
      text: 'text-red-400',
      label: 'Flagged',
    },
  }

  const { bg, text, label } = config[quality]

  return (
    <span
      className={cn(
        'inline-flex items-center rounded-full px-2 py-0.5 font-mono text-[10px] font-medium',
        bg,
        text
      )}
    >
      {label}
    </span>
  )
}
