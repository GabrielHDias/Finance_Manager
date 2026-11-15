"use client";

import { sign, validBalance, decimal, cents } from "@/lib/money";

import { FormEvent, useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import ResourceActions from "@/components/resource-actions";
import { ApiError } from "@/lib/api-error";
import { formatCurrency } from "@/lib/formatters";
import {
  createAccount,
  deleteAccount,
  findAllAccounts,
  getAccountBalance,
  getAccountBalances,
  renameAccount,
} from "@/services/account-service";
import type { AccountResponse } from "@/types/account";

export default function AccountsPage() {
  const router = useRouter();
  const [accounts, setAccounts] = useState<AccountResponse[]>([]);
  const [balances, setBalances] = useState<Record<number, string>>({});
  const [balanceErrors, setBalanceErrors] = useState<Record<number, boolean>>({});
  const [name, setName] = useState("");
  const [openingBalance, setOpeningBalance] = useState("");
  const [editingId, setEditingId] = useState<number | null>(null);
  const [pendingDelete, setPendingDelete] = useState<number | null>(null);
  const [deletingId, setDeletingId] = useState<number | null>(null);
  const [loading, setLoading] = useState(true);
  const [listLoadFailed, setListLoadFailed] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [errorMessage, setErrorMessage] = useState("");
  const [deleteError, setDeleteError] = useState("");
  const [successMessage, setSuccessMessage] = useState("");
  const busy = loading || submitting || deletingId !== null;

  useEffect(() => {
    let cancelled = false;

    async function loadAccounts() {
      try {
        const response = await findAllAccounts();
        if (cancelled) return;
        setAccounts(response);
        setLoading(false);
        try {
          const values = await getAccountBalances();
          if (!cancelled) setBalances(Object.fromEntries(values.map(item => [item.accountId, item.balance])));
        } catch (error) {
          if (cancelled) return;
          if (error instanceof ApiError && error.status === 401) { router.replace("/login"); return; }
          setBalanceErrors(Object.fromEntries(response.map(item => [item.id, true])));
        }
      } catch (error) {
        if (cancelled) return;
        setListLoadFailed(true);
        if (error instanceof ApiError && error.status === 401) {
          router.replace("/login");
          return;
        }
        setErrorMessage(error instanceof ApiError ? error.message : "Não foi possível carregar as contas.");
      } finally {
        if (!cancelled) setLoading(false);
      }
    }

    loadAccounts();
    return () => { cancelled = true; };
  }, [router]);

  function handleError(error: unknown, onError = setErrorMessage) {
    if (error instanceof ApiError && error.status === 401) {
      router.replace("/login");
      return;
    }
    onError(error instanceof ApiError ? error.message : "Não foi possível completar a operação. Tente novamente.");
  }

  function resetForm() {
    setEditingId(null);
    setName("");
    setOpeningBalance("");
  }

  function startEditing(item: AccountResponse) {
    setEditingId(item.id);
    setName(item.name);
    setOpeningBalance(item.openingBalance);
    setPendingDelete(null);
    setDeleteError("");
    setErrorMessage("");
    setSuccessMessage("");
    document.getElementById("account-name")?.focus();
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (busy) return;
    const trimmedName = name.trim();
    if (!trimmedName) {
      setErrorMessage("Informe o nome da conta.");
      return;
    }
    const balanceValue = openingBalance.trim() === "" ? "0.00" : openingBalance.trim();
    if (!validBalance(balanceValue)) {
      setErrorMessage("Informe um saldo inicial com até 17 dígitos inteiros e 2 casas decimais. Zero e valores negativos são aceitos.");
      return;
    }

    setSubmitting(true);
    setErrorMessage("");
    setSuccessMessage("");
    try {
      const wasEditing = editingId !== null;
      const request = { name: trimmedName, openingBalance: decimal(cents(balanceValue)) };
      const saved = wasEditing
        ? await renameAccount(editingId, request)
        : await createAccount(request);
      setAccounts((current) => wasEditing
        ? current.map((item) => item.id === saved.id ? saved : item)
        : [...current, saved]);
      resetForm();
      setSuccessMessage(wasEditing ? "Conta atualizada." : "Conta criada.");
      if (!wasEditing) await loadAccountBalance(saved.id);
    } catch (error) {
      handleError(error);
    } finally {
      setSubmitting(false);
    }
  }

  async function handleDelete(id: number) {
    if (busy) return;
    setDeletingId(id);
    setDeleteError("");
    setSuccessMessage("");
    try {
      await deleteAccount(id);
      setAccounts((current) => current.filter((item) => item.id !== id));
      setBalances((current) => {
        const next = { ...current };
        delete next[id];
        return next;
      });
      setBalanceErrors((current) => {
        const next = { ...current };
        delete next[id];
        return next;
      });
      if (editingId === id) resetForm();
      setPendingDelete(null);
      setSuccessMessage("Conta excluída.");
    } catch (error) {
      handleError(error, setDeleteError);
    } finally {
      setDeletingId(null);
    }
  }

  async function loadAccountBalance(accountId: number) {
    setBalanceErrors((current) => ({ ...current, [accountId]: false }));
    try {
      const balance = await getAccountBalance(accountId);
      setBalances((current) => ({ ...current, [accountId]: balance.balance }));
    } catch (error) {
      if (error instanceof ApiError && error.status === 401) {
        router.replace("/login");
        return;
      }
      setBalanceErrors((current) => ({ ...current, [accountId]: true }));
    }
  }

  if (loading) return <p>Carregando contas...</p>;

  return (
    <main>
      <h1>Contas financeiras</h1>
      {successMessage && <p className="success-message" role="status">{successMessage}</p>}
      <section className="card">
        <h2>{editingId === null ? "Nova conta" : "Editar conta"}</h2>
        <form className="simple-form" onSubmit={handleSubmit}>
          <div>
            <label htmlFor="account-name">Nome</label>
            <input
              id="account-name"
              name="name"
              type="text"
              value={name}
              onChange={(event) => setName(event.target.value)}
              maxLength={100}
              disabled={busy}
              required
            />
          </div>
          <div>
            <label htmlFor="account-opening-balance">Saldo inicial</label>
            <input
              id="account-opening-balance"
              className="amount-input"
              name="openingBalance"
              inputMode="decimal"
              value={openingBalance}
              onChange={(event) => setOpeningBalance(event.target.value)}
              placeholder="0.00"
              disabled={busy}
            />
          </div>
          <button type="submit" disabled={busy}>
            {submitting ? "Salvando..." : editingId === null ? "Criar conta" : "Salvar alterações"}
          </button>
          {editingId !== null && (
            <button className="button--secondary" type="button" disabled={busy} onClick={() => {
              resetForm();
              setErrorMessage("");
            }}>
              Cancelar edição
            </button>
          )}
        </form>
        {errorMessage && <p className="error-message" role="alert">{errorMessage}</p>}
      </section>
      <section className="card">
        <h2>Contas cadastradas</h2>
        {listLoadFailed && (
          <p className="error-message" role="alert">
            Não foi possível carregar a lista completa de contas. Recarregue a página para tentar novamente.
          </p>
        )}
        {accounts.length === 0 && !listLoadFailed ? (
          <p>Nenhuma conta cadastrada.</p>
        ) : (
          <ul className="data-list resource-list">
            {accounts.map((item) => (
              <li key={item.id}>
                <div className="resource-row">
                  <strong className="resource-name">{item.name}</strong>
                  <span>Saldo inicial: {formatCurrency(item.openingBalance)}</span>
                  <div aria-live="polite">
                    {balanceErrors[item.id] ? (
                      <>
                        <span>Saldo indisponível. </span>
                        <button
                          className="button--secondary" type="button"
                          disabled={busy}
                          onClick={() => loadAccountBalance(item.id)}
                          aria-label={`Tentar carregar saldo de ${item.name} novamente`}
                        >
                          Tentar novamente
                        </button>
                      </>
                    ) : balances[item.id] !== undefined ? (
                      <strong className={`account-balance${sign(balances[item.id]) > 0 ? " financial-value--income" : sign(balances[item.id]) < 0 ? " financial-value--expense" : ""}`}>{formatCurrency(balances[item.id])}</strong>
                    ) : <span>Carregando saldo...</span>}
                  </div>
                </div>
                <ResourceActions
                  kind="conta"
                  name={item.name}
                  confirming={pendingDelete === item.id}
                  deleting={deletingId === item.id}
                  disabled={busy}
                  errorMessage={pendingDelete === item.id ? deleteError : ""}
                  onEdit={() => startEditing(item)}
                  onRequestDelete={() => {
                    setPendingDelete(item.id);
                    setDeleteError("");
                    setErrorMessage("");
                    setSuccessMessage("");
                  }}
                  onConfirmDelete={() => handleDelete(item.id)}
                  onCancelDelete={() => {
                    setPendingDelete(null);
                    setDeleteError("");
                  }}
                />
              </li>
            ))}
          </ul>
        )}
      </section>
    </main>
  );
}
