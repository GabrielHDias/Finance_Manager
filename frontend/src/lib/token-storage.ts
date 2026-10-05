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

  return sessionStorage.getItem(ACCESS_TOKEN_KEY);
}

export function removeAccessToken(): void {
  if (typeof window === "undefined") {
    return;
  }

  sessionStorage.removeItem(ACCESS_TOKEN_KEY);
}