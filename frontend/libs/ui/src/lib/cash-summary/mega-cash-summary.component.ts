import { Component, computed, input } from '@angular/core';

/** Tono de un dato del resumen de caja. */
export type MegaCashSummaryTone = 'default' | 'success' | 'danger' | 'warning' | 'muted';

/** Dato del resumen de caja. */
export interface MegaCashSummaryItem {
  /** Etiqueta legible (ej. "Efectivo contado"). */
  label: string;
  /** Valor ya formateado (usar `formatUsd` del contenedor si aplica). */
  value: string;
  /** Tono del valor. */
  tone?: MegaCashSummaryTone;
  /** Detalle secundario (opcional). */
  hint?: string | null;
}

/**
 * Resumen de caja: cuadrícula de datos (esperado, contado, diferencia, etc.)
 * con la diferencia destacada. UI pura: el contenedor formatea los importes.
 */
@Component({
  selector: 'mega-cash-summary',
  standalone: true,
  styleUrl: './mega-cash-summary.component.scss',
  template: `
    <section class="mega-cash-summary" [attr.aria-label]="title()">
      @if (title()) {
        <h3 class="mega-cash-summary__title">{{ title() }}</h3>
      }

      <dl class="mega-cash-summary__grid">
        @for (item of items(); track item.label) {
          <div
            class="mega-cash-summary__item"
            [attr.data-tone]="item.tone ?? 'default'"
          >
            <dt class="mega-cash-summary__label">{{ item.label }}</dt>
            <dd class="mega-cash-summary__value">{{ item.value }}</dd>
            @if (item.hint) {
              <p class="mega-cash-summary__hint">{{ item.hint }}</p>
            }
          </div>
        }
      </dl>

      @if (differenceLabel(); as label) {
        <p class="mega-cash-summary__footer" [attr.data-tone]="differenceTone()">
          <span>{{ label }}</span>
          <strong>{{ differenceValue() }}</strong>
        </p>
      }
    </section>
  `,
})
export class MegaCashSummaryComponent {
  /** Título de la sección. */
  title = input('Resumen de caja');

  /** Datos a mostrar. */
  items = input<MegaCashSummaryItem[]>([]);

  /** Etiqueta de la diferencia (si hay, se muestra destacada). */
  differenceLabel = input<string | null>('Diferencia');

  /** Valor de la diferencia (formateado). */
  differenceValue = input<string>('—');

  /** Tono de la diferencia (`danger` si hay faltante/sobrante). */
  differenceTone = input<'default' | 'success' | 'danger'>('default');

  /** Cantidad de datos (para el aria). */
  count = computed(() => this.items().length);
}
