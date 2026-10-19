"use client";

import { FormEvent, useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { ApiError } from "@/lib/api-error";
import { formatCurrency } from "@/lib/formatters";
import { formatDate, parseDisplayDate } from "@/lib/dates";
import DateInput from "@/components/date-input";
import { matchesTransactionFilters, sortTransactions } from "@/lib/transaction-list";
import { downloadTransactionCsv } from "@/lib/transaction-csv";
import { findAllAccounts } from "@/services/account-service";
import { findAllCategories } from "@/services/category-service";
import { createTransaction, deleteTransaction, findAllTransactions, findTransactionPage, updateTransaction } from "@/services/transaction-service";
import type { AccountResponse } from "@/types/account";
import type { CategoryResponse } from "@/types/category";
import type { TransactionFilters, TransactionResponse, TransactionType } from "@/types/transaction";

export default function TransactionsPage() {
  const router = useRouter();
  const [transactions, setTransactions] = useState<TransactionResponse[]>([]);
  const [pagination, setPagination] = useState({ page: 0, size: 10, totalElements: 0, totalPages: 0 });
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
  const [exporting, setExporting] = useState(false);
  const [listFailed, setListFailed] = useState(false);
  const [errorMessage, setErrorMessage] = useState("");
  const [successMessage, setSuccessMessage] = useState("");
  const busy = loading || submitting || deleting || exporting;

  useEffect(() => {
    let cancelled = false;
    async function loadPageData() {
      try {
        const [items, accountsResponse, categoriesResponse] = await Promise.all([
          findTransactionPage(), findAllAccounts(), findAllCategories(),
        ]);
        if (cancelled) return;
        setTransactions(items.content);
        setPagination({ page: items.page, size: items.size, totalElements: items.totalElements, totalPages: items.totalPages });
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
    setDate(formatDate(transaction.date));
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
    const apiDate = parseDisplayDate(date);
    if (!apiDate) {
      setErrorMessage("Informe uma data válida no formato DD/MM/YYYY."); return;
    }
    setSubmitting(true);
    setErrorMessage("");
    setSuccessMessage("");
    try {
      const request = { description: description.trim(), amount: numericAmount, date: apiDate, type,
        accountId: Number(accountId), categoryId: Number(categoryId) };
      const saved = editingId === null
        ? await createTransaction(request)
        : await updateTransaction(editingId, request);
      const action = editingId === null ? "Transação criada." : "Transação atualizada.";
      resetForm();
      await refreshAfterMutation(matchesTransactionFilters(saved, filters)
        ? action : `${action} Ela não corresponde aos filtros ativos e não aparece nesta lista.`, 0);
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
      if (editingId === id) resetForm();
      setPendingDelete(null);
      await refreshAfterMutation("Transação excluída.", pagination.page);
    } catch (error) { handleError(error); }
    finally { setDeleting(false); }
  }

  async function refreshPage(nextFilters: TransactionFilters, page: number, size: number) {
    let result = await findTransactionPage(nextFilters, page, size);
    // Removing the last row of a page (including in another session) can shrink the page count.
    if (result.page > 0 && result.page >= result.totalPages) {
      result = await findTransactionPage(nextFilters, Math.max(result.totalPages - 1, 0), size);
    }
    setTransactions(result.content);
    setPagination({ page: result.page, size: result.size, totalElements: result.totalElements, totalPages: result.totalPages });
    setFilters(nextFilters);
    setListFailed(false);
  }

  async function refreshAfterMutation(message: string, page: number) {
    setSuccessMessage(message);
    try {
      await refreshPage(filters, page, pagination.size);
    } catch (error) {
      setListFailed(true);
      setTransactions([]);
      handleError(error);
    }
  }

  async function loadTransactions(nextFilters: TransactionFilters, page = 0, size = pagination.size) {
    if (busy) return;
    setLoading(true);
    setErrorMessage("");
    setSuccessMessage("");
    setPendingDelete(null);
    try {
      await refreshPage(nextFilters, page, size);
    } catch (error) { setListFailed(true); handleError(error); }
    finally { setLoading(false); }
  }

  async function handleExport() {
    if (busy || listFailed || transactions.length === 0) return;
    setExporting(true);
    setErrorMessage("");
    setSuccessMessage("");
    try {
      const items = sortTransactions(await findAllTransactions(filters));
      if (items.length === 0) {
        setSuccessMessage("Nenhuma transação encontrada para exportar com os filtros aplicados.");
        return;
      }
      downloadTransactionCsv(items);
      setSuccessMessage(`CSV gerado com ${items.length} ${items.length === 1 ? "transação" : "transações"}.`);
    } catch (error) {
      handleError(error);
    } finally {
      setExporting(false);
    }
  }

  function handleFilters(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const data = new FormData(event.currentTarget);
    const startValue = String(data.get("startDate") || "");
    const endValue = String(data.get("endDate") || "");
    const startDate = startValue ? parseDisplayDate(startValue) : undefined;
    const endDate = endValue ? parseDisplayDate(endValue) : undefined;
    if (startDate === null || endDate === null) {
      setErrorMessage("Informe datas válidas no formato DD/MM/YYYY.");
      setSuccessMessage("");
      return;
    }
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
      {successMessage && <p className="success-message" role="status">{successMessage}</p>}
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
            className="amount-input"
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

          <DateInput
            id="date"
            name="date"
            value={date}
            onValueChange={setDate}
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
          <button className="button--secondary" type="button" disabled={busy} onClick={resetForm}>Cancelar edição</button>
        )}
      </form>

      </section>
      <section className="card">
        <h2>Filtros</h2>
        <form className="transaction-form" onSubmit={handleFilters} onReset={() => loadTransactions({})}>
          <div><label htmlFor="filter-start">Data inicial</label><DateInput id="filter-start" name="startDate" disabled={busy} /></div>
          <div><label htmlFor="filter-end">Data final</label><DateInput id="filter-end" name="endDate" disabled={busy} /></div>
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
          <button className="button--secondary" type="reset" disabled={busy}>Limpar filtros</button>
        </form>
      </section>
      <section className="card" aria-busy={busy}>
        <h2>Transações cadastradas</h2>
        <nav className="pagination-controls" aria-label="Paginação de transações">
          <div>
            <label htmlFor="transaction-page-size">Transações por página</label>
            <select id="transaction-page-size" value={pagination.size} disabled={busy || listFailed} onChange={event => loadTransactions(filters, 0, Number(event.target.value))}>
              <option value="10">10</option><option value="25">25</option><option value="50">50</option>
            </select>
          </div>
          <button className="button--secondary" type="button" disabled={busy || listFailed || pagination.page === 0} onClick={() => loadTransactions(filters, pagination.page - 1)}>Página anterior</button>
          <button className="button--secondary" type="button" disabled={busy || listFailed || pagination.page + 1 >= pagination.totalPages} onClick={() => loadTransactions(filters, pagination.page + 1)}>Próxima página</button>
        </nav>
        {!loading && !listFailed && <p role="status">{pagination.totalElements === 0 ? "0 transações." : `Mostrando ${pagination.page * pagination.size + 1} a ${pagination.page * pagination.size + transactions.length} de ${pagination.totalElements} transações. Página ${pagination.page + 1} de ${pagination.totalPages}.`}</p>}
        <p id="export-help">Exporte em CSV usando os filtros aplicados à lista. Após alterar os campos, clique em Aplicar filtros antes de exportar.</p>
        <button
          className="button--secondary"
          type="button"
          disabled={busy || listFailed || transactions.length === 0}
          aria-describedby="export-help"
          onClick={handleExport}
        >
          {exporting ? "Exportando..." : "Exportar CSV"}
        </button>
        <p>O relatório inclui data, descrição, valor, tipo, conta e categoria de todas as páginas.</p>
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
                      <button className="button--danger" type="button" disabled={busy} onClick={() => handleDelete(transaction.id)}>
                        {deleting ? "Excluindo..." : "Confirmar exclusão"}
                      </button>
                      <button className="button--secondary" type="button" disabled={busy} onClick={() => setPendingDelete(null)}>Cancelar exclusão</button>
                    </>
                  ) : (
                    <>
                      <button className="button--secondary" type="button" disabled={busy} onClick={() => startEditing(transaction)} aria-label={`Editar ${transaction.description}`}>Editar</button>
                      <button className="button--danger-quiet" type="button" disabled={busy} onClick={() => { setPendingDelete(transaction.id); setErrorMessage(""); setSuccessMessage(""); }} aria-label={`Excluir ${transaction.description}`}>Excluir</button>
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
