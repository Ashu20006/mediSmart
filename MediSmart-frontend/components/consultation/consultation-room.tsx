"use client"

import API_BASE_URL from "@/config/api"
import { useEffect, useRef, useState, useCallback } from "react"
import { useRouter } from "next/navigation"
import { Input } from "@/components/ui/input"
import { Button } from "@/components/ui/button"
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
let _activeConsultationId: string | null = null

// ─── SVG Icons ───────────────────────────────────────────────────────────────
function MicIcon() {
  return (
    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="white" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <path d="M12 1a3 3 0 0 0-3 3v8a3 3 0 0 0 6 0V4a3 3 0 0 0-3-3z" />
      <path d="M19 10v2a7 7 0 0 1-14 0v-2" />
      <line x1="12" y1="19" x2="12" y2="23" />
      <line x1="8" y1="23" x2="16" y2="23" />
    </svg>
  )
}

function MicOffIcon() {
  return (
    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="white" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <line x1="1" y1="1" x2="23" y2="23" />
      <path d="M9 9v3a3 3 0 0 0 5.12 2.12M15 9.34V4a3 3 0 0 0-5.94-.6" />
      <path d="M17 16.95A7 7 0 0 1 5 12v-2m14 0v2a7 7 0 0 1-.11 1.23" />
      <line x1="12" y1="19" x2="12" y2="23" />
      <line x1="8" y1="23" x2="16" y2="23" />
    </svg>
  )
}

function CameraIcon() {
  return (
    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="white" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <polygon points="23 7 16 12 23 17 23 7" />
      <rect x="1" y="5" width="15" height="14" rx="2" ry="2" />
    </svg>
  )
}

function CameraOffIcon() {
  return (
    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="white" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <line x1="1" y1="1" x2="23" y2="23" />
      <path d="M21 21H3a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h3m3-3h6l2 3h3a2 2 0 0 1 2 2v9.34" />
      <path d="M16 11.37A4 4 0 1 1 12.63 8" />
    </svg>
  )
}

