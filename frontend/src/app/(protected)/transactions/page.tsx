"use client";

import { FormEvent, useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { ApiError } from "@/lib/api-error";
import { formatCurrency } from "@/lib/formatters";
import { matchesTransactionFilters, mergeTransaction, sortTransactions } from "@/lib/transaction-list";
import { findAllAccounts } from "@/services/account-service";
import { findAllCategories } from "@/services/category-service";
import { createTransaction, deleteTransaction, findAllTransactions, updateTransaction } from "@/services/transaction-service";
import type { AccountResponse } from "@/types/account";
import type { CategoryResponse } from "@/types/category";
import type { TransactionFilters, TransactionResponse, TransactionType } from "@/types/transaction";

function formatDate(date: string): string {
  const [year, month, day] = date.split("-");
  return `${day}/${month}/${year}`;
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
  const [editingId, setEditingId] = useState<number | null>(null);
  const [pendingDelete, setPendingDelete] = useState<number | null>(null);
  const [filters, setFilters] = useState<TransactionFilters>({});
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [deleting, setDeleting] = useState(false);
  const [listFailed, setListFailed] = useState(false);
  const [errorMessage, setErrorMessage] = useState("");
  const [successMessage, setSuccessMessage] = useState("");
  const busy = loading || submitting || deleting;

  useEffect(() => {
    let cancelled = false;
    async function loadPageData() {
      try {
        const [items, accountsResponse, categoriesResponse] = await Promise.all([
          findAllTransactions(), findAllAccounts(), findAllCategories(),
        ]);
        if (cancelled) return;
        setTransactions(sortTransactions(items));
        setAccounts(accountsResponse);
        setCategories(categoriesResponse);
      } catch (error) {
        if (cancelled) return;
        setListFailed(true);
        if (error instanceof ApiError && error.status === 401) {
          router.replace("/login");
          return;
        }
        setErrorMessage(error instanceof ApiError ? error.message : "Não foi possível carregar os dados. Recarregue a página para tentar novamente.");
      } finally {
        if (!cancelled) setLoading(false);
      }
    }
    loadPageData();
    return () => { cancelled = true; };
  }, [router]);

  function handleError(error: unknown) {
    if (error instanceof ApiError && error.status === 401) {
      router.replace("/login");
      return;
    }
    setErrorMessage(error instanceof ApiError ? error.message : "Não foi possível completar a operação. Tente novamente.");
  }

  function resetForm() {
    setEditingId(null);
    setDescription("");
    setAmount("");
    setDate("");
    setType("EXPENSE");
    setAccountId("");
    setCategoryId("");
  }

  function startEditing(transaction: TransactionResponse) {
    setEditingId(transaction.id);
    setDescription(transaction.description);
    setAmount(String(transaction.amount));
    setDate(transaction.date);
    setType(transaction.type);
    setAccountId(String(transaction.accountId));
    setCategoryId(String(transaction.categoryId));
    setPendingDelete(null);
    setErrorMessage("");
    setSuccessMessage("");
    document.getElementById("description")?.focus();
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (busy) return;
    const numericAmount = Number(amount);
    if (!description.trim()) { setErrorMessage("Informe a descrição da transação."); return; }
    if (!amount || !Number.isFinite(numericAmount) || numericAmount <= 0) {
      setErrorMessage("Informe um valor válido."); return;
    }
    if (!date || !accountId || !categoryId) {
      setErrorMessage("Informe a data, a conta e a categoria."); return;
    }
    setSubmitting(true);
    setErrorMessage("");
    setSuccessMessage("");
    try {
      const request = { description: description.trim(), amount: numericAmount, date, type,
        accountId: Number(accountId), categoryId: Number(categoryId) };
      const saved = editingId === null
        ? await createTransaction(request)
        : await updateTransaction(editingId, request);
      setTransactions((current) => mergeTransaction(current, saved, filters));
      const action = editingId === null ? "Transação criada." : "Transação atualizada.";
      setSuccessMessage(matchesTransactionFilters(saved, filters)
        ? action : `${action} Ela não corresponde aos filtros ativos e não aparece nesta lista.`);
      resetForm();
    } catch (error) { handleError(error); }
    finally { setSubmitting(false); }
  }

  async function handleDelete(id: number) {
    if (busy) return;
    setDeleting(true);
    setErrorMessage("");
    setSuccessMessage("");
    try {
      await deleteTransaction(id);
      setTransactions((current) => current.filter((transaction) => transaction.id !== id));
      if (editingId === id) resetForm();
      setPendingDelete(null);
      setSuccessMessage("Transação excluída.");
    } catch (error) { handleError(error); }
    finally { setDeleting(false); }
  }

  async function loadTransactions(nextFilters: TransactionFilters) {
    if (busy) return;
    setLoading(true);
    setErrorMessage("");
    setSuccessMessage("");
    setPendingDelete(null);
    try {
      const items = await findAllTransactions(nextFilters);
      setTransactions(sortTransactions(items));
      setFilters(nextFilters);
      setListFailed(false);
    } catch (error) { setListFailed(true); handleError(error); }
    finally { setLoading(false); }
  }

  function handleFilters(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const data = new FormData(event.currentTarget);
    const startDate = String(data.get("startDate") || "");
    const endDate = String(data.get("endDate") || "");
    if (startDate && endDate && startDate > endDate) {
      setErrorMessage("A data inicial deve ser anterior ou igual à data final.");
      setSuccessMessage("");
      return;
    }
    loadTransactions({
      startDate: startDate || undefined,
      endDate: endDate || undefined,
      type: (data.get("type") || undefined) as TransactionType | undefined,
      accountId: data.get("accountId") ? Number(data.get("accountId")) : undefined,
      categoryId: data.get("categoryId") ? Number(data.get("categoryId")) : undefined,
    });
  }

  return (
    <main>
      <h1>Transações</h1>
      {errorMessage && <p className="error-message" role="alert">{errorMessage}</p>}
      {successMessage && <p role="status">{successMessage}</p>}
      <section className="card">
        <h2>{editingId === null ? "Nova transação" : "Editar transação"}</h2>
        {!loading && (accounts.length === 0 || categories.length === 0) && (
          <p>Cadastre uma conta e uma categoria para registrar transações.</p>
        )}
      <form className="transaction-form" onSubmit={handleSubmit}>
        <div>
          <label htmlFor="description">Descrição</label>

          <input
            id="description"
            name="description"
            maxLength={255}
            type="text"
            value={description}
            onChange={(event) => setDescription(event.target.value)}
            required
            disabled={busy}
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
            disabled={busy}
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
            disabled={busy}
          />
        </div>

        <div>
          <label htmlFor="type">Tipo</label>

          <select
            id="type"
            name="type"
            disabled={busy}
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
            disabled={busy}
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
            disabled={busy}
          >
            <option value="">Selecione uma categoria</option>

            {categories.map((category) => (
              <option key={category.id} value={category.id}>
                {category.name}
              </option>
            ))}
          </select>
        </div>

        <button type="submit" disabled={busy || accounts.length === 0 || categories.length === 0}>
          {submitting ? "Salvando..." : editingId === null ? "Criar transação" : "Salvar alterações"}
        </button>
        {editingId !== null && (
          <button type="button" disabled={busy} onClick={resetForm}>Cancelar edição</button>
        )}
      </form>

      </section>
      <section className="card">
        <h2>Filtros</h2>
        <form className="transaction-form" onSubmit={handleFilters} onReset={() => loadTransactions({})}>
          <div><label htmlFor="filter-start">Data inicial</label><input id="filter-start" name="startDate" type="date" disabled={busy} /></div>
          <div><label htmlFor="filter-end">Data final</label><input id="filter-end" name="endDate" type="date" disabled={busy} /></div>
          <div>
            <label htmlFor="filter-type">Tipo de transação</label>
            <select id="filter-type" name="type" disabled={busy}>
              <option value="">Todos os tipos</option><option value="INCOME">Receita</option><option value="EXPENSE">Despesa</option>
            </select>
          </div>
          <div>
            <label htmlFor="filter-account">Conta financeira</label>
            <select id="filter-account" name="accountId" disabled={busy}>
              <option value="">Todas as contas</option>
              {accounts.map((account) => <option key={account.id} value={account.id}>{account.name}</option>)}
            </select>
          </div>
          <div>
            <label htmlFor="filter-category">Categoria da transação</label>
            <select id="filter-category" name="categoryId" disabled={busy}>
              <option value="">Todas as categorias</option>
              {categories.map((category) => <option key={category.id} value={category.id}>{category.name}</option>)}
            </select>
          </div>
          <button type="submit" disabled={busy}>Aplicar filtros</button>
          <button type="reset" disabled={busy}>Limpar filtros</button>
        </form>
      </section>
      <section className="card" aria-busy={loading}>
        <h2>Transações cadastradas</h2>
        {loading ? <p role="status">Carregando transações...</p> : listFailed ? (
          <p>Lista indisponível. Tente aplicar os filtros novamente ou recarregue a página.</p>
        ) : transactions.length === 0 ? (
          <p>Nenhuma transação encontrada.</p>
        ) : (
          <ul className="transaction-list">
            {transactions.map((transaction) => (
              <li key={transaction.id} className="transaction-item">
                <div className="transaction-item__header">
                  <strong>{transaction.description}</strong>
                  <strong className={`transaction-amount transaction-amount--${transaction.type === "INCOME" ? "income" : "expense"}`}>
                    {formatCurrency(transaction.amount)}
                  </strong>
                </div>
                <div className="transaction-item__details">
                  <span>{formatDate(transaction.date)}</span>
                  <span>{transaction.type === "INCOME" ? "Receita" : "Despesa"}</span>
                  <span>Conta: {transaction.accountName}</span>
                  <span>Categoria: {transaction.categoryName}</span>
                </div>
                <div className="transaction-actions">
                  {pendingDelete === transaction.id ? (
                    <>
                      <p>Excluir “{transaction.description}”? O saldo será recalculado. Esta ação não pode ser desfeita.</p>
                      <button type="button" disabled={busy} onClick={() => handleDelete(transaction.id)}>
                        {deleting ? "Excluindo..." : "Confirmar exclusão"}
                      </button>
                      <button type="button" disabled={busy} onClick={() => setPendingDelete(null)}>Cancelar exclusão</button>
                    </>
                  ) : (
                    <>
                      <button type="button" disabled={busy} onClick={() => startEditing(transaction)} aria-label={`Editar ${transaction.description}`}>Editar</button>
                      <button type="button" disabled={busy} onClick={() => { setPendingDelete(transaction.id); setErrorMessage(""); setSuccessMessage(""); }} aria-label={`Excluir ${transaction.description}`}>Excluir</button>
                    </>
                  )}
                </div>
              </li>
            ))}
          </ul>
        )}
      </section>
    </main>
  );
}
