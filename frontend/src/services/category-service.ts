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