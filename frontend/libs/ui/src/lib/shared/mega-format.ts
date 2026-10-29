/** Contador para identificadores DOM únicos de los componentes. */
let uidCounter = 0;

/** Genera un id DOM único con prefijo (`mega-…`). */
export function megaUid(prefix = 'mega'): string {
  uidCounter += 1;
  return `${prefix}-${uidCounter}`;
}

/** Detecta `prefers-reduced-motion` (seguro en SSR/jsdom). */
export function prefersReducedMotion(): boolean {
  return (
    typeof window !== 'undefined' &&
    typeof window.matchMedia === 'function' &&
    window.matchMedia('(prefers-reduced-motion: reduce)').matches
  );
}

const usdFormatter = new Intl.NumberFormat('en-US', {
  style: 'currency',
  currency: 'USD',
  minimumFractionDigits: 2,
  maximumFractionDigits: 2,
});

/**
 * Formatea un importe USD (`Intl.NumberFormat('en-US',{style:'currency',currency:'USD'})`).
 * Acepta decimal string (contrato backend), number o null.
 */
export function formatUsd(value: string | number | null | undefined): string {
  if (value === null || value === undefined || value === '') {
    return '—';
  }
  const numeric = typeof value === 'number' ? value : Number(String(value).replace(/,/g, ''));
  if (Number.isNaN(numeric)) {
    return '—';
  }
  return usdFormatter.format(numeric);
}

/** Convierte un valor monetario a decimal string con 2 decimales (o null si es inválido). */
export function toDecimalString(value: string | number | null | undefined): string | null {
  if (value === null || value === undefined || value === '') {
    return null;
  }
  const numeric = typeof value === 'number' ? value : Number(String(value).replace(/,/g, ''));
  if (Number.isNaN(numeric)) {
    return null;
  }
  return numeric.toFixed(2);
}

/** `LOW_STOCK` → `Low stock` (fallback legible para códigos sin traducción). */
export function prettifyCode(code: string | null | undefined): string {
  if (!code) {
    return '—';
  }
  const text = code.replace(/_/g, ' ').toLowerCase();
  return text.charAt(0).toUpperCase() + text.slice(1);
}
