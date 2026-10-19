import { apiRequest } from "@/lib/api";
import type { BudgetPeriod, BudgetSummary, SaveBudgetRequest } from "@/types/budget";

export function findBudgets(period: BudgetPeriod): Promise<BudgetSummary[]> {
  return apiRequest<BudgetSummary[]>(`/budgets?year=${period.year}&month=${period.month}`);
}

export function saveBudget(request: SaveBudgetRequest): Promise<BudgetSummary> {
  return apiRequest<BudgetSummary>("/budgets", {
    method: "PUT", headers: { "Content-Type": "application/json" }, body: JSON.stringify(request),
  });
}

export function deleteBudget(id: number): Promise<void> {
  return apiRequest<void>(`/budgets/${id}`, { method: "DELETE" });
}
