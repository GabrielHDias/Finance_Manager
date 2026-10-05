"use client";

import { FormEvent, useState } from "react";
import { login } from "@/services/auth-service";
import { ApiError } from "@/lib/api-error";
import { saveAccessToken } from "@/lib/token-storage";

export default function LoginPage() {
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [errorMessage, setErrorMessage] = useState("");

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();

    setErrorMessage("");

    try {
      const response = await login({
        email,
        password,
      });

      saveAccessToken(response.accessToken);
    } catch (error) {
      if (error instanceof ApiError) {
        setErrorMessage(error.message);
        return;
      }

      setErrorMessage("Não foi possível conectar ao servidor.");
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
            name="password"
            type="password"
            value={password}
            onChange={(event) => setPassword(event.target.value)}
            required
          />
        </div>

        {errorMessage && <p>{errorMessage}</p>}

        <button type="submit">Entrar</button>
      </form>
    </main>
  );
}