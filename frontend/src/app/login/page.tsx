"use client";

import { FormEvent, useState } from "react";
import { useRouter } from "next/navigation";
import { ApiError } from "@/lib/api-error";
import { saveAccessToken } from "@/lib/token-storage";
import { login } from "@/services/auth-service";
import Link from "next/link";

export default function LoginPage() {
  const router = useRouter();

  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [errorMessage, setErrorMessage] = useState("");

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (submitting) return;

    setSubmitting(true);
    setErrorMessage("");

    try {
      const response = await login({
        email: email.trim(),
        password,
      });

      saveAccessToken(response.accessToken);

      router.replace("/dashboard");
    } catch (error) {
      if (error instanceof ApiError) {
        setErrorMessage(error.message);
        return;
      }

      setErrorMessage("Não foi possível conectar ao servidor.");
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <main>
      <h1>Entrar</h1>

      <form onSubmit={handleSubmit}>
        <div>
          <label htmlFor="email">E-mail</label>

          <input
            id="email"
            autoComplete="username"
            maxLength={255}
            disabled={submitting}
            name="email"
            type="email"
            value={email}
            onChange={(event) => setEmail(event.target.value)}
            required
          />
        </div>

        <div>
          <label htmlFor="password">Senha</label>

          <input
            id="password"
            autoComplete="current-password"
            disabled={submitting}
            name="password"
            type="password"
            value={password}
            onChange={(event) => setPassword(event.target.value)}
            required
          />
        </div>

        {errorMessage && (
        <p className="error-message" role="alert">{errorMessage}</p>
        )}

        <button type="submit" disabled={submitting}>
          {submitting ? "Entrando..." : "Entrar"}
        </button>
      </form>
      <p>
  Ainda não possui conta? <Link href="/register">Criar conta</Link>
</p>
    </main>
  );
}