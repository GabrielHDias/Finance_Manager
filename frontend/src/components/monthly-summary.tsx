"use client";

import { useEffect, useState, type FormEvent } from "react";
import { useRouter } from "next/navigation";
import { ApiError } from "@/lib/api-error";
import { formatDate } from "@/lib/dates";
import { formatCurrency } from "@/lib/formatters";
import { MONTH_NAMES, monthLabel, monthlyDifference, monthlyPeriods } from "@/lib/monthly-summary";
import { getDashboardSummary } from "@/services/dashboard-service";
import type { DashboardSummaryResponse } from "@/types/dashboard";

interface MonthlyData {
  periods: ReturnType<typeof monthlyPeriods>;
  current: DashboardSummaryResponse;
  previous: DashboardSummaryResponse;
}

const metrics = [
  { key: "totalIncome", label: "Receitas" },
  { key: "totalExpense", label: "Despesas" },
  { key: "balance", label: "Saldo do mês" },
] as const;

export default function MonthlySummary() {
  const router = useRouter();
  const [request, setRequest] = useState<{ reference?: string; revision: number }>({ revision: 0 });
  const [data, setData] = useState<MonthlyData | null>(null);
  const [month, setMonth] = useState<string | null>(null);
  const [year, setYear] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [validation, setValidation] = useState("");

  useEffect(() => {
    let cancelled = false;
    async function load() {
      try {
        // Read the user's local month after mounting, keeping SSR and hydration identical.
        const now = new Date();
        const reference = request.reference ?? `${String(now.getFullYear()).padStart(4, "0")}-${String(now.getMonth() + 1).padStart(2, "0")}`;
        const periods = monthlyPeriods(reference);
        const [current, previous] = await Promise.all([
          getDashboardSummary(periods.current),
          getDashboardSummary(periods.previous),
        ]);
        if (!cancelled) setData({ periods, current, previous });
      } catch (failure) {
        if (cancelled) return;
        if (failure instanceof ApiError && failure.status === 401) {
          router.replace("/login");
          return;
        }
        setError(failure instanceof ApiError ? failure.message : "Não foi possível carregar a comparação mensal. Tente novamente.");
      } finally {
        if (!cancelled) setLoading(false);
      }
    }
    load();
    return () => { cancelled = true; };
  }, [request, router]);

  function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (loading) return;
    const form = new FormData(event.currentTarget);
    const reference = `${form.get("year")}-${form.get("month")}`;
    try {
      monthlyPeriods(reference);
    } catch (failure) {
      setValidation(failure instanceof Error ? failure.message : "Informe um mês e um ano válidos.");
      return;
    }
    setValidation("");
    setError("");
    setLoading(true);
    setRequest(current => ({ reference, revision: current.revision + 1 }));
  }

  function retry() {
    if (loading) return;
    setError("");
    setLoading(true);
    setRequest(current => ({ ...current, revision: current.revision + 1 }));
  }

  return (
    <section className="card monthly-summary" aria-labelledby="monthly-title">
      <h2 id="monthly-title">Resumo mensal</h2>
      <p>Compare um mês com o anterior. Esta seleção é independente do filtro de período acima.</p>
      <form className="transaction-form" onSubmit={submit}>
        <div>
          <label htmlFor="monthly-month">Mês de referência</label>
          <select id="monthly-month" name="month" value={month ?? data?.periods.reference.slice(5) ?? ""} onChange={event => setMonth(event.target.value)} required disabled={loading}>
            <option value="" disabled>Selecione o mês</option>
            {MONTH_NAMES.map((name, index) => <option key={name} value={String(index + 1).padStart(2, "0")}>{name}</option>)}
          </select>
        </div>
        <div>
          <label htmlFor="monthly-year">Ano de referência</label>
          <input id="monthly-year" name="year" type="text" inputMode="numeric" pattern="[0-9]{4}" maxLength={4} placeholder="YYYY" title="Informe o ano com quatro dígitos." value={year ?? data?.periods.reference.slice(0, 4) ?? ""} onChange={event => setYear(event.target.value)} required disabled={loading} />
        </div>
        <button type="submit" disabled={loading}>Comparar meses</button>
      </form>
      {validation && <p className="error-message" role="alert">{validation}</p>}
      <div aria-busy={loading}>
        {loading ? <p role="status">Carregando comparação mensal...</p> : error ? (
          <div>
            <p className="error-message" role="alert">{error}</p>
            <button className="button--secondary" type="button" onClick={retry}>Tentar comparação novamente</button>
          </div>
        ) : data && (
          <>
            <p><strong>{monthLabel(data.periods.reference)}</strong>: {formatDate(data.periods.current.startDate)} a {formatDate(data.periods.current.endDate)}<br />
              Mês anterior: <strong>{monthLabel(data.periods.previousReference)}</strong>, de {formatDate(data.periods.previous.startDate)} a {formatDate(data.periods.previous.endDate)}.</p>
            <div className="summary-grid monthly-summary__metrics">
              {metrics.map(metric => {
                const value = data.current[metric.key];
                const previous = data.previous[metric.key];
                const difference = monthlyDifference(value, previous);
                const color = metric.key === "totalIncome" ? "financial-value--income" : metric.key === "totalExpense" ? "financial-value--expense" : value > 0 ? "financial-value--income" : value < 0 ? "financial-value--expense" : undefined;
                return (
                  <div key={metric.key}>
                    <h3>{metric.label}</h3>
                    <p className={color}>{formatCurrency(value)}</p>
                    <p className="monthly-summary__detail">Mês anterior: {formatCurrency(previous)}</p>
                    <p className="monthly-summary__detail">{difference === 0 ? "Sem variação." : `${difference > 0 ? "Aumento" : "Redução"} de ${formatCurrency(Math.abs(difference))}.`}</p>
                  </div>
                );
              })}
            </div>
            {data.current.totalIncome === 0 && data.current.totalExpense === 0 && <p>Nenhuma movimentação no mês selecionado.</p>}
            {data.previous.totalIncome === 0 && data.previous.totalExpense === 0 && <p>O mês anterior não possui movimentações.</p>}
            <p className="monthly-summary__note">Os totais incluem todas as transações datadas em cada mês. Saldo do mês = receitas − despesas; não é o saldo acumulado das contas.</p>
          </>
        )}
      </div>
    </section>
  );
}
