import { Component, computed, input } from '@angular/core';
import { prettifyCode } from '../shared/mega-format';

export interface MegaStatusTone {
  label: string;
  /** Clase CSS de tono (mega-status-badge--*). */
  tone: 'success' | 'warning' | 'danger' | 'info' | 'neutral';
}

const STATUS_TONES: Record<string, MegaStatusTone> = {
  // Ventas / caja
  PAID: { label: 'Pagada', tone: 'success' },
  PENDING: { label: 'Pendiente', tone: 'warning' },
  VOID: { label: 'Anulada', tone: 'danger' },
  RETURNED: { label: 'Devuelta', tone: 'danger' },
  PARTIALLY_RETURNED: { label: 'Devuelta parcialmente', tone: 'warning' },
  OPEN: { label: 'Abierta', tone: 'success' },
  CLOSED: { label: 'Cerrada', tone: 'neutral' },
  // Documentos electrónicos
  AUTHORIZED: { label: 'Autorizado', tone: 'success' },
  PROCESSING: { label: 'Procesando', tone: 'info' },
  REJECTED: { label: 'Rechazado', tone: 'danger' },
  ERROR: { label: 'Con error', tone: 'danger' },
  CREDIT_NOTE: { label: 'Nota de crédito', tone: 'info' },
  // Servicios
  QUOTATION: { label: 'Cotización', tone: 'neutral' },
  CONFIRMED: { label: 'Confirmada', tone: 'info' },
  IN_PROGRESS: { label: 'En ejecución', tone: 'info' },
  READY: { label: 'Lista', tone: 'warning' },
  DELIVERED: { label: 'Entregada', tone: 'success' },
  CANCELLED: { label: 'Cancelada', tone: 'danger' },
  // Inventario / compras
  DRAFT: { label: 'Borrador', tone: 'neutral' },
  ORDERED: { label: 'Ordenada', tone: 'info' },
  RECEIVED: { label: 'Recibida', tone: 'success' },
  PARTIALLY_RECEIVED: { label: 'Recepción parcial', tone: 'warning' },
  APPROVED: { label: 'Aprobado', tone: 'success' },
  COMPLETED: { label: 'Completado', tone: 'success' },
  // Impresión
  PRINTED: { label: 'Impreso', tone: 'success' },
  QUEUED: { label: 'En cola', tone: 'info' },
  FAILED: { label: 'Fallido', tone: 'danger' },
};

/**
 * Insignia de estado genérica con mapa de colores por código.
 * El texto SIEMPRE está visible (nunca solo color); acepta etiqueta y tono
 * personalizados como input opcional.
 */
@Component({
  selector: 'mega-status-badge',
  standalone: true,
  styleUrl: './mega-status-badge.component.scss',
  template: `
    <span class="mega-status-badge" [class]="'mega-status-badge--' + tone()">
      <span class="mega-status-badge__dot" aria-hidden="true"></span>
      {{ text() }}
    </span>
  `,
})
export class MegaStatusBadgeComponent {
  /** Código de estado (p. ej. PAID, AUTHORIZED, IN_PROGRESS). */
  status = input.required<string | null | undefined>();

  /** Etiqueta personalizada (sobrescribe el mapa). */
  label = input<string | null>(null);

  /** Tono personalizado. */
  toneOverride = input<MegaStatusTone['tone'] | null>(null);

  /** Mapa adicional de etiquetas para códigos propios del dominio. */
  customLabels = input<Record<string, string>>({});

  /** Mapa adicional de tonos para códigos propios del dominio. */
  customTones = input<Record<string, MegaStatusTone['tone']>>({});

  private readonly resolved = computed<MegaStatusTone>(() => {
    const raw = (this.status() ?? '').toString();
    const upper = raw.toUpperCase();
    const label = this.customLabels()[raw] ?? this.customLabels()[upper] ?? STATUS_TONES[upper]?.label;
    const tone = this.customTones()[raw] ?? this.customTones()[upper] ?? STATUS_TONES[upper]?.tone;
    return {
      label: label ?? prettifyCode(raw),
      tone: tone ?? 'neutral',
    };
  });

  text(): string {
    return this.label() ?? this.resolved().label;
  }

  tone(): MegaStatusTone['tone'] {
    return this.toneOverride() ?? this.resolved().tone;
  }
}
