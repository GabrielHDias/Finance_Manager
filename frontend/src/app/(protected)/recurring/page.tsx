"use client";

import { FormEvent, useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { ApiError } from "@/lib/api-error";
import { formatCurrency } from "@/lib/formatters";
import { findAllAccounts } from "@/services/account-service";
import { findAllCategories } from "@/services/category-service";
import { createRecurringRule, deleteRecurringRule, findRecurringRules, type RecurringRule } from "@/services/recurring-service";
import type { AccountResponse } from "@/types/account";
import type { CategoryResponse } from "@/types/category";

export default function RecurringPage() {
  const router = useRouter();
  const [rules, setRules] = useState<RecurringRule[]>([]);
  const [accounts, setAccounts] = useState<AccountResponse[]>([]);
  const [categories, setCategories] = useState<CategoryResponse[]>([]);
  const [description, setDescription] = useState("");
  const [amount, setAmount] = useState("");
  const [type, setType] = useState<"INCOME" | "EXPENSE">("EXPENSE");
  const [frequency, setFrequency] = useState<"MONTHLY" | "WEEKLY">("MONTHLY");
  const [startDate, setStartDate] = useState("");
  const [accountId, setAccountId] = useState("");
  const [categoryId, setCategoryId] = useState("");
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    let cancelled = false;
    Promise.all([findRecurringRules(), findAllAccounts(), findAllCategories()]).then(([nextRules, nextAccounts, nextCategories]) => {
      if (cancelled) return;
      setRules(nextRules);
      setAccounts(nextAccounts);
      setCategories(nextCategories);
    }).catch(failure => {
      if (failure instanceof ApiError && failure.status === 401) { router.replace("/login"); return; }
      setError(failure instanceof ApiError ? failure.message : "Não foi possível carregar as recorrências.");
    }).finally(() => { if (!cancelled) setLoading(false); });
    return () => { cancelled = true; };
  }, [router]);

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setError("");
    setSuccess("");
    try {
      const saved = await createRecurringRule({
        description: description.trim(), amount, type, frequency, startDate, accountId: Number(accountId), categoryId: Number(categoryId),
      });
      setRules(current => [...current, saved]);
      setDescription("");
      setAmount("");
      setSuccess("Recorrência criada. Os lançamentos até hoje foram gerados.");
    } catch (failure) {
      if (failure instanceof ApiError && failure.status === 401) { router.replace("/login"); return; }
      setError(failure instanceof ApiError ? failure.message : "Não foi possível criar a recorrência.");
    }
  }

  async function remove(id: number) {
    await deleteRecurringRule(id);
    setRules(current => current.filter(rule => rule.id !== id));
    setSuccess("Recorrência interrompida. Os lançamentos já gerados foram mantidos.");
  }

  if (loading) return <p>Carregando recorrências...</p>;
  const compatible = categories.filter(category => category.type === type);
  return <main>
    <h1>Recorrências</h1>
    <p>Uma receita ou despesa mensal ou semanal gera os lançamentos quando você consulta transações, orçamentos ou o dashboard. O fuso usado é America/Sao_Paulo. Apagar um lançamento gerado não o recria.</p>
    {error && <p className="error-message" role="alert">{error}</p>}
    {success && <p className="success-message" role="status">{success}</p>}
    <section className="card">
      <h2>Nova recorrência</h2>
      <form className="transaction-form" onSubmit={submit}>
        <div><label htmlFor="recurring-description">Descrição</label><input id="recurring-description" value={description} onChange={event => setDescription(event.target.value)} required maxLength={255} /></div>
        <div><label htmlFor="recurring-amount">Valor</label><input id="recurring-amount" className="amount-input" value={amount} onChange={event => setAmount(event.target.value)} required /></div>
        <div><label htmlFor="recurring-type">Tipo</label><select id="recurring-type" value={type} onChange={event => { setType(event.target.value as "INCOME" | "EXPENSE"); setCategoryId(""); }}><option value="EXPENSE">Despesa</option><option value="INCOME">Receita</option></select></div>
        <div><label htmlFor="recurring-frequency">Frequência</label><select id="recurring-frequency" value={frequency} onChange={event => setFrequency(event.target.value as "MONTHLY" | "WEEKLY")}><option value="MONTHLY">Mensal</option><option value="WEEKLY">Semanal</option></select></div>
        <div><label htmlFor="recurring-start">Primeira data</label><input id="recurring-start" type="date" value={startDate} onChange={event => setStartDate(event.target.value)} required /></div>
        <div><label htmlFor="recurring-account">Conta</label><select id="recurring-account" value={accountId} onChange={event => setAccountId(event.target.value)} required><option value="">Selecione</option>{accounts.map(account => <option key={account.id} value={account.id}>{account.name}</option>)}</select></div>
        <div><label htmlFor="recurring-category">Categoria</label><select id="recurring-category" value={categoryId} onChange={event => setCategoryId(event.target.value)} required><option value="">Selecione</option>{compatible.map(category => <option key={category.id} value={category.id}>{category.name}</option>)}</select></div>
        <button type="submit">Criar recorrência</button>
      </form>
    </section>
    <section className="card">
      <h2>Regras ativas</h2>
      {rules.length === 0 ? <p>Nenhuma recorrência ativa.</p> : <ul className="data-list">
        {rules.map(rule => <li key={rule.id}>
          <strong>{rule.description}</strong>
          <span>{formatCurrency(String(rule.amount))} · {rule.frequency === "MONTHLY" ? "Mensal" : "Semanal"} · {rule.accountName} · {rule.categoryName}</span>
          <button className="button--danger-quiet" type="button" onClick={() => remove(rule.id)}>Interromper</button>
        </li>)}
      </ul>}
    </section>
  </main>;
}
