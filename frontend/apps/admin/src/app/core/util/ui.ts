import { getErrorMessage } from '@megalujos/api-client';

/** Mensaje legible en español para cualquier error de API/red. */
export function errorMessage(error: unknown): string {
  return getErrorMessage(error);
}

/** Fecha local en formato ISO date (`yyyy-MM-dd`). */
export function todayIso(): string {
  return toIsoDate(new Date());
}

/** Fecha hace N días en formato ISO date. */
export function daysAgoIso(days: number): string {
  const date = new Date();
  date.setDate(date.getDate() - days);
  return toIsoDate(date);
}

function toIsoDate(date: Date): string {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  return `${year}-${month}-${day}`;
}

/** Inicio del día (`yyyy-MM-ddT00:00:00-05:00`) para filtros del backend. */
export function startOfDayIso(date: string): string {
  return `${date}T00:00:00-05:00`;
}

/** Fin del día (`yyyy-MM-ddT23:59:59-05:00`) para filtros del backend. */
export function endOfDayIso(date: string): string {
  return `${date}T23:59:59-05:00`;
}

/** Fecha y hora en zona America/Guayaquil (`19/10/2026 10:30`). */
export function formatDateTime(iso: string | null | undefined): string {
  if (!iso) {
    return '-';
  }
  const date = new Date(iso);
  if (Number.isNaN(date.getTime())) {
    return '-';
  }
  return new Intl.DateTimeFormat('es-EC', {
    timeZone: 'America/Guayaquil',
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
    hour12: false,
  }).format(date);
}

/** Fecha en zona America/Guayaquil (`19/10/2026`). */
export function formatDate(iso: string | null | undefined): string {
  if (!iso) {
    return '-';
  }
  const date = new Date(iso);
  if (Number.isNaN(date.getTime())) {
    return '-';
  }
  return new Intl.DateTimeFormat('es-EC', {
    timeZone: 'America/Guayaquil',
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
  }).format(date);
}

/** Número con separadores y decimales fijos (sin símbolo de moneda). */
export function formatNumber(value: string | number | null | undefined, digits = 2): string {
  if (value === null || value === undefined || value === '') {
    return '-';
  }
  const numeric = typeof value === 'number' ? value : Number(String(value).replace(/,/g, ''));
  if (Number.isNaN(numeric)) {
    return '-';
  }
  return new Intl.NumberFormat('en-US', {
    minimumFractionDigits: digits,
    maximumFractionDigits: digits,
  }).format(numeric);
}

/** Suma decimal simple sobre strings (solo presentación). */
export function sumDecimals(values: Array<string | number | null | undefined>): string {
  const total = values.reduce<number>((acc, value) => {
    const numeric = value === null || value === undefined || value === '' ? 0 : Number(value);
    return acc + (Number.isNaN(numeric) ? 0 : numeric);
  }, 0);
  return total.toFixed(2);
}

/** Clave de idempotencia (UUID v4) para ventas, cobros y emisión. */
export function newIdempotencyKey(): string {
  if (typeof crypto !== 'undefined' && typeof crypto.randomUUID === 'function') {
    return crypto.randomUUID();
  }
  return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, (char) => {
    const random = (Math.random() * 16) | 0;
    const value = char === 'x' ? random : (random & 0x3) | 0x8;
    return value.toString(16);
  });
}

/** Descarga un Blob (RIDE, XML, CSV) en el navegador. */
export function downloadBlob(blob: Blob, filename: string): void {
  const url = URL.createObjectURL(blob);
  const anchor = document.createElement('a');
  anchor.href = url;
  anchor.download = filename;
  anchor.rel = 'noopener';
  document.body.appendChild(anchor);
  anchor.click();
  anchor.remove();
  setTimeout(() => URL.revokeObjectURL(url), 4000);
}

/** Exporta filas a CSV (client-side) y dispara la descarga. */
export function downloadCsv(rows: Array<Record<string, unknown>>, filename: string): void {
  if (!rows.length) {
    return;
  }
  const headers = Object.keys(rows[0]);
  const escape = (value: unknown): string => {
    const text = value === null || value === undefined ? '' : String(value);
    return `"${text.replace(/"/g, '""')}"`;
  };
  const lines = [headers.map(escape).join(',')];
  for (const row of rows) {
    lines.push(headers.map((header) => escape(row[header])).join(','));
  }
  const blob = new Blob(['\uFEFF' + lines.join('\r\n')], { type: 'text/csv;charset=utf-8' });
  downloadBlob(blob, filename);
}

/** Abre una ventana de impresión con el HTML dado (ticket / comprobante). */
export function printHtml(title: string, body: string): void {
  const win = window.open('', '_blank', 'width=420,height=720');
  if (!win) {
    return;
  }
  win.document.write(`<!doctype html>
<html lang="es">
<head>
<meta charset="utf-8" />
<title>${title}</title>
<style>
  body { font-family: 'Inter', Arial, sans-serif; margin: 16px; color: #22262c; font-size: 13px; }
  h1 { font-size: 16px; margin: 0 0 4px; }
  h2 { font-size: 13px; margin: 14px 0 6px; }
  table { width: 100%; border-collapse: collapse; }
  th, td { text-align: left; padding: 4px 2px; border-bottom: 1px solid #dde2e7; font-size: 12px; }
  .num { text-align: right; font-variant-numeric: tabular-nums; }
  .total { font-weight: 700; font-size: 15px; }
  .muted { color: #8a939d; }
  .brand { color: #d71920; letter-spacing: .06em; font-weight: 800; }
</style>
</head>
<body>${body}</body>
</html>`);
  win.document.close();
  win.focus();
  setTimeout(() => win.print(), 250);
}

/** Recorta un UUID para mostrarlo de forma compacta. */
export function shortId(id: string | null | undefined): string {
  if (!id) {
    return '-';
  }
  return id.length > 12 ? `${id.slice(0, 8)}…` : id;
}