function PhoneOffIcon() {
  return (
    <svg width="20" height="20" viewBox="0 0 24 24" fill="white">
      <path d="M6.6 10.8c1.4 2.8 3.8 5.1 6.6 6.6l2.2-2.2c.3-.3.7-.4 1-.2 1.1.4 2.3.6 3.6.6.6 0 1 .4 1 1V20c0 .6-.4 1-1 1-9.4 0-17-7.6-17-17 0-.6.4-1 1-1h3.5c.6 0 1 .4 1 1 0 1.3.2 2.5.6 3.6.1.3 0 .7-.2 1L6.6 10.8z" />
    </svg>
  )
}

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
  const [isMicMuted, setIsMicMuted] = useState(false)
  const [isCamOff, setIsCamOff] = useState(false)

  const agoraClientRef = useRef<any>(null)
  const localTracksRef = useRef<any[]>([])
  const stompClientRef = useRef<any>(null)
  const stompSubscriptionRef = useRef<any>(null)
  const localUidRef = useRef<number | null>(null)

  // ─── cleanupRoom ─────────────────────────────────────────────────────────────
  const cleanupRoom = useCallback(async () => {
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
      setIsMicMuted(false)
      setIsCamOff(false)
    }
  }, [])

  // ─── Main effect ──────────────────────────────────────────────────────────────
  useEffect(() => {
    const isActive = mode === "page" ? Boolean(appointmentId) : open
    if (!isActive || !appointmentId) return

    if (_activeConsultationId === appointmentId) return
    _activeConsultationId = appointmentId

    let cancelled = false

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
                /* ignore malformed messages */
              }
            }
          )
        },
        () => {
          if (!cancelled) setChatConnected(false)
        }
      )
    }

    const connectRoom = async () => {
      setIsConnecting(true)
      setMessages([])
      setRemoteConnected(false)

      try {
        const token = localStorage.getItem("token") || ""
        if (!token) throw new Error("Authentication required")

        const joinRes = await fetch(`${API_BASE_URL}/api/consultation/${appointmentId}/join`, {
          headers: { Authorization: `Bearer ${token}` },
        })
        if (!joinRes.ok) {
          const msg = await joinRes.text()
          throw new Error(msg || "Unable to join consultation")
        }

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

        if (cancelled) return

        setSession(payload)
        localUidRef.current = payload.uid

        const [{ default: AgoraRTC }, { default: SockJS }, stompModule] = await Promise.all([
          import("agora-rtc-sdk-ng"),
          import("sockjs-client"),
          import("stompjs/lib/stomp.js"),
        ])
        const Stomp = (stompModule as any).Stomp

        if (cancelled) return

        const client = AgoraRTC.createClient({ mode: "rtc", codec: "vp8" })
        agoraClientRef.current = client

        client.on("user-published", async (user: any, mediaType: "video" | "audio") => {
          if (user.uid === localUidRef.current) return
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
          if (user.uid === localUidRef.current) return
          setRemoteConnected(true)
        })

        client.on("user-unpublished", (user: any) => {
          if (user.uid === localUidRef.current) return
          const remoteContainer = document.getElementById("remote-video-container")
          if (remoteContainer) remoteContainer.innerHTML = ""
          setRemoteConnected(false)
        })

        client.on("user-left", (user: any) => {
          if (user.uid === localUidRef.current) return
          const remoteContainer = document.getElementById("remote-video-container")
          if (remoteContainer) remoteContainer.innerHTML = ""
          setRemoteConnected(false)
        })

        await client.join(payload.appId, payload.channelName, payload.token, payload.uid)

        if (cancelled) {
          await client.leave()
          agoraClientRef.current = null
          return
        }

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
        tracks[1]?.play("local-video-container")

        setIsConnected(true)
        connectChat(appointmentId, SockJS, Stomp)
      } catch (error: any) {
        console.error("Consultation connection failed:", error)
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

    return () => {
      cancelled = true
      void cleanupRoom()
    }
  }, [appointmentId, open, mode, toast, onOpenChange, cleanupRoom])

  // ─── UI Handlers ──────────────────────────────────────────────────────────────
  const handleEndCall = () => {
    void cleanupRoom()
    if (onOpenChange) onOpenChange(false)
    if (mode === "page") router.back()
  }

  const handleToggleMic = () => {
    const audioTrack = localTracksRef.current[0]
    if (!audioTrack) return
    const next = !isMicMuted
    audioTrack.setMuted(next)
    setIsMicMuted(next)
  }

  const handleToggleCam = () => {
    const videoTrack = localTracksRef.current[1]
    if (!videoTrack) return
    const next = !isCamOff
    videoTrack.setMuted(next)
    setIsCamOff(next)
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

  // ─── Render ───────────────────────────────────────────────────────────────────
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
        <div className="relative w-full h-[480px] rounded-xl overflow-hidden bg-black">

          {/* Remote video — fills full area */}
          <div id="remote-video-container" className="w-full h-full" />

          {/* Waiting overlay */}
          {!remoteConnected && isConnected && (
            <div className="absolute inset-0 flex items-center justify-center text-white/70 text-base bg-black/50 pointer-events-none select-none">
              Waiting for other participant...
            </div>
          )}

          {/* Connecting overlay */}
          {isConnecting && (
            <div className="absolute inset-0 flex items-center justify-center text-white/70 text-base bg-black/70 pointer-events-none select-none">
              Connecting...
            </div>
          )}

          {/* Local PiP — sits above control bar */}
          <div
            id="local-video-container"
            className="absolute bottom-20 right-3 w-36 h-24 bg-black/80 rounded-lg border border-white/20 overflow-hidden shadow-lg"
          />

          {/* ── Control bar ── */}
          <div className="absolute bottom-0 left-0 right-0 h-16 bg-black/70 flex items-center justify-center gap-5 px-6">

            {/* Mic toggle */}
            <div className="flex flex-col items-center gap-1">
              <button
                onClick={handleToggleMic}
                disabled={!isConnected}
                title={isMicMuted ? "Unmute microphone" : "Mute microphone"}
                className={[
                  "w-11 h-11 rounded-full border flex items-center justify-center transition-all duration-150",
                  "disabled:opacity-30 disabled:cursor-not-allowed",
                  isMicMuted
                    ? "bg-white/10 border-white/15 opacity-60"
                    : "bg-white/10 border-white/25 hover:bg-white/20 active:scale-95",
                ].join(" ")}
              >
                {isMicMuted ? <MicOffIcon /> : <MicIcon />}
              </button>
              <span className="text-[10px] text-white/40 leading-none">
                {isMicMuted ? "Unmute" : "Mute"}
              </span>
            </div>

            {/* Camera toggle */}
            <div className="flex flex-col items-center gap-1">
              <button
                onClick={handleToggleCam}
                disabled={!isConnected}
                title={isCamOff ? "Turn camera on" : "Turn camera off"}
                className={[
                  "w-11 h-11 rounded-full border flex items-center justify-center transition-all duration-150",
                  "disabled:opacity-30 disabled:cursor-not-allowed",
                  isCamOff
                    ? "bg-white/10 border-white/15 opacity-60"
                    : "bg-white/10 border-white/25 hover:bg-white/20 active:scale-95",
                ].join(" ")}
              >
                {isCamOff ? <CameraOffIcon /> : <CameraIcon />}
              </button>
              <span className="text-[10px] text-white/40 leading-none">
                {isCamOff ? "Cam off" : "Camera"}
              </span>
            </div>

            {/* End call — larger red circle */}
            <div className="flex flex-col items-center gap-1">
              <button
                onClick={handleEndCall}
                title="End call"
                className="w-[52px] h-[52px] rounded-full bg-red-600 hover:bg-red-700 active:scale-95 border-0 flex items-center justify-center transition-all duration-150"
              >
                <PhoneOffIcon />
              </button>
              <span className="text-[10px] text-white/40 leading-none">End call</span>
            </div>

          </div>
        </div>
      </div>

      {/* ── Chat panel ── */}
      <div className="flex flex-col border rounded-xl overflow-hidden">
        <div className="p-3 font-medium text-sm">Consultation Chat</div>
        <Separator />
        <div className="flex-1 p-3 overflow-y-auto h-[408px] space-y-3">
          {messages.length === 0 && (
            <p className="text-sm text-muted-foreground">No messages yet.</p>
          )}
          {messages.map((msg, idx) => {
            const isOwn = msg.senderId === currentUser.id
            return (
              <div
                key={`${msg.timestamp || ""}-${idx}`}
                className={`flex flex-col gap-0.5 ${isOwn ? "items-end" : "items-start"}`}
              >
                <span className="text-[11px] text-muted-foreground px-1">
                  {msg.senderName || "User"}
                </span>
                <div
                  className={[
                    "text-sm px-3 py-1.5 rounded-2xl max-w-[85%] break-words",
                    isOwn
                      ? "bg-primary text-primary-foreground rounded-br-sm"
                      : "bg-muted text-foreground rounded-bl-sm",
                  ].join(" ")}
                >
                  {msg.message}
                </div>
              </div>
            )
          })}
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
          <Button onClick={handleSendMessage} disabled={!chatConnected} size="sm">
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