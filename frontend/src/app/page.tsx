import Link from "next/link";

export default function HomePage() {
  return (
    <main>
      <h1>Finance Manager</h1>

      <p>
        Gerencie suas contas financeiras, categorias, transações e acompanhe seu
        saldo em um único lugar.
      </p>

      <div>
        <Link href="/login">Entrar</Link>

        <Link href="/register">Criar conta</Link>
      </div>
    </main>
  );
}