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
    <nav className="app-navigation">
      <div className="app-navigation__links">
        <Link
          href="/dashboard"
          className={pathname === "/dashboard" ? "active" : ""}
        >
          Dashboard
        </Link>

        <Link
          href="/accounts"
          className={pathname === "/accounts" ? "active" : ""}
        >
          Contas financeiras
        </Link>

        <Link
          href="/categories"
          className={pathname === "/categories" ? "active" : ""}
        >
          Categorias
        </Link>

        <Link
          href="/transactions"
          className={pathname === "/transactions" ? "active" : ""}
        >
          Transações
        </Link>
      </div>

      <button type="button" onClick={handleLogout}>
        Sair
      </button>
    </nav>
  );
}