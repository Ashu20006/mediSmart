"use client"

import API_BASE_URL from "@/config/api"
import { useEffect, useRef, useState, useCallback } from "react"
import { useRouter } from "next/navigation"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Badge } from "@/components/ui/badge"
import { Separator } from "@/components/ui/separator"
import { useToast } from "@/hooks/use-toast"

interface ConsultationSessionPayload {
  appId: string
  token: string
  channelName: string
  uid: number
}

interface CurrentUser {
  id?: string
  name?: string
}

interface ChatMessage {
  senderId?: string
  senderName?: string
  message: string
  timestamp?: string
  appointmentId: string
}

interface ConsultationRoomProps {
  appointmentId: string
  currentUser: CurrentUser
  mode?: "page" | "dialog"
  open?: boolean
  onOpenChange?: (open: boolean) => void
}

// ─── Module-level singleton guard ────────────────────────────────────────────
// Using a module-level variable is the ONLY reliable way to survive React
// StrictMode double-mount, because refs and window flags are reset between
// the unmount and remount that StrictMode performs in development.
let _activeConsultationId: string | null = null

export function ConsultationRoom({
  appointmentId,
  currentUser,
  mode = "page",
  open = true,
  onOpenChange,
}: ConsultationRoomProps) {
  const { toast } = useToast()
  const router = useRouter()

  const [session, setSession] = useState<ConsultationSessionPayload | null>(null)
  const [isConnecting, setIsConnecting] = useState(false)
  const [isConnected, setIsConnected] = useState(false)
  const [chatConnected, setChatConnected] = useState(false)
  const [remoteConnected, setRemoteConnected] = useState(false)
  const [messages, setMessages] = useState<ChatMessage[]>([])
  const [chatInput, setChatInput] = useState("")

  const agoraClientRef = useRef<any>(null)
  const localTracksRef = useRef<any[]>([])
  const stompClientRef = useRef<any>(null)
  const stompSubscriptionRef = useRef<any>(null)
  const localUidRef = useRef<number | null>(null)

  // ─── cleanupRoom ────────────────────────────────────────────────────────────
  // Defined with useCallback so it has a stable reference and can safely be
  // called both from inside the effect and from the UI.
  const cleanupRoom = useCallback(async () => {
    // Clear the singleton so a future mount can join again
    _activeConsultationId = null

    try {
      if (stompSubscriptionRef.current) {
        stompSubscriptionRef.current.unsubscribe()
        stompSubscriptionRef.current = null
      }
      if (stompClientRef.current) {
        stompClientRef.current.disconnect(() => {})
        stompClientRef.current = null
      }

      for (const track of localTracksRef.current) {
        try {
          track.stop()
          track.close()
        } catch {
          /* ignore */
        }
      }
      localTracksRef.current = []

      if (agoraClientRef.current) {
        try {
          await agoraClientRef.current.leave()
        } catch {
          /* ignore if already left */
        }
        agoraClientRef.current = null
      }
    } finally {
      localUidRef.current = null
      setIsConnected(false)
      setChatConnected(false)
      setIsConnecting(false)
      setMessages([])
      setChatInput("")
      setRemoteConnected(false)
    }
  }, [])

  // ─── Main effect ────────────────────────────────────────────────────────────
  useEffect(() => {
    const isActive = mode === "page" ? Boolean(appointmentId) : open
    if (!isActive || !appointmentId) return

    // ── Singleton guard ──────────────────────────────────────────────────────
    // If another instance (StrictMode double-mount or re-render) already claimed
    // this appointment, do nothing.
    if (_activeConsultationId === appointmentId) return
    _activeConsultationId = appointmentId

    let cancelled = false

    // ── connectChat ──────────────────────────────────────────────────────────
    const connectChat = (apptId: string, SockJSClass: any, StompClass: any) => {
      const token = localStorage.getItem("token") || ""
      if (!token) return

      const socket = new SockJSClass(`${API_BASE_URL}/ws`)
      const stompClient = StompClass.over(socket)
      stompClient.debug = () => {}
      stompClientRef.current = stompClient

      stompClient.connect(
        { Authorization: `Bearer ${token}` },
        () => {
          if (cancelled) return
          setChatConnected(true)
          stompSubscriptionRef.current = stompClient.subscribe(
            `/topic/consultation/${apptId}`,
            (message: any) => {
              try {
                const parsed: ChatMessage = JSON.parse(message.body)
                setMessages((prev) => [...prev, parsed])
              } catch {
                // ignore malformed messages
              }
            }
          )
        },
        () => {
          if (!cancelled) setChatConnected(false)
        }
      )
    }

    // ── connectRoom ──────────────────────────────────────────────────────────
    const connectRoom = async () => {
      setIsConnecting(true)
      setMessages([])
      setRemoteConnected(false)

      try {
        const token = localStorage.getItem("token") || ""
        if (!token) throw new Error("Authentication required")

        // 1) Validate join & get channel info
        const joinRes = await fetch(`${API_BASE_URL}/api/consultation/${appointmentId}/join`, {
          headers: { Authorization: `Bearer ${token}` },
        })
        if (!joinRes.ok) {
          const msg = await joinRes.text()
          throw new Error(msg || "Unable to join consultation")
        }

        // 2) Fetch Agora token
        const tokenRes = await fetch(`${API_BASE_URL}/api/consultation/${appointmentId}/token`, {
          headers: { Authorization: `Bearer ${token}` },
        })
        if (!tokenRes.ok) {
          const msg = await tokenRes.text()
          throw new Error(msg || "Unable to generate token")
        }
        const tokenData = await tokenRes.json()
        const payload: ConsultationSessionPayload = {
          appId: tokenData.appId,
          token: tokenData.token,
          channelName: tokenData.channelName,
          uid: tokenData.uid,
        }

        if (cancelled) return   // ← bail if effect was already torn down

        setSession(payload)
        localUidRef.current = payload.uid

        // 3) Load SDKs
        const [{ default: AgoraRTC }, { default: SockJS }, stompModule] = await Promise.all([
          import("agora-rtc-sdk-ng"),
          import("sockjs-client"),
          import("stompjs/lib/stomp.js"),
        ])
        const Stomp = (stompModule as any).Stomp

        if (cancelled) return   // ← bail after async imports

        // 4) Create ONE Agora client
        const client = AgoraRTC.createClient({ mode: "rtc", codec: "vp8" })
        agoraClientRef.current = client

        // 5) Wire up remote-user events
        //    Always skip events whose uid matches our own uid to avoid
        //    displaying our own stream in the remote container.
        client.on("user-published", async (user: any, mediaType: "video" | "audio") => {
          if (user.uid === localUidRef.current) return   // ← self-filter

          await client.subscribe(user, mediaType)

          if (mediaType === "video") {
            user.videoTrack?.stop()
            user.videoTrack?.play("remote-video-container")
            setRemoteConnected(true)
          }
          if (mediaType === "audio") {
            user.audioTrack?.play()
            setRemoteConnected(true)
          }
        })

        client.on("user-joined", (user: any) => {
          if (user.uid === localUidRef.current) return   // ← self-filter
          setRemoteConnected(true)
        })

        client.on("user-unpublished", (user: any) => {
          if (user.uid === localUidRef.current) return   // ← self-filter
          const remoteContainer = document.getElementById("remote-video-container")
          if (remoteContainer) remoteContainer.innerHTML = ""
          setRemoteConnected(false)
        })

        client.on("user-left", (user: any) => {
          if (user.uid === localUidRef.current) return   // ← self-filter
          const remoteContainer = document.getElementById("remote-video-container")
          if (remoteContainer) remoteContainer.innerHTML = ""
          setRemoteConnected(false)
        })

        // 6) Join channel
        await client.join(payload.appId, payload.channelName, payload.token, payload.uid)

        if (cancelled) {
          // We joined but the component unmounted — leave immediately
          await client.leave()
          agoraClientRef.current = null
          return
        }

        // 7) Publish local tracks
        const tracks = await AgoraRTC.createMicrophoneAndCameraTracks()
        localTracksRef.current = tracks

        if (cancelled) {
          tracks.forEach((t: any) => { try { t.stop(); t.close() } catch {} })
          localTracksRef.current = []
          await client.leave()
          agoraClientRef.current = null
          return
        }

        await client.publish(tracks)
        tracks[1]?.play("local-video-container")   // index 1 = video track

        setIsConnected(true)
        connectChat(appointmentId, SockJS, Stomp)
      } catch (error: any) {
        console.error("Consultation connection failed:", error)
        // Release the singleton so the user can retry
        _activeConsultationId = null
        if (!cancelled) {
          toast({
            title: "Consultation failed",
            description: error?.message || "Unable to start consultation.",
            variant: "destructive",
          })
          if (onOpenChange) onOpenChange(false)
        }
      } finally {
        if (!cancelled) setIsConnecting(false)
      }
    }

    void connectRoom()

    // ── Cleanup ──────────────────────────────────────────────────────────────
    return () => {
      cancelled = true
      void cleanupRoom()
    }
  }, [appointmentId, open, mode, toast, onOpenChange, cleanupRoom])

  // ─── UI Handlers ────────────────────────────────────────────────────────────
  const handleEndCall = () => {
    void cleanupRoom()
    if (onOpenChange) onOpenChange(false)
    if (mode === "page") router.back()
  }

  const handleSendMessage = () => {
    if (!session || !stompClientRef.current || !chatConnected) return
    const text = chatInput.trim()
    if (!text) return

    const payload: ChatMessage = {
      senderId: currentUser.id,
      senderName: currentUser.name || "User",
      message: text,
      appointmentId,
    }

    stompClientRef.current.send(
      `/app/consultation/${appointmentId}`,
      {},
      JSON.stringify(payload)
    )
    setChatInput("")
  }

  // ─── Render ─────────────────────────────────────────────────────────────────
  const header = (
    <div className="flex items-center gap-2 mb-3">
      <h2 className="text-xl font-semibold flex-1">Online Consultation</h2>
      <Badge variant={isConnected ? "default" : "secondary"}>
        {isConnecting ? "Connecting..." : isConnected ? "Video Connected" : "Disconnected"}
      </Badge>
      <Badge variant={chatConnected ? "default" : "secondary"}>
        {chatConnected ? "Chat Connected" : "Chat Offline"}
      </Badge>
    </div>
  )

  const body = (
    <div className="grid grid-cols-1 lg:grid-cols-3 gap-4">
      <div className="lg:col-span-2">
        <div className="relative w-full h-[420px] rounded-md overflow-hidden bg-black">
          {/* Remote video fills the main area */}
          <div id="remote-video-container" className="w-full h-full" />

          {/* "Waiting" overlay — only shown when WE are connected but nobody else is */}
          {!remoteConnected && isConnected && (
            <div className="absolute inset-0 flex items-center justify-center text-white/80 text-lg bg-black/50">
              Waiting for other participant...
            </div>
          )}

          {/* Local video PiP */}
          <div
            id="local-video-container"
            className="absolute bottom-3 right-3 w-44 h-28 bg-black/80 rounded border border-white/20 overflow-hidden"
          />

          <Button
            size="sm"
            variant="secondary"
            className="absolute top-3 right-3"
            onClick={handleEndCall}
          >
            End Call
          </Button>
        </div>
      </div>

      <div className="flex flex-col border rounded-md">
        <div className="p-3 font-medium">Consultation Chat</div>
        <Separator />
        <div className="flex-1 p-3 overflow-y-auto h-[360px] space-y-2">
          {messages.length === 0 && (
            <p className="text-sm text-muted-foreground">No messages yet.</p>
          )}
          {messages.map((msg, idx) => (
            <div key={`${msg.timestamp || ""}-${idx}`} className="text-sm">
              <span className="font-medium">{msg.senderName || "User"}: </span>
              <span>{msg.message}</span>
            </div>
          ))}
        </div>
        <Separator />
        <div className="p-3 flex gap-2">
          <Input
            placeholder="Type a message..."
            value={chatInput}
            onChange={(e) => setChatInput(e.target.value)}
            onKeyDown={(e) => {
              if (e.key === "Enter") {
                e.preventDefault()
                handleSendMessage()
              }
            }}
          />
          <Button onClick={handleSendMessage} disabled={!chatConnected}>
            Send
          </Button>
        </div>
      </div>
    </div>
  )

  if (mode === "dialog") {
    return <></>
  }

  return (
    <div className="max-w-6xl w-[96vw] mx-auto py-6">
      {header}
      {body}
    </div>
  )
}