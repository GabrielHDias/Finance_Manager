import { apiRequest } from "@/lib/api";
import type {
  DashboardPeriod,
  DashboardSummaryResponse,
  ExpenseByCategoryResponse,
  ChartFilters,
  TransactionChartResponse,
} from "@/types/dashboard";

function periodQuery(period: DashboardPeriod): string {
  const query = new URLSearchParams();
  if (period.startDate) query.set("startDate", period.startDate);
  if (period.endDate) query.set("endDate", period.endDate);
  if (period.tagId) query.set("tagId", String(period.tagId));
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

export async function getTransactionsSummary(
  filters: ChartFilters,
  signal?: AbortSignal,
): Promise<TransactionChartResponse> {
  const query = new URLSearchParams({
    type: filters.type,
    groupBy: filters.groupBy,
    startDate: filters.startDate,
    endDate: filters.endDate,
  });
  if (filters.tagId) query.set("tagId", String(filters.tagId));
  return apiRequest(`/dashboard/transactions-summary?${query}`, { signal });
}
