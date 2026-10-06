"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { ApiError } from "@/lib/api-error";
import {
  getDashboardSummary,
  getExpensesByCategory,
} from "@/services/dashboard-service";
import type {
  DashboardSummaryResponse,
  ExpenseByCategoryResponse,
} from "@/types/dashboard";

function formatCurrency(amount: number): string {
  return new Intl.NumberFormat("pt-BR", {
    style: "currency",
    currency: "BRL",
  }).format(amount);
}

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

      <section>
        <h2>Resumo financeiro</h2>

        <p>Receitas: {formatCurrency(summary.totalIncome)}</p>
        <p>Despesas: {formatCurrency(summary.totalExpense)}</p>
        <p>Saldo: {formatCurrency(summary.balance)}</p>
      </section>

      <section>
        <h2>Despesas por categoria</h2>

        {expensesByCategory.length === 0 ? (
          <p>Nenhuma despesa cadastrada.</p>
        ) : (
          <ul>
            {expensesByCategory.map((expense) => (
              <li key={expense.categoryId}>
                {expense.categoryName}: {formatCurrency(expense.amount)}
              </li>
            ))}
          </ul>
        )}
      </section>
    </main>
  );
}