const ACCESS_TOKEN_KEY = "accessToken";

export function saveAccessToken(token: string): void {
  if (typeof window === "undefined") {
    return;
  }

  sessionStorage.setItem(ACCESS_TOKEN_KEY, token);
}

export function getAccessToken(): string | null {
  if (typeof window === "undefined") {
    return null;
  }

  const token = sessionStorage.getItem(ACCESS_TOKEN_KEY);
  if (!token) return null;
  try {
    const payload = JSON.parse(atob(token.split(".")[1].replace(/-/g, "+").replace(/_/g, "/")));
    if (typeof payload.exp !== "number" || payload.exp * 1000 <= Date.now()) return null;
    return token;
  } catch { return null; }
}

export function removeAccessToken(): void {
  if (typeof window === "undefined") {
    return;
  }

  sessionStorage.removeItem(ACCESS_TOKEN_KEY);
}