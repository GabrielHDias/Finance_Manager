"use client";

import { useEffect, useState, type FormEvent } from "react";
import { useRouter } from "next/navigation";
import { ApiError } from "@/lib/api-error";
import { validInput, ratio, sign } from "@/lib/money";
import { formatCurrency } from "@/lib/formatters";
import { formatDate } from "@/lib/dates";
import { MONTH_NAMES, monthLabel, monthPeriod } from "@/lib/monthly-summary";
import { findAllCategories } from "@/services/category-service";
import { deleteBudget, findBudgets, saveBudget } from "@/services/budget-service";
import type { BudgetPeriod, BudgetSummary } from "@/types/budget";
import type { CategoryResponse } from "@/types/category";

interface BudgetData { period: BudgetPeriod; budgets: BudgetSummary[]; categories: CategoryResponse[] }

export default function BudgetsPage() {
  const router = useRouter();
  const [period, setPeriod] = useState<BudgetPeriod | null>(null);
  const [revision, setRevision] = useState(0);
  const [data, setData] = useState<BudgetData | null>(null);
  const [month, setMonth] = useState<string | null>(null);
  const [year, setYear] = useState<string | null>(null);
  const [categoryId, setCategoryId] = useState("");
  const [limit, setLimit] = useState("");
  const [pendingDelete, setPendingDelete] = useState<number | null>(null);
  const [loading, setLoading] = useState(true);
  const [working, setWorking] = useState(false);
  const [loadFailed, setLoadFailed] = useState(false);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");
  const busy = loading || working;

  useEffect(() => {
    let cancelled = false;
    async function load() {
      try {
        const now = new Date();
        const selected = period ?? { year: now.getFullYear(), month: now.getMonth() + 1 };
        const [budgets, categories] = await Promise.all([findBudgets(selected), findAllCategories()]);
        if (cancelled) return;
        setData({ period: selected, budgets, categories });
        setLoadFailed(false);
      } catch (failure) {
        if (cancelled) return;
        setLoadFailed(true);
        if (failure instanceof ApiError && failure.status === 401) { router.replace("/login"); return; }
        setError(failure instanceof ApiError ? failure.message : "Não foi possível carregar os orçamentos. Tente novamente.");
      } finally {
        if (!cancelled) setLoading(false);
      }
    }
    load();
    return () => { cancelled = true; };
  }, [period, revision, router]);

  function handleError(failure: unknown) {
    if (failure instanceof ApiError && failure.status === 401) { router.replace("/login"); return; }
    setError(failure instanceof ApiError ? failure.message : "Não foi possível concluir a operação. Tente novamente.");
  }

  function choosePeriod(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (busy) return;
    const form = new FormData(event.currentTarget);
    const selected = { year: Number(form.get("year")), month: Number(form.get("month")) };
    if (selected.year < 1 || selected.year > 9999 || selected.month < 1 || selected.month > 12) {
      setError("Informe um mês e ano válidos."); return;
    }
    setError(""); setSuccess(""); setCategoryId(""); setLimit(""); setPendingDelete(null);
    setLoading(true); setPeriod(selected);
  }

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (busy || !data || loadFailed) return;
    const amount = limit;
    if (!categoryId || !limit || !validInput(amount)) {
      setError("Informe a categoria e um limite positivo."); return;
    }
    setWorking(true); setError(""); setSuccess("");
    try {
      const saved = await saveBudget({ ...data.period, categoryId: Number(categoryId), limitAmount: amount });
      setData(current => current && ({ ...current, budgets: [...current.budgets.filter(row => row.id !== saved.id), saved].sort((a, b) => a.categoryName.localeCompare(b.categoryName, "pt-BR")) }));
      setSuccess("Limite mensal salvo."); setCategoryId(""); setLimit(""); setPendingDelete(null);
    } catch (failure) { handleError(failure); }
    finally { setWorking(false); }
  }

  async function remove(id: number) {
    if (busy) return;
    setWorking(true); setError(""); setSuccess("");
    try {
      await deleteBudget(id);
      setData(current => current && ({ ...current, budgets: current.budgets.filter(row => row.id !== id) }));
      setPendingDelete(null); setSuccess("Orçamento excluído. As transações foram mantidas.");
    } catch (failure) { handleError(failure); }
    finally { setWorking(false); }
  }

  const reference = data ? `${String(data.period.year).padStart(4, "0")}-${String(data.period.month).padStart(2, "0")}` : null;
  const range = reference ? monthPeriod(reference) : null;

  return (
    <main>
      <h1>Orçamentos por categoria</h1>
      <p>Defina limites mensais e acompanhe as despesas registradas em todas as suas contas.</p>
      {error && <p className="error-message" role="alert">{error}</p>}
      {success && <p className="success-message" role="status">{success}</p>}
      <section className="card">
        <h2>Mês do orçamento</h2>
        <form className="transaction-form" onSubmit={choosePeriod}>
          <div><label htmlFor="budget-month">Mês de referência</label>
            <select id="budget-month" name="month" value={month ?? (data ? String(data.period.month) : "")} onChange={event => setMonth(event.target.value)} disabled={busy} required>
              <option value="" disabled>Selecione o mês</option>
              {MONTH_NAMES.map((name, index) => <option key={name} value={index + 1}>{name}</option>)}
            </select>
          </div>
          <div><label htmlFor="budget-year">Ano de referência</label>
            <input id="budget-year" name="year" type="text" inputMode="numeric" pattern="[0-9]{4}" maxLength={4} placeholder="YYYY" value={year ?? (data ? String(data.period.year).padStart(4, "0") : "")} onChange={event => setYear(event.target.value)} disabled={busy} required />
          </div>
          <button type="submit" disabled={busy}>Consultar mês</button>
        </form>
      </section>
      <div aria-busy={busy}>
        {loading ? <p role="status">Carregando orçamentos...</p> : loadFailed ? (
          <button className="button--secondary" type="button" onClick={() => { setError(""); setLoading(true); setRevision(current => current + 1); }}>Tentar carregar novamente</button>
        ) : data && reference && range && <>
          <section className="card">
            <h2>Definir limite — {monthLabel(reference)}</h2>
            <p>De {formatDate(range.startDate)} a {formatDate(range.endDate)}. Salvar uma categoria já cadastrada atualiza o limite deste mês.</p>
            {data.categories.length === 0 && <p>Cadastre uma categoria antes de definir um orçamento.</p>}
            <form className="transaction-form" onSubmit={submit}>
              <div><label htmlFor="budget-category">Categoria</label>
                <select id="budget-category" value={categoryId} onChange={event => setCategoryId(event.target.value)} disabled={busy} required>
                  <option value="">Selecione uma categoria</option>
                  {data.categories.map(category => <option key={category.id} value={category.id}>{category.name}</option>)}
                </select>
              </div>
              <div><label htmlFor="budget-limit">Limite mensal</label>
                <input id="budget-limit" className="amount-input" type="number" min="0.01" step="0.01" value={limit} onChange={event => setLimit(event.target.value)} required disabled={busy} />
              </div>
              <button type="submit" disabled={busy || data.categories.length === 0}>{working ? "Salvando..." : "Salvar limite"}</button>
            </form>
          </section>
          <section className="card">
            <h2>Acompanhamento — {monthLabel(reference)}</h2>
            <p>Somente despesas entram no consumo do limite. Receitas não compensam gastos. Transações com datas futuras neste mês também são incluídas.</p>
            {data.budgets.length === 0 ? <p>Nenhum orçamento definido para este mês.</p> : <ul className="budget-grid">
              {data.budgets.map(budget => <li key={budget.id} className="budget-card">
                <h3>{budget.categoryName}</h3>
                <dl>
                  <div><dt>Limite</dt><dd>{formatCurrency(budget.limitAmount)}</dd></div>
                  <div><dt>Gasto no mês</dt><dd>{formatCurrency(budget.spent)}</dd></div>
                  <div><dt>{sign(budget.remaining) < 0 ? "Excedido em" : "Disponível"}</dt><dd className={sign(budget.remaining) < 0 ? "financial-value--expense" : undefined}>{formatCurrency(budget.remaining.replace(/^-/, ""))}</dd></div>
                </dl>
                <progress max={1} value={Math.min(ratio(budget.spent, budget.limitAmount), 1)} aria-label={`Consumo do orçamento de ${budget.categoryName}`} />
                <p className={sign(budget.remaining) <= 0 ? "financial-value--expense" : undefined}>{sign(budget.remaining) < 0 ? "Limite excedido" : sign(budget.remaining) === 0 ? "Limite atingido" : "Dentro do limite"} · {new Intl.NumberFormat("pt-BR", { maximumFractionDigits: 1 }).format(ratio(budget.spent, budget.limitAmount) * 100)}% utilizado</p>
                <div className="resource-actions">
                  {pendingDelete === budget.id ? <>
                    <p>Excluir o orçamento de {budget.categoryName} deste mês? As transações serão mantidas.</p>
                    <button className="button--danger" type="button" disabled={busy} onClick={() => remove(budget.id)}>Confirmar exclusão</button>
                    <button className="button--secondary" type="button" disabled={busy} onClick={() => setPendingDelete(null)}>Cancelar exclusão</button>
                  </> : <>
                    <button className="button--secondary" type="button" disabled={busy} aria-label={`Editar orçamento ${budget.categoryName}`} onClick={() => { setCategoryId(String(budget.categoryId)); setLimit(String(budget.limitAmount)); setError(""); setSuccess(""); document.getElementById("budget-limit")?.focus(); }}>Editar limite</button>
                    <button className="button--danger-quiet" type="button" disabled={busy} aria-label={`Excluir orçamento ${budget.categoryName}`} onClick={() => { setPendingDelete(budget.id); setError(""); setSuccess(""); }}>Excluir orçamento</button>
                  </>}
                </div>
              </li>)}
            </ul>}
          </section>
        </>}
      </div>
    </main>
  );
}
