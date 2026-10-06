export interface CreateAccountRequest {
  name: string;
}

export interface AccountResponse {
  id: number;
  name: string;
}

export interface AccountBalanceResponse {
  accountId: number;
  balance: number;
}