import {
  Component,
  TemplateRef,
  booleanAttribute,
  input,
  output,
} from '@angular/core';
import { NgTemplateOutlet } from '@angular/common';

/** Contexto pasado a las plantillas de celda (`let-row`, `let-index`, `let-column`). */
export interface MegaTableCellContext<T = unknown> {
  $implicit: T;
  row: T;
  index: number;
  column: MegaTableColumn<T>;
}

/** Definición de columna de la tabla. */
export interface MegaTableColumn<T = unknown> {
  /** Ruta del valor en la fila (`"customer.name"`); opcional si se usa `cell`. */
  key?: string;
  /** Texto de la cabecera. */
  header: string;
  /** Alineación del contenido. */
  align?: 'start' | 'center' | 'end';
  /** Ancho CSS de la columna (ej. `"12rem"`). */
  width?: string;
  /** Habilita el ordenamiento por cabecera. */
  sortable?: boolean;
  /** Plantilla de celda personalizada (TemplateRef del contenedor). */
  cell?: TemplateRef<MegaTableCellContext<T>>;
}

/** Estado de orden enviado en `sortChange`. */
export interface MegaTableSort {
  key: string;
  direction: 'asc' | 'desc';
}

/**
 * Tabla genérica MEGALUJOS: columnas configurables, celdas con plantilla,
 * estados de carga/vacío, ordenamiento por cabecera y fila clicable.
 */
@Component({
  selector: 'mega-table',
  standalone: true,
  imports: [NgTemplateOutlet],
  styleUrl: './mega-table.component.scss',
  template: `
    <div class="mega-table__scroll">
      <table class="mega-table__table" [attr.aria-busy]="loading()">
        <thead class="mega-table__head">
          <tr>
            @for (column of columns(); track column.key ?? column.header) {
              <th
                scope="col"
                [style.width]="column.width ?? null"
                [style.text-align]="column.align ?? 'start'"
                [class.mega-table__th--sortable]="column.sortable"
                [class.mega-table__th--align-end]="column.align === 'end'"
                [attr.aria-sort]="ariaSort(column)"
                [attr.tabindex]="column.sortable ? 0 : null"
                (click)="onSort(column)"
                (keydown.enter)="onSort(column)"
                (keydown.space)="onSort(column)"
              >
                <span class="mega-table__th-label">{{ column.header }}</span>
                @if (column.sortable) {
                  <span class="mega-table__sort" aria-hidden="true">
                    {{ sortGlyph(column) }}
                  </span>
                }
              </th>
            }
          </tr>
        </thead>

        <tbody>
          @if (loading()) {
            @for (row of skeletonRows(); track row) {
              <tr class="mega-table__row mega-table__row--skeleton">
                @for (column of columns(); track column.key ?? column.header) {
                  <td [style.text-align]="column.align ?? 'start'">
                    <span class="mega-table__skeleton"></span>
                  </td>
                }
              </tr>
            }
          } @else if (rows().length === 0) {
            <tr class="mega-table__row mega-table__row--empty">
              <td [attr.colspan]="columns().length">
                <span class="mega-table__empty">{{ emptyText() }}</span>
              </td>
            </tr>
          } @else {
            @for (row of rows(); track rowKey()(row, $index); let i = $index) {
              <tr
                class="mega-table__row"
                [class.mega-table__row--clickable]="clickable()"
                (click)="onRowClick.emit({ row, index: i })"
              >
                @for (column of columns(); track column.key ?? column.header) {
                  <td
                    [style.text-align]="column.align ?? 'start'"
                    [class.mega-table__td--align-end]="column.align === 'end'"
                  >
                    @if (column.cell) {
                      <ng-container
                        *ngTemplateOutlet="column.cell; context: cellContext(row, i, column)"
                      />
                    } @else {
                      <span class="mega-table__text">{{ valueFor(row, column.key) }}</span>
                    }
                  </td>
                }
              </tr>
            }
          }
        </tbody>
      </table>
    </div>
  `,
})
export class MegaTableComponent<T = unknown> {
  /** Definición de columnas. */
  columns = input<MegaTableColumn<T>[]>([]);

  /** Filas a renderizar. */
  rows = input<T[]>([]);

  /** true mientras carga (muestra filas esqueleto). */
  loading = input(false, { transform: booleanAttribute });

  /** Cantidad de filas esqueleto en estado de carga. */
  loadingRows = input(5);

  /** Texto del estado vacío. */
  emptyText = input('Sin resultados');

  /** Clave de fila para `@for` (evita re-renderizados innecesarios). */
  rowKey = input<(row: T, index: number) => string | number>((_row, index) => index);

  /** Orden activo controlado por el contenedor. */
  sort = input<MegaTableSort | null>(null);

  /** Habilita el estilo de fila clicable (y el cursor pointer). */
  clickable = input(false, { transform: booleanAttribute });

  /** Emite cuando el usuario ordena por una columna `sortable`. */
  sortChange = output<MegaTableSort>();

  /** Emite al pulsar una fila. */
  onRowClick = output<{ row: T; index: number }>();

  /** Filas de esqueleto (señal derivada de `loadingRows`). */
  skeletonRows(): number[] {
    const count = Math.max(1, this.loadingRows());
    return Array.from({ length: count }, (_value, index) => index);
  }

  /** Valor de la celda a partir de la ruta `key`. */
  valueFor(row: T, key?: string): unknown {
    if (!key) {
      return '';
    }
    const parts = key.split('.');
    let current: unknown = row;
    for (const part of parts) {
      if (current === null || current === undefined || typeof current !== 'object') {
        return '';
      }
      current = (current as Record<string, unknown>)[part];
    }
    if (current === null || current === undefined) {
      return '';
    }
    if (typeof current === 'string' || typeof current === 'number' || typeof current === 'boolean') {
      return current;
    }
    return '';
  }

  /** Contexto de plantilla de celda. */
  cellContext(row: T, index: number, column: MegaTableColumn<T>): MegaTableCellContext<T> {
    return { $implicit: row, row, index, column };
  }

  /** Ordena al pulsar la cabecera (alterna asc/desc). */
  onSort(column: MegaTableColumn<T>): void {
    if (!column.sortable || !column.key) {
      return;
    }
    const current = this.sort();
    const direction: 'asc' | 'desc' =
      current && current.key === column.key && current.direction === 'asc' ? 'desc' : 'asc';
    this.sortChange.emit({ key: column.key, direction });
  }

  /** `aria-sort` de la cabecera actual. */
  ariaSort(column: MegaTableColumn<T>): 'ascending' | 'descending' | 'none' {
    const current = this.sort();
    if (!current || current.key !== column.key) {
      return 'none';
    }
    return current.direction === 'asc' ? 'ascending' : 'descending';
  }

  /** Glifo visual del orden (▲/▼/↕). */
  sortGlyph(column: MegaTableColumn<T>): string {
    const current = this.sort();
    if (!current || current.key !== column.key) {
      return '↕';
    }
    return current.direction === 'asc' ? '▲' : '▼';
  }
}
