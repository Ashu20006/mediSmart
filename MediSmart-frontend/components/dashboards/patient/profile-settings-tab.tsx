"use client"
import API_BASE_URL from "@/config/api"
import { useEffect, useState } from "react"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { Label } from "@/components/ui/label"
import { Input } from "@/components/ui/input"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import { Button } from "@/components/ui/button"
import { useToast } from "@/hooks/use-toast"

type ProfileForm = {
  name: string
  email: string
  phoneNumber: string
  gender: string
  age: string
  location: string
}

const DEFAULT_FORM: ProfileForm = {
  name: "",
  email: "",
  phoneNumber: "",
  gender: "",
  age: "",
  location: "",
}

export function ProfileSettingsTab() {
  const [form, setForm] = useState<ProfileForm>(DEFAULT_FORM)
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const { toast } = useToast()

  // Load current profile
  useEffect(() => {
    const loadProfile = async () => {
      try {
        const token = localStorage.getItem("token")
        if (!token) throw new Error("Missing auth token")

        const res = await fetch(`${API_BASE_URL}/api/auth/me`, {
          headers: { Authorization: `Bearer ${token}` },
        })

        if (!res.ok) throw new Error(`Failed to fetch profile (${res.status})`)

        const data = await res.json()
        setForm({
          name: data.name || "",
          email: data.email || "",
          phoneNumber: data.phoneNumber || "",
          gender: data.gender || "",
          age: data.age ? String(data.age) : "",
          location: data.location || "",
        })
      } catch (err) {
        console.error(err)
        toast({
          title: "Could not load profile",
          description: err instanceof Error ? err.message : "Please try again",
          variant: "destructive",
        })
      } finally {
        setLoading(false)
      }
    }

    loadProfile()
  }, [toast])

  const handleChange = (key: keyof ProfileForm, value: string) => {
    setForm((prev) => ({ ...prev, [key]: value }))
  }

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setSaving(true)

    try {
      const token = localStorage.getItem("token")
      if (!token) throw new Error("Missing auth token")

      const res = await fetch(`${API_BASE_URL}/api/users/profile`, {
        method: "PUT",
        headers: {
          "Content-Type": "application/json",
          Authorization: `Bearer ${token}`,
        },
        body: JSON.stringify({
          name: form.name,
          phoneNumber: form.phoneNumber,
          gender: form.gender,
          age: form.age ? Number(form.age) : null,
          location: form.location,
        }),
      })

      if (!res.ok) {
        throw new Error("Profile update failed on server")
      }

      toast({
        title: "Profile updated",
        description: "Your profile details were saved successfully.",
      })
    } catch (err) {
      console.error(err)
      toast({
        title: "Update failed",
        description:
          err instanceof Error
            ? err.message
            : "We could not save your changes. Please retry in a moment.",
        variant: "destructive",
      })
    } finally {
      setSaving(false)
    }
  }

  return (
    <div className="space-y-6">
      <div>
        <h2 className="text-2xl font-bold text-foreground mb-1">Profile Settings</h2>
        <p className="text-muted-foreground">
          Update your personal information and contact details
        </p>
      </div>

      <Card>
        <CardHeader>
          <CardTitle>Personal Information</CardTitle>
          <CardDescription>Keep your profile up to date for better care</CardDescription>
        </CardHeader>
        <CardContent>
          <form onSubmit={handleSubmit} className="space-y-4">
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              <div className="space-y-2">
                <Label htmlFor="name">Full Name</Label>
                <Input
                  id="name"
                  value={form.name}
                  onChange={(e) => handleChange("name", e.target.value)}
                  placeholder="Shivam Sharma"
                  disabled={loading}
                  required
                />
              </div>
              <div className="space-y-2">
                <Label htmlFor="email">Email</Label>
                <Input
                  id="email"
                  type="email"
                  value={form.email}
                  disabled
                  className="bg-muted/60"
                />
              </div>
              <div className="space-y-2">
                <Label htmlFor="phone">Phone Number</Label>
                <Input
                  id="phone"
                  value={form.phoneNumber}
                  onChange={(e) => handleChange("phoneNumber", e.target.value)}
                  placeholder="+1 555 123 4567"
                  disabled={loading}
                />
              </div>
              <div className="space-y-2">
                <Label htmlFor="gender">Gender</Label>
                <Select
                  value={form.gender}
                  onValueChange={(value) => handleChange("gender", value)}
                  disabled={loading}
                >
                  <SelectTrigger id="gender">
                    <SelectValue placeholder="Select gender" />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="MALE">Male</SelectItem>
                    <SelectItem value="FEMALE">Female</SelectItem>
                    <SelectItem value="OTHER">Other</SelectItem>
                  </SelectContent>
                </Select>
              </div>
              <div className="space-y-2">
                <Label htmlFor="age">Age</Label>
                <Input
                  id="age"
                  type="number"
                  min={0}
                  value={form.age}
                  onChange={(e) => handleChange("age", e.target.value)}
                  placeholder="12"
                  disabled={loading}
                />
              </div>
              <div className="space-y-2">
                <Label htmlFor="location">Location</Label>
                <Input
                  id="location"
                  value={form.location}
                  onChange={(e) => handleChange("location", e.target.value)}
                  placeholder="Bengaluru, IN"
                  disabled={loading}
                />
              </div>
            </div>

            <div className="flex justify-end">
              <Button type="submit" disabled={saving || loading}>
                {saving ? "Saving..." : "Save Changes"}
              </Button>
            </div>
          </form>
        </CardContent>
      </Card>
    </div>
  )
}
