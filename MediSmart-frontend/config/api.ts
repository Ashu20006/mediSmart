
// Reads public API base URL from env so Vercel can inject the correct backend endpoint
const API_BASE_URL =
  process.env.NEXT_PUBLIC_API_BASE_URL || "https://medismart.onrender.com";

export default API_BASE_URL;
