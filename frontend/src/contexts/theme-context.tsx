"use client";

import { createContext, useContext, useSyncExternalStore } from "react";
import { setTheme, subscribeTheme, themeSnapshot, serverThemeSnapshot, type Theme } from "@/lib/theme";

const ThemeContext = createContext<{ theme: Theme | null; setTheme: typeof setTheme } | null>(null);

export function ThemeProvider({ children }: { children: React.ReactNode }) {
  const theme = useSyncExternalStore(subscribeTheme, themeSnapshot, serverThemeSnapshot);
  return <ThemeContext.Provider value={{ theme, setTheme }}>{children}</ThemeContext.Provider>;
}

export function useTheme() {
  const context = useContext(ThemeContext);
  if (!context) throw new Error("ThemeProvider ausente.");
  return context;
}
