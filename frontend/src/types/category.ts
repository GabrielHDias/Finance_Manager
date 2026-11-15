export type CategoryType = "INCOME" | "EXPENSE";

export interface CreateCategoryRequest {
  name: string;
  type?: CategoryType;
}

export interface CategoryResponse {
  id: number;
  name: string;
  type: CategoryType;
}