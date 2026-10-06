import { apiRequest } from "@/lib/api";
import type {
  AccountBalanceResponse,
  AccountResponse,
  CreateAccountRequest,
} from "@/types/account";

export async function findAllAccounts(): Promise<AccountResponse[]> {
  return apiRequest<AccountResponse[]>("/accounts");
}

export async function createAccount(
  account: CreateAccountRequest,
): Promise<AccountResponse> {
  return apiRequest<AccountResponse>("/accounts", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    body: JSON.stringify(account),
  });
}

export async function getAccountBalance(
  accountId: number,
): Promise<AccountBalanceResponse> {
  return apiRequest<AccountBalanceResponse>(
    `/accounts/${accountId}/balance`,
  );
}