"use client";

import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { removeAccessToken } from "@/lib/token-storage";

export default function AppNavigation() {
  const pathname = usePathname();
  const router = useRouter();

  function handleLogout() {
    removeAccessToken();

    router.replace("/login");
  }

  return (
    <nav className="app-navigation" aria-label="Navegação principal">
      <div className="app-navigation__links">
        <Link
          href="/dashboard"
          className={pathname === "/dashboard" ? "active" : ""} aria-current={pathname === "/dashboard" ? "page" : undefined}
        >
          Dashboard
        </Link>

        <Link
          href="/accounts"
          className={pathname === "/accounts" ? "active" : ""} aria-current={pathname === "/accounts" ? "page" : undefined}
        >
          Contas financeiras
        </Link>

        <Link
          href="/categories"
          className={pathname === "/categories" ? "active" : ""} aria-current={pathname === "/categories" ? "page" : undefined}
        >
          Categorias
        </Link>

        <Link href="/budgets" className={pathname === "/budgets" ? "active" : ""} aria-current={pathname === "/budgets" ? "page" : undefined}>
          Orçamentos
        </Link>

        <Link
          href="/transactions"
          className={pathname === "/transactions" ? "active" : ""} aria-current={pathname === "/transactions" ? "page" : undefined}
        >
          Transações
        </Link>
        <Link href="/transactions/import" className={pathname === "/transactions/import" ? "active" : ""} aria-current={pathname === "/transactions/import" ? "page" : undefined}>
          Importar CSV
        </Link>
      </div>

      <button className="button--secondary" type="button" onClick={handleLogout}>
        Sair
      </button>
    </nav>
  );
}
