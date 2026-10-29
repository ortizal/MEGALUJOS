import { DOCUMENT, isPlatformBrowser } from '@angular/common';
import { Injectable, PLATFORM_ID, computed, inject, signal } from '@angular/core';
import { MEGA_THEME_STORAGE_KEY, MegaThemeMode } from './mega-tokens';

/**
 * Servicio de tema claro/oscuro (standalone, `providedIn: 'root'`).
 * - `theme` es la signal oficial ('light' | 'dark').
 * - Aplica `data-theme="light|dark"` sobre `<html>` (tokens.scss ajusta fondos/textos).
 * - Persiste la preferencia en `localStorage['mega-theme']`.
 * - Si no hay valor guardado, la preferencia inicial proviene de `prefers-color-scheme`.
 * - `reducedMotion` es una signal desde `matchMedia('(prefers-reduced-motion: reduce)')`
 *   con listener de cambios en caliente.
 */
@Injectable({ providedIn: 'root' })
export class ThemeService {
  private readonly doc = inject(DOCUMENT);
  private readonly platformId = inject(PLATFORM_ID);
  private readonly isBrowser = isPlatformBrowser(this.platformId);

  /** Modo de tema activo (señal oficial del servicio). */
  readonly theme = signal<MegaThemeMode>(this.resolveInitialMode());

  /** Alias de `theme` (misma señal) para lecturas en plantillas existentes. */
  readonly mode = this.theme;

  /** true cuando el usuario prefiere movimiento reducido (prefers-reduced-motion). */
  readonly reducedMotion = signal(false);

  /** true si el tema activo es oscuro. */
  readonly isDark = computed(() => this.theme() === 'dark');

  constructor() {
    if (this.isBrowser) {
      this.applyTheme(this.theme());
      this.watchSystemPreferences();
    }
  }

  /**
   * Inicialización explícita (opcional: la construcción del servicio ya aplica el tema).
   * Útil desde `provideAppInitializer` en app.config.ts si se quiere forzar el orden.
   */
  init(): void {
    if (!this.isBrowser) {
      return;
    }
    this.applyTheme(this.resolveInitialMode());
    this.watchSystemPreferences();
  }

  /** Cambia el tema y lo persiste en localStorage('mega-theme'). */
  setTheme(theme: MegaThemeMode): void {
    this.theme.set(theme);
    this.applyTheme(theme);
    if (this.isBrowser) {
      try {
        localStorage.setItem(MEGA_THEME_STORAGE_KEY, theme);
      } catch {
        /* almacenamiento no disponible */
      }
    }
  }

  /** Alias de `setTheme`. */
  setMode(mode: MegaThemeMode): void {
    this.setTheme(mode);
  }

  /** Alterna entre claro y oscuro. */
  toggle(): void {
    this.setTheme(this.theme() === 'dark' ? 'light' : 'dark');
  }

  /** Limpia la preferencia persistida (vuelve a seguir al sistema). */
  clearPreference(): void {
    if (!this.isBrowser) {
      return;
    }
    try {
      localStorage.removeItem(MEGA_THEME_STORAGE_KEY);
    } catch {
      /* almacenamiento no disponible */
    }
    this.setTheme(this.systemPrefersDark() ? 'dark' : 'light');
  }

  private resolveInitialMode(): MegaThemeMode {
    if (!this.isBrowser) {
      return 'light';
    }
    try {
      const stored = localStorage.getItem(MEGA_THEME_STORAGE_KEY);
      if (stored === 'dark' || stored === 'light') {
        return stored;
      }
    } catch {
      /* almacenamiento no disponible */
    }
    return this.systemPrefersDark() ? 'dark' : 'light';
  }

  private applyTheme(theme: MegaThemeMode): void {
    const root = this.doc.documentElement;
    root.setAttribute('data-theme', theme);
    root.style.colorScheme = theme;
  }

  private systemPrefersDark(): boolean {
    return typeof window !== 'undefined' && typeof window.matchMedia === 'function'
      ? window.matchMedia('(prefers-color-scheme: dark)').matches
      : false;
  }

  private watchingSystemPreferences = false;

  private watchSystemPreferences(): void {
    if (this.watchingSystemPreferences) {
      return;
    }
    if (typeof window !== 'undefined' && typeof window.matchMedia === 'function') {
      this.watchingSystemPreferences = true;
      const colorQuery = window.matchMedia('(prefers-color-scheme: dark)');
      colorQuery.addEventListener('change', (event) => {
        // Solo sigue al sistema mientras el usuario no haya elegido explícitamente.
        let hasStored = false;
        try {
          hasStored = localStorage.getItem(MEGA_THEME_STORAGE_KEY) !== null;
        } catch {
          /* almacenamiento no disponible */
        }
        if (!hasStored) {
          this.setTheme(event.matches ? 'dark' : 'light');
        }
      });

      const motionQuery = window.matchMedia('(prefers-reduced-motion: reduce)');
      this.reducedMotion.set(motionQuery.matches);
      motionQuery.addEventListener('change', (event) => {
        this.reducedMotion.set(event.matches);
      });
    }
  }
}
