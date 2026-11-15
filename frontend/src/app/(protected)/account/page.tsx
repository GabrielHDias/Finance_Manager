"use client";

import { FormEvent, useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { ApiError } from "@/lib/api-error";
import { removeAccessToken } from "@/lib/token-storage";
import { changeEmail, deleteOwnAccount, findCurrentUser } from "@/services/user-service";

export default function OwnAccountPage() {
  const router = useRouter();
  const [email, setEmail] = useState("");
  const [currentEmail, setCurrentEmail] = useState("");
  const [password, setPassword] = useState("");
  const [deletePassword, setDeletePassword] = useState("");
  const [confirming, setConfirming] = useState(false);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");

  useEffect(() => {
    findCurrentUser().then(user => { setEmail(user.email); setCurrentEmail(user.email); }).catch(failure => {
      if (failure instanceof ApiError && failure.status === 401) router.replace("/login");
    });
  }, [router]);

  async function submitEmail(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setError("");
    setSuccess("");
    try {
      const user = await changeEmail(email, password);
      setCurrentEmail(user.email);
      setPassword("");
      setSuccess("E-mail atualizado. O acesso atual continua válido até expirar.");
    } catch (failure) {
      if (failure instanceof ApiError && failure.status === 401 && failure.message.includes("Senha")) {
        setError(failure.message);
        return;
      }
      if (failure instanceof ApiError && failure.status === 401) { router.replace("/login"); return; }
      setError(failure instanceof ApiError ? failure.message : "Não foi possível atualizar o e-mail.");
    }
  }

  async function submitDelete(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setError("");
    try {
      await deleteOwnAccount(deletePassword);
      removeAccessToken();
      router.replace("/");
    } catch (failure) {
      if (failure instanceof ApiError && failure.status === 401 && failure.message.includes("Senha")) {
        setError(failure.message);
        return;
      }
      if (failure instanceof ApiError && failure.status === 401) { router.replace("/login"); return; }
      setError(failure instanceof ApiError ? failure.message : "Não foi possível excluir a conta.");
    }
  }

  return <main>
    <h1>Minha conta</h1>
    <p>E-mail atual: {currentEmail || "..."}</p>
    {error && <p className="error-message" role="alert">{error}</p>}
    {success && <p className="success-message" role="status">{success}</p>}
    <section className="card">
      <h2>Alterar e-mail</h2>
      <form className="simple-form" onSubmit={submitEmail}>
        <div><label htmlFor="own-email">Novo e-mail</label><input id="own-email" type="email" value={email} onChange={event => setEmail(event.target.value)} required /></div>
        <div><label htmlFor="own-password">Senha atual</label><input id="own-password" type="password" value={password} onChange={event => setPassword(event.target.value)} required /></div>
        <button type="submit">Salvar e-mail</button>
      </form>
    </section>
    <section className="card">
      <h2>Excluir conta</h2>
      <p>A exclusão apaga contas, categorias, transações, orçamentos, etiquetas e recorrências. O token deixa de acessar dados, e esta tela encerra a sessão local.</p>
      <form className="simple-form" onSubmit={submitDelete}>
        <div><label htmlFor="delete-password">Senha atual</label><input id="delete-password" type="password" value={deletePassword} onChange={event => setDeletePassword(event.target.value)} required /></div>
        {confirming ? <>
          <p>Confirmar a exclusão permanente da conta?</p>
          <button className="button--danger" type="submit">Confirmar exclusão</button>
          <button className="button--secondary" type="button" onClick={() => setConfirming(false)}>Cancelar</button>
        </> : <button className="button--danger" type="button" onClick={() => setConfirming(true)}>Excluir minha conta</button>}
      </form>
    </section>
  </main>;
}
