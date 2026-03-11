import { DOCUMENT } from '@angular/common';
import { effect, inject, Injectable, signal } from '@angular/core';

const THEME_STORAGE_KEY = 'dmusic.theme';
const DARK_THEME_CLASS = 'dark-theme';

@Injectable({ providedIn: 'root' })
export class ThemeService {
  private readonly document = inject(DOCUMENT);
  private readonly themeState = signal<'light' | 'dark'>(this.readTheme());

  readonly theme = this.themeState.asReadonly();
  readonly isDarkMode = () => this.themeState() === 'dark';

  constructor() {
    effect(() => {
      const isDark = this.themeState() === 'dark';
      const root = this.document.documentElement;
      const body = this.document.body;

      root.classList.toggle(DARK_THEME_CLASS, isDark);
      body.classList.toggle(DARK_THEME_CLASS, isDark);
      root.style.colorScheme = isDark ? 'dark' : 'light';
      localStorage.setItem(THEME_STORAGE_KEY, this.themeState());
    });
  }

  toggleTheme(): void {
    this.themeState.update((theme) => (theme === 'dark' ? 'light' : 'dark'));
  }

  private readTheme(): 'light' | 'dark' {
    if (typeof localStorage === 'undefined') {
      return 'light';
    }

    const stored = localStorage.getItem(THEME_STORAGE_KEY);
    return stored === 'dark' ? 'dark' : 'light';
  }
}
