import { formatCurrency } from "@/lib/formatters";
import type { TransactionChartResponse } from "@/types/dashboard";

export default function ChartSummary({ summary }: { summary: TransactionChartResponse }) {
  return <p>Total de {summary.type === "INCOME" ? "receitas" : "despesas"} no período: <strong>{formatCurrency(summary.total)}</strong></p>;
}
