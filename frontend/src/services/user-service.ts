import { apiRequest } from "@/lib/api";
import type {
  CreateUserRequest,
  UserResponse,
} from "@/types/user";

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