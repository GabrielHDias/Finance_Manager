import { apiRequest } from "@/lib/api";
import type { AccountResponse } from "@/types/account";

export async function findAllAccounts(): Promise<AccountResponse[]> {
  return apiRequest<AccountResponse[]>("/accounts");
}