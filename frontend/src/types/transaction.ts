export type TransactionType = "INCOME" | "EXPENSE";

export interface TransactionResponse {
  id: number;
  description: string;
  amount: number;
  date: string;
  type: TransactionType;
  accountId: number;
  accountName: string;
  categoryId: number;
  categoryName: string;
}