export interface DashboardSummaryResponse {
  totalIncome: number;
  totalExpense: number;
  balance: number;
}

export interface ExpenseByCategoryResponse {
  categoryId: number;
  categoryName: string;
  amount: number;
}
export interface DashboardPeriod {
  startDate?: string;
  endDate?: string;
}
