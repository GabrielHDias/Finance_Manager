"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { ApiError } from "@/lib/api-error";
import { getTransactionsSummary } from "@/services/dashboard-service";
import type { ChartGrouping, ChartType, FinancialType, TransactionChartResponse } from "@/types/dashboard";
import ChartFilters from "./chart-filters";
import ChartSummary from "./chart-summary";
import FinancialChart from "./financial-chart";

export default function FinancialChartPanel({ startDate, endDate }: { startDate: string; endDate: string }) {
  const router = useRouter();
  const [type, setType] = useState<FinancialType>("EXPENSE");
  const [groupBy, setGroupBy] = useState<ChartGrouping>("CATEGORY");
  const [chart, setChart] = useState<ChartType>("PIE");
  const [revision, setRevision] = useState(0);
  const [result, setResult] = useState<{ key: string; data?: TransactionChartResponse; error?: string }>();
  const key = JSON.stringify([type, groupBy, startDate, endDate, revision]);
  const current = result?.key === key ? result : undefined;
  const days = (Date.parse(endDate) - Date.parse(startDate)) / 86400000 + 1;
  const validation = groupBy === "DAY" && days > 366 ? "O agrupamento diário permite no máximo 366 dias. Reduza o período ou escolha outro agrupamento." : "";
  const loading = !validation && !current;
  const months = (Number(endDate.slice(0, 4)) - Number(startDate.slice(0, 4))) * 12 + Number(endDate.slice(5, 7)) - Number(startDate.slice(5, 7)) + 1;
  const pieDisabled = groupBy !== "CATEGORY" && (groupBy === "DAY" ? days > 8 : months > 8);
  const effectiveChart = chart === "LINE" && groupBy === "CATEGORY" ? "BAR" : chart === "PIE" && pieDisabled ? "BAR" : chart;
  const title = `${type === "INCOME" ? "Receitas" : "Despesas"} por ${groupBy === "DAY" ? "dia" : groupBy === "MONTH" ? "mês" : "categoria"}`;

  useEffect(() => {
    if (validation) return;
    const controller = new AbortController();
    let active = true;
    getTransactionsSummary({ type, groupBy, startDate, endDate }, controller.signal).then(data => {
      if (active) setResult({ key, data });
    }).catch(error => {
      if (!active) return;
      if (error instanceof ApiError && error.status === 401) { router.replace("/login"); return; }
      setResult({ key, error: error instanceof ApiError ? error.message : "Não foi possível carregar o gráfico. Tente novamente." });
    });
    return () => { active = false; controller.abort(); };
  }, [type, groupBy, startDate, endDate, key, validation, router]);

  function changeGroup(value: ChartGrouping) {
    setGroupBy(value);
    if (value === "CATEGORY" && chart === "LINE") setChart("BAR");
    if (value !== "CATEGORY" && chart === "PIE") setChart("LINE");
  }

  return <section className="card" aria-labelledby="financial-chart-title">
    <h2 id="financial-chart-title">{title}</h2>
    <ChartFilters type={type} groupBy={groupBy} chart={effectiveChart} pieDisabled={pieDisabled} onType={setType} onGroup={changeGroup} onChart={setChart} />
    <p id="chart-compatibility">{groupBy === "CATEGORY" ? "Categorias permitem pizza ou coluna. Linha está indisponível porque as categorias não formam uma série temporal." : "Séries temporais priorizam linha ou coluna. Pizza está disponível para até 8 períodos. Dias e meses sem movimentação aparecem com zero."}</p>
    {chart !== effectiveChart && <p role="status">Gráfico ajustado para coluna para manter a legibilidade; os demais filtros foram preservados.</p>}
    <div aria-busy={loading} aria-live="polite">
      {validation ? <p className="error-message" role="alert">{validation}</p> : loading ? <p role="status">Carregando gráfico...</p> : current?.error ? <><p className="error-message" role="alert">{current.error}</p><button type="button" onClick={() => setRevision(value => value + 1)}>Tentar novamente</button></> : current?.data && <>
        <ChartSummary summary={current.data} /><FinancialChart summary={current.data} chart={effectiveChart} title={title} />
      </>}
    </div>
  </section>;
}
