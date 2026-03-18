"use client"
import API_BASE_URL from "@/config/api"
import type React from "react"

import { useEffect, useMemo, useState } from "react"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { Label } from "@/components/ui/label"
import { Textarea } from "@/components/ui/textarea"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import { Star } from "lucide-react"
import { useToast } from "@/hooks/use-toast"

type AppointmentOption = {
  id: string
  doctorName: string
  appointmentDate?: string
  status?: string
}

type FeedbackItem = {
  id: string
  appointmentId: string
  doctorName: string
  rating: number
  comment: string
  createdAt?: string
}

export function FeedbackTab() {
  const [rating, setRating] = useState(0)
  const [hoveredRating, setHoveredRating] = useState(0)
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [appointments, setAppointments] = useState<AppointmentOption[]>([])
  const [selectedAppointment, setSelectedAppointment] = useState("")
  const [comment, setComment] = useState("")
  const [feedback, setFeedback] = useState<FeedbackItem[]>([])
  const { toast } = useToast()

  const token = useMemo(() => (typeof window === "undefined" ? null : localStorage.getItem("token")), [])

  useEffect(() => {
    if (!token) return

    const fetchAppointments = async () => {
      const res = await fetch(`${API_BASE_URL}/api/patient/feedback/appointments`, {
        headers: { Authorization: `Bearer ${token}` },
      })
      if (res.ok) {
        const data = await res.json()
        setAppointments(
          data.map((item: any) => ({
            id: item.id,
            doctorName: item.doctorName ?? "Doctor",
            appointmentDate: item.appointmentDate,
            status: item.status,
          })),
        )
      }
    }

    const fetchFeedback = async () => {
      const res = await fetch(`${API_BASE_URL}/api/patient/feedback`, {
        headers: { Authorization: `Bearer ${token}` },
      })
      if (res.ok) {
        const data = await res.json()
        setFeedback(data)
      }
    }

    fetchAppointments()
    fetchFeedback()
  }, [token])

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!selectedAppointment) {
      toast({ title: "Pick an appointment", variant: "destructive" })
      return
    }
    if (rating === 0) {
      toast({ title: "Add a rating", variant: "destructive" })
      return
    }
    setIsSubmitting(true)

    try {
      const res = await fetch(`${API_BASE_URL}/api/patient/feedback`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          Authorization: `Bearer ${token}`,
        },
        body: JSON.stringify({
          appointmentId: selectedAppointment,
          rating,
          comment,
        }),
      })

      if (!res.ok) {
        const message = await res.text()
        throw new Error(message || "Failed to submit feedback")
      }

      const newFeedback = await res.json()
      setFeedback((prev) => [newFeedback, ...prev])
      toast({
        title: "Feedback submitted",
        description: "Thank you for sharing your experience.",
      })
      setSelectedAppointment("")
      setRating(0)
      setComment("")
    } catch (err) {
      toast({
        title: "Could not submit",
        description: err instanceof Error ? err.message : "Please try again",
        variant: "destructive",
      })
    } finally {
      setIsSubmitting(false)
    }
  }

  const StarRating = () => (
    <div className="flex space-x-1">
      {[1, 2, 3, 4, 5].map((star) => (
        <button
          key={star}
          type="button"
          className="focus:outline-none"
          onMouseEnter={() => setHoveredRating(star)}
          onMouseLeave={() => setHoveredRating(0)}
          onClick={() => setRating(star)}
        >
          <Star
            className={`h-6 w-6 transition-colors ${
              star <= (hoveredRating || rating) ? "text-yellow-500 fill-current" : "text-gray-300"
            }`}
          />
        </button>
      ))}
    </div>
  )

  return (
    <div className="space-y-6">
      <div>
        <h2 className="text-2xl font-bold text-foreground mb-2">Doctor Feedback</h2>
        <p className="text-muted-foreground">Share your experience and help other patients</p>
      </div>

      <Card>
        <CardHeader>
          <CardTitle>Submit Feedback</CardTitle>
          <CardDescription>Rate your recent appointment and share your experience</CardDescription>
        </CardHeader>
        <CardContent>
          <form onSubmit={handleSubmit} className="space-y-4">
            <div className="space-y-2">
              <Label htmlFor="appointment">Select Appointment</Label>
              <Select value={selectedAppointment} onValueChange={setSelectedAppointment}>
                <SelectTrigger>
                  <SelectValue placeholder="Choose a recent appointment" />
                </SelectTrigger>
                <SelectContent>
                  {appointments.map((appointment) => (
                    <SelectItem key={appointment.id} value={appointment.id}>
                      {appointment.doctorName} - {appointment.appointmentDate ?? "Date TBD"}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>

            <div className="space-y-2">
              <Label>Overall Rating</Label>
              <div className="flex items-center space-x-2">
                <StarRating />
                <span className="text-sm text-muted-foreground">{rating > 0 && `${rating} out of 5 stars`}</span>
              </div>
            </div>

            <div className="space-y-2">
              <Label htmlFor="feedback">Your Feedback</Label>
              <Textarea
                id="feedback"
                placeholder="Share your experience with the doctor and the appointment..."
                className="min-h-[120px]"
                value={comment}
                onChange={(e) => setComment(e.target.value)}
                required
              />
            </div>

            <Button type="submit" disabled={isSubmitting || rating === 0 || !selectedAppointment}>
              {isSubmitting ? "Submitting..." : "Submit Feedback"}
            </Button>
          </form>
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle>Your Previous Feedback</CardTitle>
          <CardDescription>Feedback you've submitted for past appointments</CardDescription>
        </CardHeader>
        <CardContent>
          {feedback.length === 0 ? (
            <p className="text-sm text-muted-foreground">No feedback submitted yet.</p>
          ) : (
            <div className="space-y-4">
              {feedback.map((item) => (
                <div key={item.id} className="border-l-4 border-primary pl-4">
                  <div className="flex items-center space-x-2 mb-2">
                    <h4 className="font-semibold text-foreground">{item.doctorName}</h4>
                    <div className="flex space-x-1">
                      {[1, 2, 3, 4, 5].map((star) => (
                        <Star
                          key={star}
                          className={`h-4 w-4 ${
                            star <= item.rating ? "text-yellow-500 fill-current" : "text-gray-300"
                          }`}
                        />
                      ))}
                    </div>
                  </div>
                  <p className="text-sm text-muted-foreground mb-1">
                    {item.createdAt ? new Date(item.createdAt).toLocaleDateString() : ""}
                  </p>
                  <p className="text-sm">{item.comment || "No additional comments."}</p>
                </div>
              ))}
            </div>
          )}
        </CardContent>
      </Card>
    </div>
  )
}
