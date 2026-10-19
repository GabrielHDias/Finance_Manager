import { apiRequest } from "@/lib/api";
import type {
  DashboardPeriod,
  DashboardSummaryResponse,
  ExpenseByCategoryResponse,
} from "@/types/dashboard";

function periodQuery(period: DashboardPeriod): string {
  const query = new URLSearchParams();
  if (period.startDate) query.set("startDate", period.startDate);
  if (period.endDate) query.set("endDate", period.endDate);
  return query.size ? `?${query}` : "";
}

export async function getDashboardSummary(
  period: DashboardPeriod = {},
): Promise<DashboardSummaryResponse> {
  return apiRequest<DashboardSummaryResponse>(`/dashboard/summary${periodQuery(period)}`);
}

export async function getExpensesByCategory(
  period: DashboardPeriod = {},
): Promise<ExpenseByCategoryResponse[]> {
  return apiRequest<ExpenseByCategoryResponse[]>(`/dashboard/expenses-by-category${periodQuery(period)}`);
}
