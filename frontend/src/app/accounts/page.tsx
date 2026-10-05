"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { ApiError } from "@/lib/api-error";
import { findAllAccounts } from "@/services/account-service";
import type { AccountResponse } from "@/types/account";

export default function AccountsPage() {
  const router = useRouter();

  const [accounts, setAccounts] = useState<AccountResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [errorMessage, setErrorMessage] = useState("");

  useEffect(() => {
    async function loadAccounts() {
      try {
        const response = await findAllAccounts();

        setAccounts(response);
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

  if (loading) {
    return <p>Carregando contas...</p>;
  }

  if (errorMessage) {
    return <p>{errorMessage}</p>;
  }

  return (
    <main>
      <h1>Contas</h1>

      {accounts.length === 0 ? (
        <p>Nenhuma conta cadastrada.</p>
      ) : (
        <ul>
          {accounts.map((account) => (
            <li key={account.id}>{account.name}</li>
          ))}
        </ul>
      )}
    </main>
  );
}