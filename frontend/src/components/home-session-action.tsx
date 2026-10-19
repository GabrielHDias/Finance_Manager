"use client";

import Link from "next/link";
import { useSyncExternalStore } from "react";
import { getAccessToken } from "@/lib/token-storage";

function subscribe(onChange: () => void) {
  window.addEventListener("storage", onChange);
  window.addEventListener("pageshow", onChange);
  return () => {
    window.removeEventListener("storage", onChange);
    window.removeEventListener("pageshow", onChange);
  };
}

function getSnapshot() {
  return Boolean(getAccessToken());
}

function getServerSnapshot() {
  return false;
}

export default function HomeSessionAction({ className }: { className: string }) {
  const hasSession = useSyncExternalStore(subscribe, getSnapshot, getServerSnapshot);

  return (
    <Link className={className} href={hasSession ? "/dashboard" : "/login"}>
      {hasSession ? "Acessar aplicação" : "Entrar"}
    </Link>
  );
}
