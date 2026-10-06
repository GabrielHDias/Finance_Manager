import { apiRequest } from "@/lib/api";
import type {
  CreateTransactionRequest,
  TransactionResponse,
} from "@/types/transaction";

export async function findAllTransactions(): Promise<TransactionResponse[]> {
  return apiRequest<TransactionResponse[]>("/transactions");
}

export async function createTransaction(
  transaction: CreateTransactionRequest,
): Promise<TransactionResponse> {
  return apiRequest<TransactionResponse>("/transactions", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    body: JSON.stringify(transaction),
  });
}