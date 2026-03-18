"use client"
import API_BASE_URL from "@/config/api"
import { useState } from "react"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { Label } from "@/components/ui/label"
import { Input } from "@/components/ui/input"
import { Switch } from "@/components/ui/switch"
import { Button } from "@/components/ui/button"
import { useToast } from "@/hooks/use-toast"

type PasswordForm = {
  currentPassword: string
  newPassword: string
  confirmPassword: string
}

export function AccountSettingsTab() {
  const [passwordForm, setPasswordForm] = useState<PasswordForm>({
    currentPassword: "",
    newPassword: "",
    confirmPassword: "",
  })
  const [notifications, setNotifications] = useState({
    email: true,
    sms: false,
    reminders: true,
  })
  const [saving, setSaving] = useState(false)
  const { toast } = useToast()

  const handlePasswordChange = (key: keyof PasswordForm, value: string) => {
    setPasswordForm((prev) => ({ ...prev, [key]: value }))
  }

  const handlePasswordSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    if (passwordForm.newPassword !== passwordForm.confirmPassword) {
      toast({
        title: "Passwords do not match",
        description: "New password and confirmation must be the same.",
        variant: "destructive",
      })
      return
    }

    setSaving(true)
    try {
      const token = localStorage.getItem("token")
      if (!token) throw new Error("Missing auth token")

      const res = await fetch(`${API_BASE_URL}/api/users/password`, {
        method: "PUT",
        headers: {
          "Content-Type": "application/json",
          Authorization: `Bearer ${token}`,
        },
        body: JSON.stringify({
          currentPassword: passwordForm.currentPassword,
          newPassword: passwordForm.newPassword,
        }),
      })

      if (!res.ok) throw new Error("Password update failed on server")

      toast({
        title: "Password updated",
        description: "You may need to log in again on other devices.",
      })
      setPasswordForm({ currentPassword: "", newPassword: "", confirmPassword: "" })
    } catch (err) {
      console.error(err)
      toast({
        title: "Could not update password",
        description: err instanceof Error ? err.message : "Try again in a moment.",
        variant: "destructive",
      })
    } finally {
      setSaving(false)
    }
  }

  const handleNotificationSave = () => {
    toast({
      title: "Notification preferences saved",
      description: "Preferences are stored locally for now.",
    })
  }

  return (
    <div className="space-y-6">
      <div>
        <h2 className="text-2xl font-bold text-foreground mb-1">Account Settings</h2>
        <p className="text-muted-foreground">Manage security and notification preferences</p>
      </div>

      <Card>
        <CardHeader>
          <CardTitle>Security</CardTitle>
          <CardDescription>Change your password regularly to keep your account safe</CardDescription>
        </CardHeader>
        <CardContent>
          <form onSubmit={handlePasswordSubmit} className="space-y-4">
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              <div className="space-y-2">
                <Label htmlFor="currentPassword">Current Password</Label>
                <Input
                  id="currentPassword"
                  type="password"
                  value={passwordForm.currentPassword}
                  onChange={(e) => handlePasswordChange("currentPassword", e.target.value)}
                  required
                />
              </div>
              <div className="space-y-2">
                <Label htmlFor="newPassword">New Password</Label>
                <Input
                  id="newPassword"
                  type="password"
                  value={passwordForm.newPassword}
                  onChange={(e) => handlePasswordChange("newPassword", e.target.value)}
                  minLength={6}
                  required
                />
              </div>
              <div className="space-y-2">
                <Label htmlFor="confirmPassword">Confirm New Password</Label>
                <Input
                  id="confirmPassword"
                  type="password"
                  value={passwordForm.confirmPassword}
                  onChange={(e) => handlePasswordChange("confirmPassword", e.target.value)}
                  minLength={6}
                  required
                />
              </div>
            </div>
            <div className="flex justify-end">
              <Button type="submit" disabled={saving}>
                {saving ? "Saving..." : "Update Password"}
              </Button>
            </div>
          </form>
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle>Notifications</CardTitle>
          <CardDescription>Choose how you want to be notified about appointments</CardDescription>
        </CardHeader>
        <CardContent className="space-y-4">
          <div className="flex items-center justify-between rounded-lg border p-3">
            <div>
              <p className="font-medium">Email notifications</p>
              <p className="text-sm text-muted-foreground">Appointment confirmations and reminders</p>
            </div>
            <Switch
              checked={notifications.email}
              onCheckedChange={(checked) => setNotifications((p) => ({ ...p, email: checked }))}
            />
          </div>
          <div className="flex items-center justify-between rounded-lg border p-3">
            <div>
              <p className="font-medium">SMS notifications</p>
              <p className="text-sm text-muted-foreground">Quick alerts and cancellations</p>
            </div>
            <Switch
              checked={notifications.sms}
              onCheckedChange={(checked) => setNotifications((p) => ({ ...p, sms: checked }))}
            />
          </div>
          <div className="flex items-center justify-between rounded-lg border p-3">
            <div>
              <p className="font-medium">Consultation reminders</p>
              <p className="text-sm text-muted-foreground">Get notified 15 minutes before your call</p>
            </div>
            <Switch
              checked={notifications.reminders}
              onCheckedChange={(checked) => setNotifications((p) => ({ ...p, reminders: checked }))}
            />
          </div>
          <div className="flex justify-end">
            <Button type="button" onClick={handleNotificationSave}>
              Save Preferences
            </Button>
          </div>
        </CardContent>
      </Card>
    </div>
  )
}
