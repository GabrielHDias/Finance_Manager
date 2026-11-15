import type { TransactionFilters, TransactionResponse } from "@/types/transaction";

export function sortTransactions(transactions: TransactionResponse[]): TransactionResponse[] {
  return [...transactions].sort(
    (a, b) => b.date.localeCompare(a.date) || b.id - a.id,
  );
}

export function matchesTransactionFilters(
  transaction: TransactionResponse,
  filters: TransactionFilters,
): boolean {
  return (
    (!filters.startDate || transaction.date >= filters.startDate) &&
    (!filters.endDate || transaction.date <= filters.endDate) &&
    (!filters.type || transaction.type === filters.type) &&
    (filters.accountId === undefined || transaction.accountId === filters.accountId || transaction.destinationAccountId === filters.accountId) &&
    (filters.categoryId === undefined || transaction.categoryId === filters.categoryId) &&
    (filters.tagId === undefined || transaction.tags.some(tag => tag.id === filters.tagId))
  );
}

export function mergeTransaction(
  transactions: TransactionResponse[],
  saved: TransactionResponse,
  filters: TransactionFilters,
): TransactionResponse[] {
  const others = transactions.filter((transaction) => transaction.id !== saved.id);
  return sortTransactions(matchesTransactionFilters(saved, filters) ? [...others, saved] : others);
}
