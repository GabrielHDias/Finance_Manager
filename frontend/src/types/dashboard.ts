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

export type ChartType = "PIE" | "LINE" | "BAR";
export type ChartGrouping = "DAY" | "MONTH" | "CATEGORY";
export type FinancialType = "INCOME" | "EXPENSE";
export interface ChartFilters {
  type: FinancialType;
  groupBy: ChartGrouping;
  startDate: string;
  endDate: string;
}
export interface TransactionChartResponse {
  type: FinancialType;
  groupBy: ChartGrouping;
  total: string;
  items: { key: string; label: string; total: string }[];
}
