import { cents, decimal } from "./money";
import { formatDate } from "./dates";
import type { TransactionResponse } from "../types/transaction";

function csvCell(value: string): string {
  // Quoting alone does not prevent spreadsheet formulas in user-supplied text.
  const safe = /^[\t\r\n]|^\s*[=+\-@]/u.test(value) ? `'${value}` : value;
  return `"${safe.replaceAll('"', '""')}"`;
}

export function serializeTransactionCsv(transactions: readonly TransactionResponse[]): string {
  const rows = transactions.map(transaction => [
    formatDate(transaction.date),
    transaction.description,
    decimal(cents(transaction.amount)).replace(".", ","),
    transaction.type === "INCOME" ? "RECEITA" : "DESPESA",
    transaction.accountName,
    transaction.categoryName,
  ].map(csvCell).join(";"));

  return `\uFEFFdata;descricao;valor;tipo;conta;categoria\r\n${rows.join("\r\n")}${rows.length ? "\r\n" : ""}`;
}

export function downloadTransactionCsv(transactions: readonly TransactionResponse[]): void {
  const blob = new Blob([serializeTransactionCsv(transactions)], { type: "text/csv;charset=utf-8" });
  const url = URL.createObjectURL(blob);
  const link = document.createElement("a");
  try {
    link.href = url;
    link.download = "transacoes.csv";
    document.body.appendChild(link);
    link.click();
  } finally {
    link.remove();
    // Give the browser time to start the download before releasing its contents.
    window.setTimeout(() => URL.revokeObjectURL(url), 1000);
  }
}
