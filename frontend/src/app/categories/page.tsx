"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { ApiError } from "@/lib/api-error";
import { findAllCategories } from "@/services/category-service";
import type { CategoryResponse } from "@/types/category";

export default function CategoriesPage() {
  const router = useRouter();

  const [categories, setCategories] = useState<CategoryResponse[]>([]);
  const [loading, setLoading] = useState(true);
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

  if (loading) {
    return <p>Carregando categorias...</p>;
  }

  if (errorMessage) {
    return <p>{errorMessage}</p>;
  }

  return (
    <main>
      <h1>Categorias</h1>

      {categories.length === 0 ? (
        <p>Nenhuma categoria cadastrada.</p>
      ) : (
        <ul>
          {categories.map((category) => (
            <li key={category.id}>{category.name}</li>
          ))}
        </ul>
      )}
    </main>
  );
}