export type TransactionType = "INCOME" | "EXPENSE";

export interface CreateTransactionRequest {
  description: string;
  amount: number;
  date: string;
  type: TransactionType;
  accountId: number;
  categoryId: number;
}

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