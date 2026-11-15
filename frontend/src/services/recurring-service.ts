import { apiRequest } from "@/lib/api";

export interface RecurringRule {
  id: number;
  description: string;
  amount: string;
  type: "INCOME" | "EXPENSE";
  frequency: "MONTHLY" | "WEEKLY";
  startDate: string;
  endDate: string | null;
  accountId: number;
  accountName: string;
  categoryId: number;
  categoryName: string;
}

export async function findRecurringRules(): Promise<RecurringRule[]> {
  return apiRequest<RecurringRule[]>("/recurring-rules");
}

export async function createRecurringRule(rule: {
  description: string; amount: string; type: "INCOME" | "EXPENSE"; frequency: "MONTHLY" | "WEEKLY";
  startDate: string; accountId: number; categoryId: number; endDate?: string;
}): Promise<RecurringRule> {
  return apiRequest<RecurringRule>("/recurring-rules", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(rule),
  });
}

export async function deleteRecurringRule(id: number): Promise<void> {
  return apiRequest<void>(`/recurring-rules/${id}`, { method: "DELETE" });
}
