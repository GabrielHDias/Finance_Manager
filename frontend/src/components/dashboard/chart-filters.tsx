import type { ChartGrouping, ChartType, FinancialType } from "@/types/dashboard";
import styles from "./financial-chart.module.css";

interface Props {
  type: FinancialType;
  groupBy: ChartGrouping;
  chart: ChartType;
  pieDisabled: boolean;
  onType: (value: FinancialType) => void;
  onGroup: (value: ChartGrouping) => void;
  onChart: (value: ChartType) => void;
}

export default function ChartFilters({ type, groupBy, chart, pieDisabled, onType, onGroup, onChart }: Props) {
  return <div className={styles.filters}>
    <div><label htmlFor="chart-type">Movimentação</label>
      <select id="chart-type" value={type} onChange={event => onType(event.target.value as FinancialType)}>
        <option value="EXPENSE">Despesas</option><option value="INCOME">Receitas</option>
      </select></div>
    <div><label htmlFor="chart-group">Agrupamento</label>
      <select id="chart-group" value={groupBy} onChange={event => onGroup(event.target.value as ChartGrouping)}>
        <option value="CATEGORY">Por categoria</option><option value="DAY">Por dia</option><option value="MONTH">Por mês</option>
      </select></div>
    <div><label htmlFor="chart-visual">Tipo de gráfico</label>
      <select id="chart-visual" value={chart} aria-describedby="chart-compatibility" onChange={event => onChart(event.target.value as ChartType)}>
        <option value="PIE" disabled={pieDisabled}>Pizza (rosca)</option>
        <option value="LINE" disabled={groupBy === "CATEGORY"}>Linha</option><option value="BAR">Coluna</option>
      </select></div>
  </div>;
}
