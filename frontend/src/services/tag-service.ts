import { apiRequest } from "@/lib/api";
import type { TagResponse } from "@/types/tag";

export async function findAllTags(): Promise<TagResponse[]> {
  return apiRequest<TagResponse[]>("/tags");
}

export async function createTag(name: string): Promise<TagResponse> {
  return apiRequest<TagResponse>("/tags", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ name }),
  });
}

export async function renameTag(id: number, name: string): Promise<TagResponse> {
  return apiRequest<TagResponse>(`/tags/${id}`, {
    method: "PUT",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ name }),
  });
}

export async function deleteTag(id: number): Promise<void> {
  return apiRequest<void>(`/tags/${id}`, { method: "DELETE" });
}
