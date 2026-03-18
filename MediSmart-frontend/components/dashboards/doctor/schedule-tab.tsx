"use client"
import API_BASE_URL from "@/config/api"
import { useEffect, useMemo, useState } from "react"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { Avatar, AvatarFallback, AvatarImage } from "@/components/ui/avatar"
import { Calendar, Clock, Video, MapPin, User } from "lucide-react"

type Appointment = {
  id: string
  patientName: string
  appointmentDate?: string
  appointmentTime?: string
  status?: string
  reason?: string
}

export function ScheduleTab() {
  const [appointments, setAppointments] = useState<Appointment[]>([])
  const [loading, setLoading] = useState(false)

  const token = useMemo(() => (typeof window === "undefined" ? null : localStorage.getItem("token")), [])
  const doctorId = useMemo(() => {
    if (typeof window === "undefined") return null
    const storedUser = localStorage.getItem("user")
    return storedUser ? JSON.parse(storedUser)?.id : null
  }, [])

  useEffect(() => {
    const fetchAppointments = async () => {
      if (!doctorId || !token) return
      setLoading(true)
      try {
        const res = await fetch(`${API_BASE_URL}/api/appointments/doctor/${doctorId}`, {
          headers: { Authorization: `Bearer ${token}` },
        })
        if (!res.ok) throw new Error("Failed to load schedule")
        const data = await res.json()
        setAppointments(Array.isArray(data) ? data : [])
      } catch (err) {
        console.error(err)
      } finally {
        setLoading(false)
      }
    }
    fetchAppointments()
  }, [doctorId, token])

  const today = new Date().toISOString().slice(0, 10)
  const todaysAppointments = useMemo(
    () =>
      appointments
        .filter((a) => !a.appointmentDate || a.appointmentDate === today)
        .sort((a, b) => (a.appointmentTime || "").localeCompare(b.appointmentTime || "")),
    [appointments, today],
  )

  const totals = useMemo(() => {
    const confirmed = todaysAppointments.filter((a) => (a.status || "").toUpperCase() === "APPROVED").length
    const pending = todaysAppointments.filter((a) => (a.status || "").toUpperCase() === "PENDING").length
    return { total: todaysAppointments.length, confirmed, pending }
  }, [todaysAppointments])

  const getStatusColor = (status: string) => {
    switch (status) {
      case "APPROVED":
        return "bg-green-100 text-green-800"
      case "PENDING":
        return "bg-yellow-100 text-yellow-800"
      case "REJECTED":
        return "bg-red-100 text-red-800"
      default:
        return "bg-gray-100 text-gray-800"
    }
  }

  const formatTime = (t?: string) => {
    if (!t) return "Not set"
    return t.length >= 5 ? t.slice(0, 5) : t
  }

  return (
    <div className="space-y-6">
      <div>
        <h2 className="text-2xl font-bold text-foreground mb-2">My Schedule</h2>
        <p className="text-muted-foreground">View your daily appointment schedule</p>
      </div>

      <Card>
        <CardHeader>
          <CardTitle className="flex items-center space-x-2">
            <Calendar className="h-5 w-5" />
            <span>Today's Schedule - {today}</span>
          </CardTitle>
          <CardDescription>Your appointments for today</CardDescription>
        </CardHeader>
        <CardContent>
          {loading ? (
            <p className="text-sm text-muted-foreground">Loading schedule…</p>
          ) : todaysAppointments.length === 0 ? (
            <p className="text-sm text-muted-foreground">No appointments scheduled for today.</p>
          ) : (
            <div className="space-y-4">
              {todaysAppointments.map((appointment) => (
                <div key={appointment.id} className="flex items-center space-x-4 p-4 border border-border rounded-lg">
                  <div className="text-center min-w-[80px]">
                    <div className="font-semibold text-foreground">{formatTime(appointment.appointmentTime)}</div>
                    <div className="text-sm text-muted-foreground">30 min</div>
                  </div>

                  <div className="flex-1 flex items-center space-x-4">
                    <Avatar className="h-10 w-10">
                      <AvatarImage src="/default-profile-avatar.png" />
                      <AvatarFallback>
                        {(appointment.patientName || "P")
                          .split(" ")
                          .map((n) => n[0])
                          .join("")
                          .slice(0, 2)}
                      </AvatarFallback>
                    </Avatar>

                    <div className="flex-1">
                      <h4 className="font-semibold text-foreground">{appointment.patientName || "Patient"}</h4>
                      <div className="flex items-center space-x-2 text-sm text-muted-foreground">
                        <Video className="h-4 w-4" />
                        <span>Online Consultation</span>
                      </div>
                      <p className="text-xs text-muted-foreground">{appointment.reason || ""}</p>
                    </div>
                  </div>

                  <div className="flex items-center space-x-2">
                    <Badge className={getStatusColor((appointment.status || "").toUpperCase())}>
                      {(appointment.status || "PENDING").toUpperCase()}
                    </Badge>
                    {(appointment.status || "").toUpperCase() === "APPROVED" && (
                      <Button size="sm" variant="default">
                        Join Call
                      </Button>
                    )}
                  </div>
                </div>
              ))}
            </div>
          )}
        </CardContent>
      </Card>

      <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
        <Card>
          <CardContent className="p-6 text-center">
            <div className="text-2xl font-bold text-primary mb-2">{totals.total}</div>
            <div className="text-sm text-muted-foreground">Total Appointments</div>
          </CardContent>
        </Card>
        <Card>
          <CardContent className="p-6 text-center">
            <div className="text-2xl font-bold text-green-600 mb-2">{totals.confirmed}</div>
            <div className="text-sm text-muted-foreground">Confirmed</div>
          </CardContent>
        </Card>
        <Card>
          <CardContent className="p-6 text-center">
            <div className="text-2xl font-bold text-yellow-600 mb-2">{totals.pending}</div>
            <div className="text-sm text-muted-foreground">Pending</div>
          </CardContent>
        </Card>
      </div>
    </div>
  )
}
