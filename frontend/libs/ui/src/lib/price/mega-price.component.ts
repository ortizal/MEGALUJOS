import { booleanAttribute, Component, computed, input } from '@angular/core';
import { formatUsd } from '../shared/mega-format';

export type MegaPriceSize = 'sm' | 'md' | 'lg';
export type MegaPriceTone = 'default' | 'success' | 'danger' | 'muted';

/**
 * Importe monetario en formato USD (`Intl.NumberFormat`), con tabular-nums,
 * tono opcional y precio de comparación tachado.
 */
@Component({
  selector: 'mega-price',
  standalone: true,
  styleUrl: './mega-price.component.scss',
  host: { '[class]': 'hostClasses()' },
  template: `
    @if (compareAtValue()) {
      <span class="mega-price__compare">{{ compareAtText() }}</span>
    }
    <span class="mega-price__value">{{ text() }}</span>
  `,
})
export class MegaPriceComponent {
  /** Importe (decimal string del backend, number o null). */
  value = input<string | number | null>(null);

  /** Precio anterior para mostrar tachado (descuentos). */
  compareAt = input<string | number | null>(null);

  /** Tamaño del texto. */
  size = input<MegaPriceSize>('md');

  /** Tono del importe. */
  tone = input<MegaPriceTone>('default');

  /** Variante compacta (menor tamaño para tablas). */
  compact = input(false, { transform: booleanAttribute });

  /** Texto mostrado cuando no hay importe. */
  empty = input('—');

  /** Importe formateado. */
  text = computed(() => formatUsd(this.value()) || this.empty());

  /** Precio de comparación formateado (null si no corresponde). */
  compareAtValue = computed(() => {
    const compare = this.compareAt();
    if (compare === null || compare === undefined || compare === '') {
      return null;
    }
    const current = this.value();
    if (current === null || current === undefined || current === '') {
      return null;
    }
    return compare !== current ? compare : null;
  });

  compareAtText = computed(() => formatUsd(this.compareAtValue()));

  /** Clases CSS activas. */
  hostClasses = computed(() => {
    const classes = ['mega-price', `mega-price--${this.size()}`, `mega-price--${this.tone()}`];
    if (this.compact()) {
      classes.push('mega-price--compact');
    }
    return classes.join(' ');
  });
}
