"use client";

import { FormEvent, useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import ResourceActions from "@/components/resource-actions";
import { ApiError } from "@/lib/api-error";
import { createTag, deleteTag, findAllTags, renameTag } from "@/services/tag-service";
import type { TagResponse } from "@/types/tag";

export default function TagsPage() {
  const router = useRouter();
  const [tags, setTags] = useState<TagResponse[]>([]);
  const [name, setName] = useState("");
  const [editingId, setEditingId] = useState<number | null>(null);
  const [pendingDelete, setPendingDelete] = useState<number | null>(null);
  const [deletingId, setDeletingId] = useState<number | null>(null);
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [errorMessage, setErrorMessage] = useState("");
  const [successMessage, setSuccessMessage] = useState("");
  const busy = loading || submitting || deletingId !== null;

  useEffect(() => {
    let cancelled = false;
    findAllTags().then(response => { if (!cancelled) setTags(response); }).catch(error => {
      if (cancelled) return;
      if (error instanceof ApiError && error.status === 401) { router.replace("/login"); return; }
      setErrorMessage(error instanceof ApiError ? error.message : "Não foi possível carregar as etiquetas.");
    }).finally(() => { if (!cancelled) setLoading(false); });
    return () => { cancelled = true; };
  }, [router]);

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (busy) return;
    const trimmed = name.trim();
    if (!trimmed) { setErrorMessage("Informe o nome da etiqueta."); return; }
    setSubmitting(true);
    setErrorMessage("");
    setSuccessMessage("");
    try {
      const saved = editingId === null ? await createTag(trimmed) : await renameTag(editingId, trimmed);
      setTags(current => editingId === null ? [...current, saved].sort((a, b) => a.name.localeCompare(b.name, "pt-BR")) : current.map(item => item.id === saved.id ? saved : item));
      setEditingId(null);
      setName("");
      setSuccessMessage(editingId === null ? "Etiqueta criada." : "Etiqueta atualizada.");
    } catch (error) {
      if (error instanceof ApiError && error.status === 401) { router.replace("/login"); return; }
      setErrorMessage(error instanceof ApiError ? error.message : "Não foi possível salvar a etiqueta.");
    } finally { setSubmitting(false); }
  }

  async function handleDelete(id: number) {
    setDeletingId(id);
    try {
      await deleteTag(id);
      setTags(current => current.filter(item => item.id !== id));
      setPendingDelete(null);
      if (editingId === id) { setEditingId(null); setName(""); }
      setSuccessMessage("Etiqueta excluída. As transações foram mantidas.");
    } catch (error) {
      if (error instanceof ApiError && error.status === 401) { router.replace("/login"); return; }
      setErrorMessage(error instanceof ApiError ? error.message : "Não foi possível excluir a etiqueta.");
    } finally { setDeletingId(null); }
  }

  if (loading) return <p>Carregando etiquetas...</p>;
  return <main>
    <h1>Etiquetas</h1>
    <p>Etiquetas são uma dimensão extra das transações. Uma movimentação pode ter várias, sem substituir a categoria.</p>
    {successMessage && <p className="success-message" role="status">{successMessage}</p>}
    <section className="card">
      <h2>{editingId === null ? "Nova etiqueta" : "Editar etiqueta"}</h2>
      <form className="simple-form" onSubmit={handleSubmit}>
        <div>
          <label htmlFor="tag-name">Nome</label>
          <input id="tag-name" value={name} onChange={event => setName(event.target.value)} maxLength={40} required disabled={busy} />
        </div>
        <button type="submit" disabled={busy}>{submitting ? "Salvando..." : editingId === null ? "Criar etiqueta" : "Salvar alterações"}</button>
        {editingId !== null && <button className="button--secondary" type="button" disabled={busy} onClick={() => { setEditingId(null); setName(""); }}>Cancelar edição</button>}
      </form>
      {errorMessage && <p className="error-message" role="alert">{errorMessage}</p>}
    </section>
    <section className="card">
      <h2>Etiquetas cadastradas</h2>
      {tags.length === 0 ? <p>Nenhuma etiqueta cadastrada.</p> : <ul className="data-list resource-list">
        {tags.map(item => <li key={item.id}>
          <strong className="resource-name">{item.name}</strong>
          <ResourceActions kind="etiqueta" name={item.name} confirming={pendingDelete === item.id} deleting={deletingId === item.id} disabled={busy} errorMessage=""
            onEdit={() => { setEditingId(item.id); setName(item.name); setPendingDelete(null); }}
            onRequestDelete={() => setPendingDelete(item.id)} onConfirmDelete={() => handleDelete(item.id)} onCancelDelete={() => setPendingDelete(null)} />
        </li>)}
      </ul>}
    </section>
  </main>;
}
