export interface BudgetSummary {
  id: number;
  categoryId: number;
  categoryName: string;
  periodStart: string;
  limitAmount: number;
  spent: number;
  remaining: number;
}

export interface BudgetPeriod { year: number; month: number }
export interface SaveBudgetRequest extends BudgetPeriod { categoryId: number; limitAmount: number }
