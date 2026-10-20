"use client";

import { Bar, BarChart, CartesianGrid, Line, LineChart, Pie, PieChart, ResponsiveContainer, Tooltip, XAxis, YAxis } from "recharts";
import { ratio, sign } from "@/lib/money";
import { formatCurrency } from "@/lib/formatters";
import type { ChartType, TransactionChartResponse } from "@/types/dashboard";
import styles from "./financial-chart.module.css";

const percent = new Intl.NumberFormat("pt-BR", { style: "percent", maximumFractionDigits: 1 });
const money = new Intl.NumberFormat("pt-BR", { style: "currency", currency: "BRL", notation: "compact", maximumFractionDigits: 1 });

export default function FinancialChart({ summary, chart, title }: { summary: TransactionChartResponse; chart: ChartType; title: string }) {
  if (sign(summary.total) === 0) return <p role="status">Nenhuma movimentação encontrada para o período selecionado.</p>;
  // Geometry uses bounded ratios; tooltips and the accessible list retain exact decimal strings.
  const data = summary.items.map((item, index) => ({ ...item, value: ratio(item.total, summary.total), fill: `var(--chart-${index % 8})` }));
  const tooltip = <Tooltip contentStyle={{ backgroundColor: "var(--color-surface)", borderColor: "var(--color-border)", color: "var(--color-text)" }} itemStyle={{ color: "var(--color-text)" }} formatter={(_value, _name, item) => `${formatCurrency(item.payload.total)}${chart === "PIE" ? ` (${percent.format(item.payload.value)})` : ""}`} />;
  const axes = <>
    <CartesianGrid stroke="var(--color-border)" strokeDasharray="3 3" />
    <XAxis dataKey="label" stroke="var(--color-text-muted)" tick={{ fontSize: 11 }} minTickGap={24} />
    <YAxis width={82} stroke="var(--color-text-muted)" tick={{ fontSize: 11 }} tickFormatter={value => money.format(value * Number(summary.total))} />
  </>;
  return <>
    <div className={styles.chart} data-chart={chart}>
      <ResponsiveContainer width="100%" height={300} minWidth={0}>
        {chart === "PIE" ? <PieChart accessibilityLayer aria-label={title}>
          <Pie data={data.filter(item => sign(item.total) > 0)} dataKey="value" nameKey="label" innerRadius="55%" outerRadius="85%" stroke="var(--color-surface)" isAnimationActive={false} />{tooltip}
        </PieChart> : chart === "LINE" ? <LineChart data={data} accessibilityLayer aria-label={title} margin={{ left: 0, right: 16, bottom: 16 }}>
          {axes}{tooltip}<Line type="linear" dataKey="value" name={title} stroke="var(--chart-0)" strokeWidth={2} dot={data.length <= 31} isAnimationActive={false} />
        </LineChart> : <BarChart data={data} accessibilityLayer aria-label={title} margin={{ left: 0, right: 16, bottom: 16 }}>
          {axes}{tooltip}<Bar dataKey="value" name={title} fill="var(--chart-0)" maxBarSize={48} isAnimationActive={false} />
        </BarChart>}
      </ResponsiveContainer>
    </div>
    <ul className={styles.legend} aria-label={`${title}: valores e participação`}>
      {data.map(item => <li key={item.key}>
        <span className={styles.name}>{chart === "PIE" && <span className={styles.swatch} style={{ backgroundColor: item.fill }} aria-hidden="true" />}{item.label}</span>
        <span className={styles.values}><strong>{formatCurrency(item.total)}</strong>{chart === "PIE" && <span>{percent.format(item.value)}</span>}</span>
      </li>)}
    </ul>
  </>;
}
