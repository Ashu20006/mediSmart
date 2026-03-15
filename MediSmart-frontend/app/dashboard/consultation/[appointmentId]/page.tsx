"use client"

import { ConsultationRoom } from "@/components/consultation/consultation-room"
import { Navigation } from "@/components/navigation"
import withAuth from "@/components/auth/withAuth"

interface Props {
  params: { appointmentId: string }
}

function ConsultationPage({ params }: Props) {
  const { appointmentId } = params

  const storedUser = typeof window !== "undefined" ? localStorage.getItem("user") : null
  let currentUser = {} as { id?: string; name?: string }
  if (storedUser) {
    try {
      const parsed = JSON.parse(storedUser)
      currentUser = { id: parsed?.id, name: parsed?.name }
    } catch {
      currentUser = {}
    }
  }

  return (
    <div className="min-h-screen bg-muted/30">
      <Navigation />
      <div className="pt-16">
        <ConsultationRoom appointmentId={appointmentId} currentUser={currentUser} />
      </div>
    </div>
  )
}

export default withAuth(ConsultationPage)
