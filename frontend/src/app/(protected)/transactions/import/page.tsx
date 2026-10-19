"use client";

import { FormEvent, useEffect, useRef, useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { ApiError } from "@/lib/api-error";
import { findAllAccounts } from "@/services/account-service";
import { findAllCategories } from "@/services/category-service";
import { confirmCsv, previewCsv } from "@/services/csv-import-service";
import type { AccountResponse } from "@/types/account";
import type { CategoryResponse } from "@/types/category";
import type { CsvImportPreview, CsvImportResult, CsvImportRow } from "@/types/csv-import";

const labels = { READY: "Pronta para importar", DUPLICATE: "Duplicada — será ignorada", INVALID: "Inválida" };

export default function ImportTransactionsPage() {
  const router = useRouter();
  const fileInput = useRef<HTMLInputElement>(null);
  const [accounts, setAccounts] = useState<AccountResponse[]>([]);
  const [categories, setCategories] = useState<CategoryResponse[]>([]);
  const [accountId, setAccountId] = useState("");
  const [categoryId, setCategoryId] = useState("");
  const [file, setFile] = useState<File | null>(null);
  const [preview, setPreview] = useState<CsvImportPreview | null>(null);
  const [result, setResult] = useState<CsvImportResult | null>(null);
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(true);
  const [loadFailed, setLoadFailed] = useState(false);
  const [attempt, setAttempt] = useState(0);
  const [working, setWorking] = useState<"preview" | "confirm" | null>(null);
  const [filter, setFilter] = useState<"ALL" | CsvImportRow["status"]>("ALL");
  const [visible, setVisible] = useState(50);
  const busy = loading || working !== null;

  useEffect(() => {
    let cancelled = false;
    async function load() {
      try {
        const [accountList, categoryList] = await Promise.all([findAllAccounts(), findAllCategories()]);
        if (cancelled) return;
        setAccounts(accountList);
        setCategories(categoryList);
        setLoadFailed(false);
      } catch (error) {
        if (cancelled) return;
        if (error instanceof ApiError && error.status === 401) { router.replace("/login"); return; }
        setLoadFailed(true);
        setError("Não foi possível carregar contas e categorias. Tente novamente.");
      } finally {
        if (!cancelled) setLoading(false);
      }
    }
    load();
    return () => { cancelled = true; };
  }, [router, attempt]);

  function resetPreview() {
    setPreview(null);
    setResult(null);
    setError("");
    setFilter("ALL");
    setVisible(50);
  }

  function handleError(error: unknown) {
    if (error instanceof ApiError && error.status === 401) { router.replace("/login"); return; }
    setError(error instanceof ApiError ? error.message : "Não foi possível conectar ao servidor. Tente novamente.");
  }

  async function handlePreview(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (busy) return;
    resetPreview();
    if (!file || !accountId || !categoryId) { setError("Selecione o arquivo, a conta e a categoria."); return; }
    if (!file.size) { setError("O arquivo CSV está vazio."); return; }
    if (file.size > 1024 * 1024) { setError("O arquivo deve ter no máximo 1 MiB."); return; }
    setWorking("preview");
    try { setPreview(await previewCsv(file, accountId, categoryId)); }
    catch (error) { handleError(error); }
    finally { setWorking(null); }
  }

  async function handleConfirm() {
    if (busy || !file || !preview || preview.invalid || !preview.ready) return;
    setWorking("confirm");
    setError("");
    try {
      setResult(await confirmCsv(file, accountId, categoryId, preview.hash));
      setPreview(null);
      setFile(null);
      if (fileInput.current) fileInput.current.value = "";
    } catch (error) { handleError(error); }
    finally { setWorking(null); }
  }

  const missingResources = !loading && !loadFailed && (!accounts.length || !categories.length);
  const rows = preview?.rows.filter(row => filter === "ALL" || row.status === filter) ?? [];

  return (
    <main>
      <h1>Importar transações CSV</h1>
      <p>Escolha uma conta e uma categoria para todas as transações do arquivo. Confira a prévia antes de confirmar.</p>
      <section className="card">
        <h2>Preparar o arquivo</h2>
        <p>CSV em UTF-8, até 1 MiB e 1000 registros. Use ponto e vírgula ou vírgula como separador.</p>
        <p>Cabeçalhos: <code>data;descricao;valor;tipo</code>. Datas em AAAA-MM-DD ou DD/MM/AAAA; valores positivos com até duas casas decimais, sem separador de milhar. Tipos: RECEITA ou DESPESA.</p>
        <a className="text-link" href="/modelo-transacoes.csv" download>Baixar modelo CSV</a>
        <p>Duplicados têm a mesma conta, categoria, data, descrição, valor e tipo. Confira se registros iguais representam movimentações diferentes antes de usar esta importação.</p>
      </section>
      {error && <p className="error-message" role="alert">{error}</p>}
      {loading && <p role="status">Carregando contas e categorias...</p>}
      {loadFailed && <button type="button" disabled={busy} onClick={() => {
        setLoading(true); setError(""); setAttempt(current => current + 1);
      }}>Tentar carregar novamente</button>}
      {missingResources && <p>Cadastre uma <Link className="text-link" href="/accounts">conta</Link> e uma <Link className="text-link" href="/categories">categoria</Link> antes de importar.</p>}
      <section className="card">
        <h2>Selecionar dados</h2>
        <form className="transaction-form" onSubmit={handlePreview}>
          <div>
            <label htmlFor="import-account">Conta das transações</label>
            <select id="import-account" value={accountId} required disabled={busy || loadFailed || missingResources} onChange={event => {
              setAccountId(event.target.value); resetPreview();
            }}>
              <option value="">Selecione uma conta</option>
              {accounts.map(account => <option key={account.id} value={account.id}>{account.name}</option>)}
            </select>
          </div>
          <div>
            <label htmlFor="import-category">Categoria das transações</label>
            <select id="import-category" value={categoryId} required disabled={busy || loadFailed || missingResources} onChange={event => {
              setCategoryId(event.target.value); resetPreview();
            }}>
              <option value="">Selecione uma categoria</option>
              {categories.map(category => <option key={category.id} value={category.id}>{category.name}</option>)}
            </select>
          </div>
          <div className="csv-file-field">
            <label htmlFor="import-file">Arquivo CSV</label>
            <input ref={fileInput} id="import-file" type="file" accept=".csv,text/csv" required disabled={busy || loadFailed || missingResources}
              onChange={event => { setFile(event.target.files?.[0] ?? null); resetPreview(); }} />
          </div>
          <button type="submit" disabled={busy || loadFailed || missingResources}>
            {working === "preview" ? "Analisando..." : "Gerar prévia"}
          </button>
        </form>
        {working && <p role="status">{working === "confirm" ? "Importando transações..." : "Validando o arquivo..."}</p>}
      </section>
      {result && <section className="card" role="status">
        <h2>Importação concluída</h2>
        <p>{result.imported} {result.imported === 1 ? "transação importada" : "transações importadas"}. {result.skipped} {result.skipped === 1 ? "registro duplicado ignorado" : "registros duplicados ignorados"}.</p>
        <Link className="text-link" href="/transactions">Ver transações</Link>
        {" · "}<Link className="text-link" href="/dashboard">Ver dashboard</Link>
      </section>}
      {preview && <section className="card csv-preview" aria-busy={working !== null}>
        <h2>Prévia da importação</h2>
        <p>Conta: <strong>{preview.accountName}</strong> · Categoria: <strong>{preview.categoryName}</strong></p>
        <div className="summary-grid" role="status">
          <div><strong>Novas transações</strong><p>{preview.ready}</p></div>
          <div><strong>Duplicadas</strong><p>{preview.duplicates}</p></div>
          <div><strong>Inválidas</strong><p>{preview.invalid}</p></div>
        </div>
        {preview.invalid > 0 ? <p className="error-message" role="alert">Corrija todas as linhas inválidas no arquivo e gere outra prévia. Nenhuma transação será gravada enquanto houver erros.</p>
          : !preview.ready ? <p>Todos os registros já existem ou são repetidos. Não há novas transações para importar.</p>
          : <p>Nada foi gravado. Confirme para importar as linhas novas e ignorar as duplicadas.</p>}
        <div className="transaction-actions">
          <button type="button" disabled={busy || preview.invalid > 0 || !preview.ready} onClick={handleConfirm}>
            {working === "confirm" ? "Importando..." : `Confirmar importação de ${preview.ready} ${preview.ready === 1 ? "transação" : "transações"}`}
          </button>
          <button type="button" disabled={busy} onClick={() => setPreview(null)}>Descartar prévia</button>
        </div>
        <p>Duplicados são verificados novamente na confirmação; se outra importação já tiver gravado algum registro, ele será ignorado.</p>
        <label htmlFor="import-filter">Linhas da prévia</label>
        <select id="import-filter" value={filter} disabled={busy} onChange={event => {
          setFilter(event.target.value as typeof filter); setVisible(50);
        }}>
          <option value="ALL">Todas</option><option value="INVALID">Inválidas</option>
          <option value="DUPLICATE">Duplicadas</option><option value="READY">Prontas para importar</option>
        </select>
        <p>Exibindo {Math.min(visible, rows.length)} de {rows.length} {rows.length === 1 ? "linha" : "linhas"}.</p>
        <ol className="csv-preview-list">
          {rows.slice(0, visible).map(row => <li key={row.line} className={`csv-row csv-row--${row.status.toLowerCase()}`}>
            <h3>Linha {row.line} — {labels[row.status]}</h3>
            <dl>
              <div><dt>Descrição</dt><dd>{row.description || "(vazia)"}</dd></div>
              <div><dt>Data</dt><dd>{row.date || "(vazia)"}</dd></div>
              <div><dt>Valor</dt><dd>{row.amount || "(vazio)"}</dd></div>
              <div><dt>Tipo</dt><dd>{row.type === "INCOME" ? "Receita" : row.type === "EXPENSE" ? "Despesa" : row.type || "(vazio)"}</dd></div>
            </dl>
            {row.errors.length > 0 && <ul>{row.errors.map(message => <li key={message}>{message}</li>)}</ul>}
          </li>)}
        </ol>
        {visible < rows.length && <button type="button" disabled={busy} onClick={() => setVisible(current => current + 50)}>Mostrar mais linhas</button>}
      </section>}
    </main>
  );
}
