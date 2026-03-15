const TOKEN_COOKIE_KEY = "token"

export function normalizeToken(rawToken?: string | null): string {
  if (!rawToken) {
    return ""
  }

  return rawToken
    .replace(/^Bearer\s+/i, "")
    .trim()
    .replace(/^"+|"+$/g, "")
}

function getTokenFromCookie(): string {
  if (typeof document === "undefined") {
    return ""
  }

  const match = document.cookie.match(
    new RegExp(`(?:^|; )${TOKEN_COOKIE_KEY}=([^;]+)`),
  )
  if (!match) {
    return ""
  }

  try {
    return normalizeToken(decodeURIComponent(match[1]))
  } catch {
    return normalizeToken(match[1])
  }
}

export function getAuthToken(): string {
  if (typeof window === "undefined") {
    return ""
  }

  const localToken = normalizeToken(localStorage.getItem(TOKEN_COOKIE_KEY))
  if (localToken) {
    return localToken
  }

  const cookieToken = getTokenFromCookie()
  if (cookieToken) {
    localStorage.setItem(TOKEN_COOKIE_KEY, cookieToken)
  }

  return cookieToken
}

export function clearAuthSession(): void {
  if (typeof window === "undefined") {
    return
  }

  localStorage.removeItem("token")
  localStorage.removeItem("user")
  localStorage.removeItem("userRole")
  document.cookie = `${TOKEN_COOKIE_KEY}=; path=/; expires=Thu, 01 Jan 1970 00:00:00 GMT; SameSite=Strict`
}
