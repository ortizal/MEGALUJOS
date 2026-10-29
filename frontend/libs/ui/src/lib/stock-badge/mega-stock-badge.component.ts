import { Component, computed, input } from '@angular/core';
import { StockAvailability, STOCK_AVAILABILITY_LABELS } from '@megalujos/models';

const TONE_CLASS: Record<StockAvailability, string> = {
  AVAILABLE: 'mega-stock-badge--available',
  LOW_STOCK: 'mega-stock-badge--low',
  OUT_OF_STOCK: 'mega-stock-badge--out',
  ON_REQUEST: 'mega-stock-badge--request',
};

const ICON: Record<StockAvailability, string> = {
  AVAILABLE: '●',
  LOW_STOCK: '▲',
  OUT_OF_STOCK: '✕',
  ON_REQUEST: '⏱',
};

/**
 * Insignia de disponibilidad con texto SIEMPRE visible
 * (nunca depende solo del color): "Disponible", "Stock bajo", "Agotado", "Bajo pedido".
 */
@Component({
  selector: 'mega-stock-badge',
  standalone: true,
  styleUrl: './mega-stock-badge.component.scss',
  template: `
    <span class="mega-stock-badge" [class]="toneClass()">
      <span class="mega-stock-badge__icon" aria-hidden="true">{{ icon() }}</span>
      {{ text() }}
      @if (quantity() !== null && quantity() !== undefined) {
        <span class="mega-stock-badge__qty">({{ quantity() }})</span>
      }
    </span>
  `,
})
export class MegaStockBadgeComponent {
  /** Código de disponibilidad (AVAILABLE, LOW_STOCK, OUT_OF_STOCK, ON_REQUEST). */
  availability = input.required<StockAvailability | string>();

  /** Cantidad disponible opcional. */
  quantity = input<string | number | null | undefined>(null);

  /** Etiqueta legible; cae a un texto genérico para códigos desconocidos. */
  label = input<string | null>(null);

  /** Clase de tono según el código. */
  toneClass = computed(() => {
    const code = this.availability() as StockAvailability;
    return TONE_CLASS[code] ?? 'mega-stock-badge--neutral';
  });

  icon(): string {
    const code = this.availability() as StockAvailability;
    return ICON[code] ?? '•';
  }

  text(): string {
    const code = this.availability() as StockAvailability;
    return (
      this.label() ??
      STOCK_AVAILABILITY_LABELS[code] ??
      String(this.availability()).replace(/_/g, ' ')
    );
  }
}
