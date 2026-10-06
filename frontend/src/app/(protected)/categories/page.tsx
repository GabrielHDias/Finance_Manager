"use client";

import { FormEvent, useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { ApiError } from "@/lib/api-error";
import {
  createCategory,
  findAllCategories,
} from "@/services/category-service";
import type { CategoryResponse } from "@/types/category";

export default function CategoriesPage() {
  const router = useRouter();

  const [categories, setCategories] = useState<CategoryResponse[]>([]);
  const [name, setName] = useState("");
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [errorMessage, setErrorMessage] = useState("");

  useEffect(() => {
    async function loadCategories() {
      try {
        const response = await findAllCategories();

        setCategories(response);
      } catch (error) {
        if (error instanceof ApiError) {
          if (error.status === 401) {
            router.replace("/login");
            return;
          }

          setErrorMessage(error.message);
          return;
        }

        setErrorMessage("Não foi possível carregar as categorias.");
      } finally {
        setLoading(false);
      }
    }

    loadCategories();
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
      setErrorMessage("Informe o nome da categoria.");
      return;
    }

    setSubmitting(true);
    setErrorMessage("");

    try {
      const createdCategory = await createCategory({
        name: trimmedName,
      });

      setCategories((currentCategories) => [
        ...currentCategories,
        createdCategory,
      ]);

      setName("");
    } catch (error) {
      handleError(error);
    } finally {
      setSubmitting(false);
    }
  }

  if (loading) {
    return <p>Carregando categorias...</p>;
  }

  return (
  <main>
    <h1>Categorias</h1>

    <section className="card">
      <h2>Nova categoria</h2>

      <form className="simple-form" onSubmit={handleSubmit}>
        <div>
          <label htmlFor="category-name">Nome</label>

          <input
            id="category-name"
            name="name"
            type="text"
            value={name}
            onChange={(event) => setName(event.target.value)}
            required
          />
        </div>

        <button type="submit" disabled={submitting}>
          {submitting ? "Salvando..." : "Criar categoria"}
        </button>
      </form>

      {errorMessage && (
  <p className="error-message">{errorMessage}</p>
)}
    </section>

    <section className="card">
      <h2>Categorias cadastradas</h2>

      {categories.length === 0 ? (
        <p>Nenhuma categoria cadastrada.</p>
      ) : (
        <ul className="data-list">
          {categories.map((category) => (
            <li key={category.id}>
              <span>{category.name}</span>
            </li>
          ))}
        </ul>
      )}
    </section>
  </main>
);
}