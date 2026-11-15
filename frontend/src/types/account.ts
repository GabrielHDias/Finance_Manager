export interface CreateAccountRequest {
  name: string;
  openingBalance?: string;
}

export interface AccountResponse {
  id: number;
  name: string;
  openingBalance: string;
}

export interface AccountBalanceResponse {
  accountId: number;
  balance: string;
}