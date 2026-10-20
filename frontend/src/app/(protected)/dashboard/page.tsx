"use client";

import { FormEvent, useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { ApiError } from "@/lib/api-error";
import { formatCurrency } from "@/lib/formatters";
import { formatDate, parseDisplayDate } from "@/lib/dates";
import styles from "./dashboard.module.css";
import DateInput from "@/components/date-input";
import ExpensesByCategoryChart from "@/components/dashboard/expenses-by-category-chart";
import { monthPeriod } from "@/lib/monthly-summary";
import MonthlySummary from "@/components/monthly-summary";
import {
  getDashboardSummary,
  getExpensesByCategory,
} from "@/services/dashboard-service";
import type {
  DashboardPeriod,
  DashboardSummaryResponse,
  ExpenseByCategoryResponse,
} from "@/types/dashboard";

interface DashboardData {
  period: DashboardPeriod;
  summary: DashboardSummaryResponse;
  expenses: ExpenseByCategoryResponse[];
}

function formatPeriod(period: DashboardPeriod): string {
  if (period.startDate && period.endDate) {
    return `De ${formatDate(period.startDate)} a ${formatDate(period.endDate)}`;
  }
  if (period.startDate) return `A partir de ${formatDate(period.startDate)}`;
  if (period.endDate) return `Até ${formatDate(period.endDate)}`;
  return "Todo o período";
}

export default function DashboardPage() {
  const router = useRouter();
  const [period, setPeriod] = useState<DashboardPeriod | null>(null);
  const [dashboard, setDashboard] = useState<DashboardData | null>(null);
  const [revision, setRevision] = useState(0);
  const [loading, setLoading] = useState(true);
  const [errorMessage, setErrorMessage] = useState("");
  const [validationMessage, setValidationMessage] = useState("");

  useEffect(() => {
    let cancelled = false;

    async function loadDashboard() {
      const now = new Date();
      const selectedPeriod = period ?? monthPeriod(`${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, "0")}`);
      try {
        const [summary, expenses] = await Promise.all([
          getDashboardSummary(selectedPeriod),
          getExpensesByCategory(selectedPeriod),
        ]);
        if (cancelled) return;
        setDashboard({ period: selectedPeriod, summary, expenses });
      } catch (error) {
        if (cancelled) return;
        if (error instanceof ApiError && error.status === 401) {
          router.replace("/login");
          return;
        }
        setErrorMessage(error instanceof ApiError
          ? error.message
          : "Não foi possível carregar o dashboard. Tente novamente.");
      } finally {
        if (!cancelled) setLoading(false);
      }
    }

    loadDashboard();
    return () => { cancelled = true; };
  }, [period, revision, router]);

  function applyPeriod(nextPeriod: DashboardPeriod | null) {
    if (loading) return;
    setValidationMessage("");
    setErrorMessage("");
    setDashboard(null);
    setLoading(true);
    setRevision(value => value + 1);
    setPeriod(nextPeriod === null ? null : { ...nextPeriod });
  }

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const data = new FormData(event.currentTarget);
    const startValue = String(data.get("startDate") || "");
    const endValue = String(data.get("endDate") || "");
    const startDate = startValue ? parseDisplayDate(startValue) : undefined;
    const endDate = endValue ? parseDisplayDate(endValue) : undefined;
    if (startDate === null || endDate === null) {
      setValidationMessage("Informe datas válidas no formato DD/MM/YYYY.");
      return;
    }
    if (startDate && endDate && startDate > endDate) {
      setValidationMessage("A data inicial deve ser anterior ou igual à data final.");
      return;
    }
    applyPeriod({ startDate: startDate || undefined, endDate: endDate || undefined });
  }

  return (
    <main className={styles.dashboard}>
      <h1>Dashboard</h1>
      <section className={`card ${styles.filters}`}>
        <h2>Período</h2>
        <p>O mês atual é exibido inicialmente. Selecione as datas desejadas ou deixe-as em branco para consultar todo o período.</p>
        <form key={dashboard ? "ready" : "loading"} className="transaction-form" onSubmit={handleSubmit} onReset={() => applyPeriod({})}>
          <div>
            <label htmlFor="dashboard-start">Data inicial</label>
            <DateInput id="dashboard-start" name="startDate" defaultValue={dashboard?.period.startDate ? formatDate(dashboard.period.startDate) : ""} disabled={loading} />
          </div>
          <div>
            <label htmlFor="dashboard-end">Data final</label>
            <DateInput id="dashboard-end" name="endDate" defaultValue={dashboard?.period.endDate ? formatDate(dashboard.period.endDate) : ""} disabled={loading} />
          </div>
          <button type="submit" disabled={loading}>Aplicar período</button>
          <button className="button--secondary" type="reset" disabled={loading}>Limpar período</button>
        </form>
        {validationMessage && <p className="error-message" role="alert">{validationMessage}</p>}
      </section>
      <div className={styles.results} aria-busy={loading}>
        {loading ? <p role="status">Carregando dashboard...</p> : errorMessage ? (
          <div><p className="error-message" role="alert">{errorMessage}</p><button type="button" onClick={() => applyPeriod(period)}>Tentar novamente</button></div>
        ) : dashboard && (
          <>
            <section className="card">
              <h2>Resumo financeiro</h2>
              <p>Período exibido: {formatPeriod(dashboard.period)}</p>
              <div className="summary-grid">
                <div>
                  <strong>Receitas</strong>
                  <p className="financial-value--income">{formatCurrency(dashboard.summary.totalIncome)}</p>
                </div>
                <div>
                  <strong>Despesas</strong>
                  <p className="financial-value--expense">{formatCurrency(dashboard.summary.totalExpense)}</p>
                </div>
                <div>
                  <strong>{dashboard.period.startDate || dashboard.period.endDate ? "Saldo do período" : "Saldo"}</strong>
                  <p className={dashboard.summary.balance > 0 ? "financial-value--income" : dashboard.summary.balance < 0 ? "financial-value--expense" : undefined}>{formatCurrency(dashboard.summary.balance)}</p>
                </div>
              </div>
            </section>
            <section className="card">
              <h2>Despesas por categoria</h2>
              <ExpensesByCategoryChart expenses={dashboard.expenses} />
            </section>
          </>
        )}
      </div>
      <MonthlySummary />
    </main>
  );
}
