import { apiRequest } from "@/lib/api";
import type { CategoryResponse } from "@/types/category";

export async function findAllCategories(): Promise<CategoryResponse[]> {
  return apiRequest<CategoryResponse[]>("/categories");
}