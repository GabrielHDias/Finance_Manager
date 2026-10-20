export type Theme = "light" | "dark";
export const THEME_KEY = "finance-manager-theme";
const CHANGE_EVENT = "finance-manager-theme-change";
let manualSelection: Theme | null = null;

export function isTheme(value: unknown): value is Theme {
  return value === "light" || value === "dark";
}

export function readPreference(): Theme | null {
  try {
    const saved = window.localStorage.getItem(THEME_KEY);
    return isTheme(saved) ? saved : null;
  } catch {
    return null;
  }
}

export function setTheme(theme: Theme) {
  manualSelection = theme;
  document.documentElement.dataset.theme = theme;
  try { window.localStorage.setItem(THEME_KEY, theme); } catch { /* Keep the choice for this session when storage is unavailable. */ }
  window.dispatchEvent(new Event(CHANGE_EVENT));
}

export function themeSnapshot(): Theme {
  return document.documentElement.dataset.theme === "dark" ? "dark" : "light";
}

export function serverThemeSnapshot(): null { return null; }

export function subscribeTheme(notify: () => void) {
  const media = window.matchMedia("(prefers-color-scheme: dark)");
  const sync = () => {
    document.documentElement.dataset.theme = manualSelection ?? readPreference() ?? (media.matches ? "dark" : "light");
    notify();
  };
  const storage = (event: StorageEvent) => {
    if (event.key === THEME_KEY || event.key === null) { manualSelection = null; sync(); }
  };
  sync();
  media.addEventListener("change", sync);
  window.addEventListener("storage", storage);
  window.addEventListener(CHANGE_EVENT, notify);
  return () => {
    media.removeEventListener("change", sync);
    window.removeEventListener("storage", storage);
    window.removeEventListener(CHANGE_EVENT, notify);
  };
}

// Static code only; no user data is interpolated into this pre-hydration script.
export const themeInitializer = `(()=>{let t;try{t=localStorage.getItem("${THEME_KEY}")}catch{}document.documentElement.dataset.theme=t==="light"||t==="dark"?t:matchMedia("(prefers-color-scheme: dark)").matches?"dark":"light"})()`;
