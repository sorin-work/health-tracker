export type Theme = 'system' | 'light' | 'dark';

const THEME_KEY = 'health-tracker.theme';

export function getTheme(): Theme {
  try {
    const stored = localStorage.getItem(THEME_KEY);
    return stored === 'light' || stored === 'dark' ? stored : 'system';
  } catch {
    return 'system';
  }
}

/** Sets `data-theme` on the root element; "system" removes it so the media query decides. */
export function applyTheme(theme: Theme, root: HTMLElement = document.documentElement): void {
  if (theme === 'system') delete root.dataset.theme;
  else root.dataset.theme = theme;
}

export function setTheme(theme: Theme): void {
  try {
    if (theme === 'system') localStorage.removeItem(THEME_KEY);
    else localStorage.setItem(THEME_KEY, theme);
  } catch {
    // Without storage the choice only lasts until the page is reloaded.
  }
  applyTheme(theme);
}
