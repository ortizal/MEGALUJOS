import { Component, booleanAttribute, computed, input, output } from '@angular/core';

/**
 * Paginación MEGALUJOS (1-based): prev/next, ventana de páginas y resumen
 * "Mostrando X–Y de Z". El estado lo controla el contenedor.
 */
@Component({
  selector: 'mega-pagination',
  standalone: true,
  styleUrl: './mega-pagination.component.scss',
  template: `
    <p class="mega-pagination__summary">{{ summary() }}</p>

    <nav class="mega-pagination__nav" [attr.aria-label]="ariaLabel()">
      <button
        type="button"
        class="mega-pagination__button"
        [disabled]="!hasPrevious()"
        (click)="goTo(page() - 1)"
        aria-label="Página anterior"
      >
        ‹
      </button>

      @for (item of pages(); track item) {
        @if (item === '…') {
          <span class="mega-pagination__ellipsis" aria-hidden="true">…</span>
        } @else {
          <button
            type="button"
            class="mega-pagination__button"
            [class.mega-pagination__button--active]="item === page()"
            [attr.aria-current]="item === page() ? 'page' : null"
            (click)="goTo(item)"
          >
            {{ item }}
          </button>
        }
      }

      <button
        type="button"
        class="mega-pagination__button"
        [disabled]="!hasNext()"
        (click)="goTo(page() + 1)"
        aria-label="Página siguiente"
      >
        ›
      </button>
    </nav>
  `,
})
export class MegaPaginationComponent {
  /** Página actual (1-based). */
  page = input(1);

  /** Tamaño de página. */
  size = input(20);

  /** Total de elementos. */
  total = input(0);

  /** Páginas visibles a izquierda/derecha de la actual. */
  siblings = input(1);

  /** Deshabilita toda la interacción. */
  disabled = input(false, { transform: booleanAttribute });

  /** Texto del resumen personalizado (si no, usa el rango). */
  summaryText = input<string | null>(null);

  /** aria-label del nav. */
  ariaLabel = input('Paginación');

  /** Emite la nueva página (1-based). */
  pageChange = output<number>();

  /** Total de páginas. */
  totalPages = computed(() => {
    const size = Math.max(1, this.size());
    const total = Math.max(0, this.total());
    return Math.max(1, Math.ceil(total / size));
  });

  /** Resumen accesible ("Mostrando 1–20 de 134"). */
  summary = computed(() => {
    const custom = this.summaryText();
    if (custom) {
      return custom;
    }
    const total = this.total();
    if (total === 0) {
      return 'Sin elementos';
    }
    const size = Math.max(1, this.size());
    const from = (this.page() - 1) * size + 1;
    const to = Math.min(this.page() * size, total);
    return `Mostrando ${from}–${to} de ${total}`;
  });

  /** Ventana de páginas con elipses (`[1, '…', 4, 5, 6, '…', 12]`). */
  pages = computed<Array<number | '…'>>(() => {
    const current = Math.min(Math.max(1, this.page()), this.totalPages());
    const siblings = Math.max(0, this.siblings());
    const totalPages = this.totalPages();

    const window: Array<number | '…'> = [];
    const start = Math.max(2, current - siblings);
    const end = Math.min(totalPages - 1, current + siblings);

    window.push(1);
    if (start > 2) {
      window.push('…');
    }
    for (let index = start; index <= end; index += 1) {
      window.push(index);
    }
    if (end < totalPages - 1) {
      window.push('…');
    }
    if (totalPages > 1) {
      window.push(totalPages);
    }
    return window;
  });

  hasPrevious(): boolean {
    return !this.disabled() && this.page() > 1;
  }

  hasNext(): boolean {
    return !this.disabled() && this.page() < this.totalPages();
  }

  goTo(page: number): void {
    if (this.disabled()) {
      return;
    }
    const target = Math.min(Math.max(1, page), this.totalPages());
    if (target !== this.page()) {
      this.pageChange.emit(target);
    }
  }
}
