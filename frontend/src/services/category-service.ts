import { apiRequest } from "@/lib/api";
import type {
  CategoryResponse,
  CreateCategoryRequest,
} from "@/types/category";

export async function findAllCategories(): Promise<CategoryResponse[]> {
  return apiRequest<CategoryResponse[]>("/categories");
}

export async function createCategory(
  category: CreateCategoryRequest,
): Promise<CategoryResponse> {
  return apiRequest<CategoryResponse>("/categories", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    body: JSON.stringify(category),
  });
}

export async function renameCategory(
  id: number,
  request: CreateCategoryRequest,
): Promise<CategoryResponse> {
  return apiRequest<CategoryResponse>(`/categories/${id}`, {
    method: "PUT",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(request),
  });
}

export async function deleteCategory(id: number): Promise<void> {
  return apiRequest<void>(`/categories/${id}`, { method: "DELETE" });
}
