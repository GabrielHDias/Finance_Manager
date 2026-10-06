import { apiRequest } from "@/lib/api";
import type {
  DashboardSummaryResponse,
  ExpenseByCategoryResponse,
} from "@/types/dashboard";

export async function getDashboardSummary(): Promise<DashboardSummaryResponse> {
  return apiRequest<DashboardSummaryResponse>("/dashboard/summary");
}

export async function getExpensesByCategory(): Promise<
  ExpenseByCategoryResponse[]
> {
  return apiRequest<ExpenseByCategoryResponse[]>(
    "/dashboard/expenses-by-category",
  );
}