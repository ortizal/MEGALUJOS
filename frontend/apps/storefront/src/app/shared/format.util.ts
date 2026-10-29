import { formatUsd } from '@megalujos/ui';

/** Formatea una fecha ISO a dd/mm/yyyy (America/Guayaquil en el dato del backend). */
export function formatDate(iso: string | null | undefined): string {
  if (!iso) {
    return '—';
  }
  const date = new Date(iso);
  if (Number.isNaN(date.getTime())) {
    return '—';
  }
  return new Intl.DateTimeFormat('es-EC', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
  }).format(date);
}

/** Rango legible de vigencia de una promoción. */
export function formatRange(startsAt?: string | null, endsAt?: string | null): string {
  const from = startsAt ? formatDate(startsAt) : null;
  const to = endsAt ? formatDate(endsAt) : null;
  if (from && to) {
    return `${from} – ${to}`;
  }
  if (from) {
    return `Desde ${from}`;
  }
  if (to) {
    return `Hasta ${to}`;
  }
  return 'Sin vigencia definida';
}

/** Descuento legible según el tipo de promoción (porcentaje o monto fijo). */
export function formatDiscount(
  promoType: string | null | undefined,
  value: string | number,
): string {
  const type = (promoType ?? '').toUpperCase();
  const numeric = Number(value);
  if (type.includes('PERCENT') || type.includes('PORC')) {
    return `-${numeric}%`;
  }
  if (type.includes('FIXED') || type.includes('AMOUNT') || type.includes('MONTO')) {
    return `-${formatUsd(value)}`;
  }
  return `${formatUsd(value)} de descuento`;
}
