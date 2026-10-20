"use client";

import { Pie, PieChart, ResponsiveContainer, Tooltip } from "recharts";
import { formatCurrency } from "@/lib/formatters";
import type { ExpenseByCategoryResponse } from "@/types/dashboard";
import styles from "./expenses-by-category-chart.module.css";

const colors = Array.from({ length: 8 }, (_, index) => `var(--chart-${index})`);
const percent = new Intl.NumberFormat("pt-BR", { style: "percent", maximumFractionDigits: 1 });

export default function ExpensesByCategoryChart({ expenses }: { expenses: ExpenseByCategoryResponse[] }) {
  // Integer cents keep presentation totals consistent with the API's decimal amounts.
  const totalCents = expenses.reduce((sum, item) => sum + Math.round(item.amount * 100), 0);
  if (!expenses.length || totalCents <= 0) {
    return <p>Nenhuma despesa encontrada para o período selecionado.</p>;
  }
  const data = expenses.map(item => ({
    ...item,
    cents: Math.round(item.amount * 100),
    fill: colors[(item.categoryId % colors.length + colors.length) % colors.length],
  }));

  return (
    <div className={styles.layout}>
      <div className={styles.chart}>
        <ResponsiveContainer width="100%" height={280} minWidth={0}>
          <PieChart accessibilityLayer aria-label="Distribuição das despesas por categoria. Valores detalhados na lista a seguir.">
            <Pie data={data} dataKey="cents" nameKey="categoryName" innerRadius="58%" outerRadius="88%" stroke="var(--color-surface)" isAnimationActive={false} />
            <Tooltip contentStyle={{ backgroundColor: "var(--color-surface)", borderColor: "var(--color-border)", color: "var(--color-text)", borderRadius: "var(--radius-control)" }} itemStyle={{ color: "var(--color-text)" }} formatter={(value) => `${formatCurrency(Number(value) / 100)} (${percent.format(Number(value) / totalCents)})`} />
          </PieChart>
        </ResponsiveContainer>
        <p className={styles.total}>Total de despesas <strong>{formatCurrency(totalCents / 100)}</strong></p>
      </div>
      <ul className={styles.legend} aria-label="Despesas por categoria: valores e participação">
        {data.map(item => (
          <li key={item.categoryId}>
            <span className={styles.name}><span className={styles.swatch} style={{ backgroundColor: item.fill }} aria-hidden="true" />{item.categoryName}</span>
            <span className={styles.values}><strong>{formatCurrency(item.amount)}</strong><span>{percent.format(item.cents / totalCents)}</span></span>
          </li>
        ))}
      </ul>
    </div>
  );
}
