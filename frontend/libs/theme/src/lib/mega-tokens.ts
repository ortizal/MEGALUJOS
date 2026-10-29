/** Valores canónicos de los tokens MEGALUJOS (espejo de tokens.scss). */
export const MEGA_TOKENS: Readonly<Record<string, string>> = {
  '--mega-red': '#D71920',
  '--mega-red-hover': '#B9141A',
  '--mega-black': '#111318',
  '--mega-carbon': '#1B1F25',
  '--mega-silver': '#AEB4BC',
  '--mega-white': '#FFFFFF',
  '--mega-light': '#F4F6F8',
  '--mega-text': '#22262C',
  '--mega-success': '#18855B',
  '--mega-warning': '#B66A00',
} as const;

/** Clave de persistencia de la preferencia de tema (`localStorage['mega-theme']`). */
export const MEGA_THEME_STORAGE_KEY = 'mega-theme';

/** Modos de tema soportados. */
export type MegaThemeMode = 'light' | 'dark';
