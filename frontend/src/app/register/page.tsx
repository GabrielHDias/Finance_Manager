"use client";

import { FormEvent, useState } from "react";
import { useRouter } from "next/navigation";
import { ApiError } from "@/lib/api-error";
import { createUser } from "@/services/user-service";
import Link from "next/link";

export default function RegisterPage() {
  const router = useRouter();

  const [name, setName] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [errorMessage, setErrorMessage] = useState("");
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (submitting) return;

    const trimmedName = name.trim();
    const trimmedEmail = email.trim();

    if (!trimmedName) {
      setErrorMessage("Informe o nome.");
      return;
    }

    if (!trimmedEmail) {
      setErrorMessage("Informe o e-mail.");
      return;
    }

    if (password.length < 8 || password.length > 72) {
      setErrorMessage("A senha deve possuir entre 8 e 72 caracteres.");
      return;
    }

    if (new TextEncoder().encode(password).length > 72) {
      setErrorMessage("A senha é muito longa. Use uma senha menor, especialmente se incluir acentos ou emojis.");
      return;
    }

    setSubmitting(true);
    setErrorMessage("");
    setFieldErrors({});

    try {
      await createUser({
        name: trimmedName,
        email: trimmedEmail,
        password,
      });

      router.push("/login");
    } catch (error) {
      if (error instanceof ApiError) {
        setErrorMessage(error.message);
        setFieldErrors(error.fields);
        return;
      }

      setErrorMessage("Não foi possível criar o usuário.");
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <main>
      <h1>Criar conta</h1>

      <form onSubmit={handleSubmit}>
        <div>
          <label htmlFor="name">Nome</label>

          <input
            id="name"
            autoComplete="name"
            maxLength={100}
            disabled={submitting}
            aria-invalid={Boolean(fieldErrors.name)}
            aria-describedby={fieldErrors.name ? "name-error" : undefined}
            name="name"
            type="text"
            value={name}
            onChange={(event) => setName(event.target.value)}
            required
          />
          {fieldErrors.name && <p id="name-error" role="alert">{fieldErrors.name}</p>}
        </div>

        <div>
          <label htmlFor="email">E-mail</label>

          <input
            id="email"
            autoComplete="email"
            maxLength={255}
            disabled={submitting}
            aria-invalid={Boolean(fieldErrors.email)}
            aria-describedby={fieldErrors.email ? "email-error" : undefined}
            name="email"
            type="email"
            value={email}
            onChange={(event) => setEmail(event.target.value)}
            required
          />
          {fieldErrors.email && <p id="email-error" role="alert">{fieldErrors.email}</p>}
        </div>

        <div>
          <label htmlFor="password">Senha</label>

          <input
            id="password"
            autoComplete="new-password"
            minLength={8}
            maxLength={72}
            disabled={submitting}
            aria-invalid={Boolean(fieldErrors.password)}
            aria-describedby={`password-help${fieldErrors.password ? " password-error" : ""}`}
            name="password"
            type="password"
            value={password}
            onChange={(event) => setPassword(event.target.value)}
            required
          />
          <small id="password-help">Use de 8 a 72 caracteres. Acentos e emojis podem reduzir o limite.</small>
          {fieldErrors.password && <p id="password-error" role="alert">{fieldErrors.password}</p>}
        </div>

        {errorMessage && (
        <p className="error-message" role="alert">{errorMessage}</p>
        )}

        <button type="submit" disabled={submitting}>
          {submitting ? "Criando..." : "Criar usuário"}
        </button>
      </form>
      <p>
  Já possui conta? <Link href="/login">Entrar</Link>
</p>
    </main>
  );
}