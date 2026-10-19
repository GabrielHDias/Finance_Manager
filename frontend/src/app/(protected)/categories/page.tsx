"use client";

import { FormEvent, useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import ResourceActions from "@/components/resource-actions";
import { ApiError } from "@/lib/api-error";
import {
  createCategory,
  deleteCategory,
  findAllCategories,
  renameCategory,
} from "@/services/category-service";
import type { CategoryResponse } from "@/types/category";

export default function CategoriesPage() {
  const router = useRouter();
  const [categories, setCategories] = useState<CategoryResponse[]>([]);
  const [name, setName] = useState("");
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

    async function loadCategories() {
      try {
        const response = await findAllCategories();
        if (cancelled) return;
        setCategories(response);
      } catch (error) {
        if (cancelled) return;
        setListLoadFailed(true);
        if (error instanceof ApiError && error.status === 401) {
          router.replace("/login");
          return;
        }
        setErrorMessage(error instanceof ApiError ? error.message : "Não foi possível carregar as categorias.");
      } finally {
        if (!cancelled) setLoading(false);
      }
    }

    loadCategories();
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
  }

  function startEditing(item: CategoryResponse) {
    setEditingId(item.id);
    setName(item.name);
    setPendingDelete(null);
    setDeleteError("");
    setErrorMessage("");
    setSuccessMessage("");
    document.getElementById("category-name")?.focus();
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (busy) return;
    const trimmedName = name.trim();
    if (!trimmedName) {
      setErrorMessage("Informe o nome da categoria.");
      return;
    }

    setSubmitting(true);
    setErrorMessage("");
    setSuccessMessage("");
    try {
      const wasEditing = editingId !== null;
      const saved = wasEditing
        ? await renameCategory(editingId, { name: trimmedName })
        : await createCategory({ name: trimmedName });
      setCategories((current) => wasEditing
        ? current.map((item) => item.id === saved.id ? saved : item)
        : [...current, saved]);
      resetForm();
      setSuccessMessage(wasEditing ? "Categoria atualizada." : "Categoria criada.");
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
      await deleteCategory(id);
      setCategories((current) => current.filter((item) => item.id !== id));
      if (editingId === id) resetForm();
      setPendingDelete(null);
      setSuccessMessage("Categoria excluída.");
    } catch (error) {
      handleError(error, setDeleteError);
    } finally {
      setDeletingId(null);
    }
  }

  if (loading) return <p>Carregando categorias...</p>;

  return (
    <main>
      <h1>Categorias</h1>
      {successMessage && <p className="success-message" role="status">{successMessage}</p>}
      <section className="card">
        <h2>{editingId === null ? "Nova categoria" : "Editar categoria"}</h2>
        <form className="simple-form" onSubmit={handleSubmit}>
          <div>
            <label htmlFor="category-name">Nome</label>
            <input
              id="category-name"
              name="name"
              type="text"
              value={name}
              onChange={(event) => setName(event.target.value)}
              maxLength={100}
              disabled={busy}
              required
            />
          </div>
          <button type="submit" disabled={busy}>
            {submitting ? "Salvando..." : editingId === null ? "Criar categoria" : "Salvar alterações"}
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
        <h2>Categorias cadastradas</h2>
        {listLoadFailed && (
          <p className="error-message" role="alert">
            Não foi possível carregar a lista completa de categorias. Recarregue a página para tentar novamente.
          </p>
        )}
        {categories.length === 0 && !listLoadFailed ? (
          <p>Nenhuma categoria cadastrada.</p>
        ) : (
          <ul className="data-list resource-list">
            {categories.map((item) => (
              <li key={item.id}>
                <div className="resource-row">
                  <strong className="resource-name">{item.name}</strong>
                </div>
                <ResourceActions
                  kind="categoria"
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
