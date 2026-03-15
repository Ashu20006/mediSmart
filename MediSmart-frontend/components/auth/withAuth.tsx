"use client";
import API_BASE_URL from "@/config/api";
import { clearAuthSession, getAuthToken } from "@/lib/auth";
import { useEffect } from "react";
import { useRouter } from "next/navigation";

export default function withAuth(Component: React.ComponentType) {
  return function ProtectedRoute(props: any) {
    const router = useRouter();

    useEffect(() => {
      const token = getAuthToken();
      if (!token) {
        router.push("/login");
        return;
      }

      (async () => {
        try {
          const res = await fetch(`${API_BASE_URL}/api/auth/me`, {
            headers: { Authorization: `Bearer ${token}` },
            credentials: "include",
          });

          if (res.ok) {
            const user = await res.json();
            localStorage.setItem("user", JSON.stringify(user));
            if (user?.role) {
              localStorage.setItem("userRole", String(user.role));
            }
            return;
          }

          if (res.status === 401 || res.status === 403) {
            clearAuthSession();
            router.push("/login");
          }
        } catch {
          // ignore network/transient errors
        }
      })();
    }, [router]);

    return <Component {...props} />;
  };
}
