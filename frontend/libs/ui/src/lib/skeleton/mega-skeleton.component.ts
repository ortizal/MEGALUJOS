import { Component, computed, input } from '@angular/core';

/**
 * Placeholder de carga con efecto shimmer.
 * Respeta `prefers-reduced-motion` (sin animación, gris plano).
 */
@Component({
  selector: 'mega-skeleton',
  standalone: true,
  styleUrl: './mega-skeleton.component.scss',
  template: `
    @if (variant() === 'text') {
      <div class="mega-skeleton__lines" aria-hidden="true">
        @for (line of lineArray(); track line) {
          <span
            class="mega-skeleton__line"
            [style.width]="line === lineArray().length ? lastWidth() : width()"
          ></span>
        }
      </div>
    } @else if (variant() === 'circle') {
      <span class="mega-skeleton__circle" [style.width]="size()" [style.height]="size()" aria-hidden="true"></span>
    } @else {
      <span
        class="mega-skeleton__block"
        [style.width]="width()"
        [style.height]="height()"
        aria-hidden="true"
      ></span>
    }
    <span class="mega-skeleton__sr">{{ label() }}</span>
  `,
})
export class MegaSkeletonComponent {
  /** Forma del placeholder. */
  variant = input<'text' | 'block' | 'circle'>('text');

  /** Cantidad de líneas (solo variante text). */
  lines = input(3);

  /** Ancho de cada línea/bloque (CSS length o %). */
  width = input('100%');

  /** Última línea más corta (variante text). */
  lastWidth = input('60%');

  /** Alto del bloque (variante block). */
  height = input('1rem');

  /** Tamaño del círculo (variante circle). */
  size = input('3rem');

  /** Etiqueta accesible (describe la región de carga). */
  label = input('Cargando contenido');

  lineArray = computed(() => Array.from({ length: Math.max(1, this.lines()) }, (_, i) => i + 1));
}
