"use client";

import { FormEvent, useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { ApiError } from "@/lib/api-error";
import { formatCurrency } from "@/lib/formatters";
import { findAllAccounts } from "@/services/account-service";
import { findAllCategories } from "@/services/category-service";
import {
  createTransaction,
  findAllTransactions,
} from "@/services/transaction-service";
import type { AccountResponse } from "@/types/account";
import type { CategoryResponse } from "@/types/category";
import type {
  TransactionResponse,
  TransactionType,
} from "@/types/transaction";

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
  const [accounts, setAccounts] = useState<AccountResponse[]>([]);
  const [categories, setCategories] = useState<CategoryResponse[]>([]);

  const [description, setDescription] = useState("");
  const [amount, setAmount] = useState("");
  const [date, setDate] = useState("");
  const [type, setType] = useState<TransactionType>("EXPENSE");
  const [accountId, setAccountId] = useState("");
  const [categoryId, setCategoryId] = useState("");

  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [errorMessage, setErrorMessage] = useState("");

  useEffect(() => {
    async function loadPageData() {
      try {
        const [
          transactionsResponse,
          accountsResponse,
          categoriesResponse,
        ] = await Promise.all([
          findAllTransactions(),
          findAllAccounts(),
          findAllCategories(),
        ]);

        setTransactions(transactionsResponse);
        setAccounts(accountsResponse);
        setCategories(categoriesResponse);
      } catch (error) {
        if (error instanceof ApiError) {
          if (error.status === 401) {
            router.replace("/login");
            return;
          }

          setErrorMessage(error.message);
          return;
        }

        setErrorMessage("Não foi possível carregar os dados da página.");
      } finally {
        setLoading(false);
      }
    }

    loadPageData();
  }, [router]);

  function handleError(error: unknown) {
    if (error instanceof ApiError) {
      if (error.status === 401) {
        router.replace("/login");
        return;
      }

      setErrorMessage(error.message);
      return;
    }

    setErrorMessage("Não foi possível completar a operação.");
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();

    const trimmedDescription = description.trim();
    const numericAmount = Number(amount);
    const numericAccountId = Number(accountId);
    const numericCategoryId = Number(categoryId);

    if (!trimmedDescription) {
      setErrorMessage("Informe a descrição da transação.");
      return;
    }

    if (!amount || numericAmount <= 0) {
      setErrorMessage("Informe um valor válido.");
      return;
    }

    if (!date) {
      setErrorMessage("Informe a data.");
      return;
    }

    if (!accountId) {
      setErrorMessage("Selecione uma conta.");
      return;
    }

    if (!categoryId) {
      setErrorMessage("Selecione uma categoria.");
      return;
    }

    setSubmitting(true);
    setErrorMessage("");

    try {
      const createdTransaction = await createTransaction({
        description: trimmedDescription,
        amount: numericAmount,
        date,
        type,
        accountId: numericAccountId,
        categoryId: numericCategoryId,
      });

      setTransactions((currentTransactions) => [
        ...currentTransactions,
        createdTransaction,
      ]);

      setDescription("");
      setAmount("");
      setDate("");
      setType("EXPENSE");
      setAccountId("");
      setCategoryId("");
    } catch (error) {
      handleError(error);
    } finally {
      setSubmitting(false);
    }
  }

  if (loading) {
    return <p>Carregando transações...</p>;
  }

  return (
  <main>
    <h1>Transações</h1>

    <section className="card">
      <h2>Nova transação</h2>

      <form className="transaction-form" onSubmit={handleSubmit}>
        <div>
          <label htmlFor="description">Descrição</label>

          <input
            id="description"
            name="description"
            type="text"
            value={description}
            onChange={(event) => setDescription(event.target.value)}
            required
          />
        </div>

        <div>
          <label htmlFor="amount">Valor</label>

          <input
            id="amount"
            name="amount"
            type="number"
            min="0.01"
            step="0.01"
            value={amount}
            onChange={(event) => setAmount(event.target.value)}
            required
          />
        </div>

        <div>
          <label htmlFor="date">Data</label>

          <input
            id="date"
            name="date"
            type="date"
            value={date}
            onChange={(event) => setDate(event.target.value)}
            required
          />
        </div>

        <div>
          <label htmlFor="type">Tipo</label>

          <select
            id="type"
            name="type"
            value={type}
            onChange={(event) =>
              setType(event.target.value as TransactionType)
            }
          >
            <option value="EXPENSE">Despesa</option>
            <option value="INCOME">Receita</option>
          </select>
        </div>

        <div>
          <label htmlFor="account">Conta</label>

          <select
            id="account"
            name="accountId"
            value={accountId}
            onChange={(event) => setAccountId(event.target.value)}
            required
          >
            <option value="">Selecione uma conta</option>

            {accounts.map((account) => (
              <option key={account.id} value={account.id}>
                {account.name}
              </option>
            ))}
          </select>
        </div>

        <div>
          <label htmlFor="category">Categoria</label>

          <select
            id="category"
            name="categoryId"
            value={categoryId}
            onChange={(event) => setCategoryId(event.target.value)}
            required
          >
            <option value="">Selecione uma categoria</option>

            {categories.map((category) => (
              <option key={category.id} value={category.id}>
                {category.name}
              </option>
            ))}
          </select>
        </div>

        <button type="submit" disabled={submitting}>
          {submitting ? "Salvando..." : "Criar transação"}
        </button>
      </form>

      {errorMessage && (
  <p className="error-message">{errorMessage}</p>
)}
    </section>

    <section className="card">
      <h2>Transações cadastradas</h2>

      {transactions.length === 0 ? (
        <p>Nenhuma transação cadastrada.</p>
      ) : (
        <ul className="transaction-list">
          {transactions.map((transaction) => (
            <li key={transaction.id} className="transaction-item">
              <div className="transaction-item__header">
                <strong>{transaction.description}</strong>
                <strong
                  className={
                    transaction.type === "INCOME"
                      ? "transaction-amount transaction-amount--income"
                      : "transaction-amount transaction-amount--expense"
                  }
                >
                  {formatCurrency(transaction.amount)}
              </strong>
              </div>

              <div className="transaction-item__details">
                <span>{formatDate(transaction.date)}</span>
                <span>{formatTransactionType(transaction.type)}</span>
                <span>Conta: {transaction.accountName}</span>
                <span>Categoria: {transaction.categoryName}</span>
              </div>
            </li>
          ))}
        </ul>
      )}
    </section>
  </main>
);
}