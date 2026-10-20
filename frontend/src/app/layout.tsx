import type { Metadata } from "next";
import "./globals.css";
import { ThemeProvider } from "@/contexts/theme-context";
import { themeInitializer } from "@/lib/theme";

export const metadata: Metadata = {
  title: "Finance Manager",
  description: "Aplicação para gerenciamento financeiro pessoal.",
};

export default function RootLayout({ children }: LayoutProps<"/">) {
  return (
    <html lang="pt-BR" suppressHydrationWarning>
      <head><script dangerouslySetInnerHTML={{ __html: themeInitializer }} /></head>
      <body><ThemeProvider>{children}</ThemeProvider></body>
    </html>
  );
}