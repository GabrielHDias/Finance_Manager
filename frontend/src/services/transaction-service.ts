import { apiRequest } from "@/lib/api";
import type {
  CreateTransactionRequest,
  TransactionFilters,
  TransactionResponse,
} from "@/types/transaction";

export async function findAllTransactions(
  filters: TransactionFilters = {},
): Promise<TransactionResponse[]> {
  const query = new URLSearchParams();
  for (const [key, value] of Object.entries(filters)) {
    if (value !== undefined && value !== "") query.set(key, String(value));
  }
  const suffix = query.size ? `?${query}` : "";
  return apiRequest<TransactionResponse[]>(`/transactions${suffix}`);
}

export async function createTransaction(
  transaction: CreateTransactionRequest,
): Promise<TransactionResponse> {
  return apiRequest<TransactionResponse>("/transactions", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(transaction),
  });
}

export async function updateTransaction(
  id: number,
  transaction: CreateTransactionRequest,
): Promise<TransactionResponse> {
  return apiRequest<TransactionResponse>(`/transactions/${id}`, {
    method: "PUT",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(transaction),
  });
}

export async function deleteTransaction(id: number): Promise<void> {
  return apiRequest<void>(`/transactions/${id}`, { method: "DELETE" });
}
