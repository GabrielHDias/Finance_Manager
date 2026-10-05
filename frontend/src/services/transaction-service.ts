import { apiRequest } from "@/lib/api";
import type { TransactionResponse } from "@/types/transaction";

export async function findAllTransactions(): Promise<TransactionResponse[]> {
  return apiRequest<TransactionResponse[]>("/transactions");
}