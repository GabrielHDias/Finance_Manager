"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { ApiError } from "@/lib/api-error";
import { findAllTransactions } from "@/services/transaction-service";
import type {
  TransactionResponse,
  TransactionType,
} from "@/types/transaction";

function formatCurrency(amount: number): string {
  return new Intl.NumberFormat("pt-BR", {
    style: "currency",
    currency: "BRL",
  }).format(amount);
}

function formatDate(date: string): string {
  const [year, month, day] = date.split("-");

  return `${day}/${month}/${year}`;
}

function formatTransactionType(type: TransactionType): string {
  return type === "INCOME" ? "Receita" : "Despesa";
}

export default function TransactionsPage() {
  const router = useRouter();

  const [transactions, setTransactions] = useState<TransactionResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [errorMessage, setErrorMessage] = useState("");

  useEffect(() => {
    async function loadTransactions() {
      try {
        const response = await findAllTransactions();

        setTransactions(response);
      } catch (error) {
        if (error instanceof ApiError) {
          if (error.status === 401) {
            router.replace("/login");
            return;
          }

          setErrorMessage(error.message);
          return;
        }

        setErrorMessage("Não foi possível carregar as transações.");
      } finally {
        setLoading(false);
      }
    }

    loadTransactions();
  }, [router]);

  if (loading) {
    return <p>Carregando transações...</p>;
  }

  if (errorMessage) {
    return <p>{errorMessage}</p>;
  }

  return (
    <main>
      <h1>Transações</h1>

      {transactions.length === 0 ? (
        <p>Nenhuma transação cadastrada.</p>
      ) : (
        <ul>
          {transactions.map((transaction) => (
            <li key={transaction.id}>
              <p>{transaction.description}</p>
              <p>{formatCurrency(transaction.amount)}</p>
              <p>{formatDate(transaction.date)}</p>
              <p>{formatTransactionType(transaction.type)}</p>
              <p>Conta: {transaction.accountName}</p>
              <p>Categoria: {transaction.categoryName}</p>
            </li>
          ))}
        </ul>
      )}
    </main>
  );
}