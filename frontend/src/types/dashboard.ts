export interface DashboardSummaryResponse {
  totalIncome: string;
  totalExpense: string;
  balance: string;
}

export interface ExpenseByCategoryResponse {
  categoryId: number;
  categoryName: string;
  amount: string;
}
export interface DashboardPeriod {
  startDate?: string;
  endDate?: string;
}
