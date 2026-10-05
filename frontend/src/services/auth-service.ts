import { apiRequest } from "@/lib/api";
import type { LoginRequest, LoginResponse } from "@/types/auth";

export async function login(
  credentials: LoginRequest,
): Promise<LoginResponse> {
  return apiRequest<LoginResponse>("/auth/login", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    body: JSON.stringify(credentials),
  });
}