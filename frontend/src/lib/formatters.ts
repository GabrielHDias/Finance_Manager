import { cents, decimal } from "./money";
export function formatCurrency(amount: string): string {
  const normalized = decimal(cents(amount));
  const [whole, fraction] = normalized.replace(/^-/, "").split(".");
  return `${normalized.startsWith("-") ? "-" : ""}R$\u00a0${BigInt(whole).toLocaleString("pt-BR")},${fraction}`;
}
