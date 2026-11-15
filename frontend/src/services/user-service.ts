import { apiRequest } from "@/lib/api";
import type {
  CreateUserRequest,
  UserResponse,
} from "@/types/user";

export async function findCurrentUser(): Promise<UserResponse> {
  return apiRequest<UserResponse>("/users/me");
}

export async function changeEmail(email: string, currentPassword: string): Promise<UserResponse> {
  return apiRequest<UserResponse>("/users/me/email", {
    method: "PUT",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ email, currentPassword }),
  });
}

export async function deleteOwnAccount(currentPassword: string): Promise<void> {
  return apiRequest<void>("/users/me", {
    method: "DELETE",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ currentPassword }),
  });
}

export async function createUser(
  user: CreateUserRequest,
): Promise<UserResponse> {
  return apiRequest<UserResponse>(
    "/users",
    {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify(user),
    },
    false,
  );
}