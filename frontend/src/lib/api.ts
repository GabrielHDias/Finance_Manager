import { ApiError } from "@/lib/api-error";
import {
  getAccessToken,
  removeAccessToken,
} from "@/lib/token-storage";
import type { ApiErrorResponse } from "@/types/api-error";

const API_URL = process.env.NEXT_PUBLIC_API_URL;

export async function apiRequest<T>(
  path: string,
  options?: RequestInit,
  authenticated = true,
): Promise<T> {
  if (!API_URL) {
    throw new Error("NEXT_PUBLIC_API_URL não está configurada.");
  }

  const headers = new Headers(options?.headers);

  if (authenticated) {
    const token = getAccessToken();

    if (token) {
      headers.set("Authorization", `Bearer ${token}`);
    }
  }

  const response = await fetch(`${API_URL}${path}`, {
    ...options,
    headers,
  });

  if (authenticated && response.status === 401) {
    removeAccessToken();
  }

  if (!response.ok) {
    const errorResponse: ApiErrorResponse = await response.json();

    throw new ApiError(errorResponse);
  }

  const data: T = await response.json();

  return data;
}