"use client";

import { FormEvent, useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { ApiError } from "@/lib/api-error";
import { formatCurrency } from "@/lib/formatters";
import {
  createAccount,
  findAllAccounts,
  getAccountBalance,
} from "@/services/account-service";
import type {
  AccountBalanceResponse,
  AccountResponse,
} from "@/types/account";

export default function AccountsPage() {
  const router = useRouter();

  const [accounts, setAccounts] = useState<AccountResponse[]>([]);
  const [balances, setBalances] = useState<AccountBalanceResponse[]>([]);
  const [name, setName] = useState("");
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [errorMessage, setErrorMessage] = useState("");

  useEffect(() => {
    async function loadAccounts() {
      try {
        const accountsResponse = await findAllAccounts();

        const balancesResponse = await Promise.all(
          accountsResponse.map((account) => getAccountBalance(account.id)),
        );

        setAccounts(accountsResponse);
        setBalances(balancesResponse);
      } catch (error) {
        if (error instanceof ApiError) {
          if (error.status === 401) {
            router.replace("/login");
            return;
          }

          setErrorMessage(error.message);
          return;
        }

        setErrorMessage("Não foi possível carregar as contas.");
      } finally {
        setLoading(false);
      }
    }

    loadAccounts();
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

    const trimmedName = name.trim();

    if (!trimmedName) {
      setErrorMessage("Informe o nome da conta.");
      return;
    }

    setSubmitting(true);
    setErrorMessage("");

    try {
      const createdAccount = await createAccount({
        name: trimmedName,
      });

      const createdAccountBalance = await getAccountBalance(
        createdAccount.id,
      );

      setAccounts((currentAccounts) => [
        ...currentAccounts,
        createdAccount,
      ]);

      setBalances((currentBalances) => [
        ...currentBalances,
        createdAccountBalance,
      ]);

      setName("");
    } catch (error) {
      handleError(error);
    } finally {
      setSubmitting(false);
    }
  }

  function findAccountBalance(accountId: number): number {
    const accountBalance = balances.find(
      (balance) => balance.accountId === accountId,
    );

    return accountBalance?.balance ?? 0;
  }

  if (loading) {
    return <p>Carregando contas...</p>;
  }

  return (
    <main>
      <h1>Contas financeiras</h1>

      <section className="card">
        <h2>Nova conta</h2>

        <form className="simple-form" onSubmit={handleSubmit}>
          <div>
            <label htmlFor="account-name">Nome</label>

            <input
              id="account-name"
              name="name"
              type="text"
              value={name}
              onChange={(event) => setName(event.target.value)}
              required
            />
          </div>

          <button type="submit" disabled={submitting}>
            {submitting ? "Salvando..." : "Criar conta"}
          </button>
        </form>

        {errorMessage && (
          <p className="error-message">{errorMessage}</p>
        )}
      </section>

      <section className="card">
        <h2>Contas cadastradas</h2>

        {accounts.length === 0 ? (
          <p>Nenhuma conta cadastrada.</p>
        ) : (
          <ul className="data-list">
            {accounts.map((account) => (
              <li key={account.id}>
                <span>{account.name}</span>
                <strong>
                  {formatCurrency(findAccountBalance(account.id))}
                </strong>
              </li>
            ))}
          </ul>
        )}
      </section>
    </main>
  );
}