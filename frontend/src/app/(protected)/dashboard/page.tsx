"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { ApiError } from "@/lib/api-error";
import { formatCurrency } from "@/lib/formatters";
import {
  getDashboardSummary,
  getExpensesByCategory,
} from "@/services/dashboard-service";
import type {
  DashboardSummaryResponse,
  ExpenseByCategoryResponse,
} from "@/types/dashboard";

export default function DashboardPage() {
  const router = useRouter();

  const [summary, setSummary] = useState<DashboardSummaryResponse | null>(null);
  const [expensesByCategory, setExpensesByCategory] = useState<
    ExpenseByCategoryResponse[]
  >([]);
  const [loading, setLoading] = useState(true);
  const [errorMessage, setErrorMessage] = useState("");

  useEffect(() => {
    async function loadDashboard() {
      try {
        const [summaryResponse, expensesResponse] = await Promise.all([
          getDashboardSummary(),
          getExpensesByCategory(),
        ]);

        setSummary(summaryResponse);
        setExpensesByCategory(expensesResponse);
      } catch (error) {
        if (error instanceof ApiError) {
          if (error.status === 401) {
            router.replace("/login");
            return;
          }

          setErrorMessage(error.message);
          return;
        }

        setErrorMessage("Não foi possível carregar o dashboard.");
      } finally {
        setLoading(false);
      }
    }

    loadDashboard();
  }, [router]);

  if (loading) {
    return <p>Carregando dashboard...</p>;
  }

  if (errorMessage) {
    return <p>{errorMessage}</p>;
  }

  if (!summary) {
    return <p>Resumo financeiro indisponível.</p>;
  }

  return (
  <main>
    <h1>Dashboard</h1>

    <section className="card">
      <h2>Resumo financeiro</h2>

      <div className="summary-grid">
        <div>
          <strong>Receitas</strong>
          <p>{formatCurrency(summary.totalIncome)}</p>
        </div>

        <div>
          <strong>Despesas</strong>
          <p>{formatCurrency(summary.totalExpense)}</p>
        </div>

        <div>
          <strong>Saldo</strong>
          <p>{formatCurrency(summary.balance)}</p>
        </div>
      </div>
    </section>

    <section className="card">
      <h2>Despesas por categoria</h2>

      {expensesByCategory.length === 0 ? (
        <p>Nenhuma despesa cadastrada.</p>
      ) : (
        <ul className="data-list">
          {expensesByCategory.map((expense) => (
            <li key={expense.categoryId}>
              <span>{expense.categoryName}</span>
              <strong>{formatCurrency(expense.amount)}</strong>
            </li>
          ))}
        </ul>
      )}
    </section>
  </main>
);
}