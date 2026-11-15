export type TransactionType = "INCOME" | "EXPENSE" | "TRANSFER";

export interface CreateTransactionRequest {
  description: string;
  amount: string;
  date: string;
  type: TransactionType;
  accountId: number;
  categoryId?: number;
  destinationAccountId?: number;
}

export interface TransactionResponse {
  id: number;
  description: string;
  amount: string;
  date: string;
  type: TransactionType;
  accountId: number;
  accountName: string;
  categoryId: number | null;
  categoryName: string | null;
  destinationAccountId: number | null;
  destinationAccountName: string | null;
}

export interface TransactionFilters {
  startDate?: string;
  endDate?: string;
  type?: TransactionType;
  accountId?: number;
  categoryId?: number;
}

export interface TransactionPage {
  content: TransactionResponse[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}
