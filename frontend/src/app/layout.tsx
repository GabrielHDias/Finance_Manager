import type { Metadata } from "next";
import "./globals.css";

export const metadata: Metadata = {
  title: "Finance Manager",
  description: "Aplicação para gerenciamento financeiro pessoal.",
};

export default function RootLayout({ children }: LayoutProps<"/">) {
  return (
    <html lang="pt-BR">
      <body>{children}</body>
    </html>
  );
}