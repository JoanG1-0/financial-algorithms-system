"use client"

import { useEffect } from "react"
import { useRouter } from "next/navigation"

export default function ETLRedirectPage() {
  const router = useRouter()

  useEffect(() => {
    router.replace("/assets")
  }, [router])

  return (
    <div className="flex h-full items-center justify-center">
      <p className="text-muted-foreground">Redirecting to Assets...</p>
    </div>
  )
}
