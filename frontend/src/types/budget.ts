export interface BudgetSummary {
  id: number;
  categoryId: number;
  categoryName: string;
  periodStart: string;
  limitAmount: string;
  spent: string;
  remaining: string;
}

export interface BudgetPeriod { year: number; month: number }
export interface SaveBudgetRequest extends BudgetPeriod { categoryId: number; limitAmount: string }
