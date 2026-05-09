'use client'

import Link from 'next/link'
import { usePathname } from 'next/navigation'
import {
  Database,
  GitCompare,
  Activity,
  Shield,
  Grid3X3,
  FileText,
  Zap,
} from 'lucide-react'
import { cn } from '@/lib/utils'

const navItems = [
  { href: '/dashboard', label: 'Dashboard', icon: Zap },
  { href: '/assets', label: 'Assets', icon: Database },
  { href: '/similarity', label: 'Similarity Analysis', icon: GitCompare },
  { href: '/patterns', label: 'Pattern Detection', icon: Activity },
  { href: '/risk', label: 'Risk Classification', icon: Shield },
  { href: '/correlation', label: 'Correlation Matrix', icon: Grid3X3 },
  { href: '/reports', label: 'Reports & Export', icon: FileText },
]

interface SidebarProps {
  systemHealthy?: boolean
}

export function Sidebar({ systemHealthy = true }: SidebarProps) {
  const pathname = usePathname()

  return (
    <aside className="fixed left-0 top-0 z-40 flex h-screen w-60 flex-col border-r border-border bg-sidebar">
      {/* Logo */}
      <div className="flex h-16 items-center gap-3 border-b border-border px-6">
        <div className="flex h-8 w-8 items-center justify-center rounded bg-primary/10">
          <Zap className="h-5 w-5 text-primary" />
        </div>
        <div>
          <h1 className="font-mono text-lg font-bold tracking-tight text-foreground">
            FinAlgo
          </h1>
          <div className="flex items-center gap-1">
            <span className="font-mono text-[10px] text-muted-foreground">BVC</span>
            <span className="text-[10px] text-muted-foreground">+</span>
            <span className="font-mono text-[10px] text-muted-foreground">S&P500</span>
          </div>
        </div>
      </div>

      {/* Navigation */}
      <nav className="flex-1 space-y-1 px-3 py-4">
        {navItems.map((item) => {
          const isActive = pathname === item.href
          const Icon = item.icon
          
          return (
            <Link
              key={item.href}
              href={item.href}
              className={cn(
                'flex items-center gap-3 rounded-md px-3 py-2.5 text-sm font-medium transition-all',
                isActive
                  ? 'bg-primary/10 text-primary glow-border-active'
                  : 'text-muted-foreground hover:bg-secondary hover:text-foreground'
              )}
            >
              <Icon className={cn('h-4 w-4', isActive && 'text-primary')} />
              <span>{item.label}</span>
            </Link>
          )
        })}
      </nav>

      {/* System Status */}
      <div className="border-t border-border px-4 py-4">
        <div className="flex items-center gap-2">
          <div
            className={cn(
              'h-2 w-2 rounded-full',
              systemHealthy ? 'bg-accent' : 'bg-destructive'
            )}
          />
          <span className="font-mono text-xs text-muted-foreground">
            {systemHealthy ? 'All services healthy' : 'Service issues detected'}
          </span>
        </div>
      </div>
    </aside>
  )
}
