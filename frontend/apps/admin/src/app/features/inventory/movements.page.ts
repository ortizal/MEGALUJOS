import { ChangeDetectionStrategy, Component, DestroyRef, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { InventoryService } from '@megalujos/api-client';
import { StockMovementDto } from '@megalujos/models';
import {
  MegaButtonComponent,
  MegaDatePickerComponent,
  MegaEmptyStateComponent,
  MegaInputComponent,
  MegaPaginationComponent,
  MegaSelectComponent,
  MegaTableComponent,
  MegaToastService,
} from '@megalujos/ui';
import {
  errorMessage,
  formatDateTime,
  formatNumber,
  shortId,
} from '../../core/util/ui';

const MOVEMENT_TYPE_OPTIONS = [
  { value: '', label: 'Todos los tipos' },
  { value: 'PURCHASE', label: 'Compra' },
  { value: 'SALE', label: 'Venta' },
  { value: 'SALE_RETURN', label: 'Devolución de venta' },
  { value: 'ADJUSTMENT_IN', label: 'Ajuste (+)' },
  { value: 'ADJUSTMENT_OUT', label: 'Ajuste (−)' },
  { value: 'SERVICE_CONSUMPTION', label: 'Consumo en servicio' },
  { value: 'INITIAL_STOCK', label: 'Stock inicial' },
];

/** Kardex: movimientos inmutables de inventario con filtros. */
@Component({
  selector: 'app-movements-page',
  imports: [
    FormsModule,
    MegaInputComponent,
    MegaSelectComponent,
    MegaDatePickerComponent,
    MegaTableComponent,
    MegaPaginationComponent,
    MegaEmptyStateComponent,
    MegaButtonComponent,
  ],
  templateUrl: './movements.page.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MovementsPage {
  private readonly inventory = inject(InventoryService);
  private readonly toast = inject(MegaToastService);
  private readonly destroyRef = inject(DestroyRef);

  readonly variantId = signal('');
  readonly movementType = signal('');
  readonly dateFrom = signal('');
  readonly dateTo = signal('');
  readonly page = signal(1);
  readonly size = 20;
  readonly total = signal(0);
  readonly rows = signal<StockMovementDto[]>([]);
  readonly loading = signal(true);
  readonly error = signal<string | null>(null);

  readonly typeOptions = MOVEMENT_TYPE_OPTIONS;

  constructor() {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.error.set(null);
    this.inventory
      .movements({
        variantId: this.variantId().trim() || undefined,
        movementType: this.movementType() || undefined,
        dateFrom: this.dateFrom() ? `${this.dateFrom()}T00:00:00-05:00` : undefined,
        dateTo: this.dateTo() ? `${this.dateTo()}T23:59:59-05:00` : undefined,
        page: this.page() - 1,
        size: this.size,
      })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (page) => {
          this.rows.set(
            (page.items ?? []).map((row) => ({ ...row, createdAt: formatDateTime(row.createdAt) })),
          );
          this.total.set(page.totalElements ?? 0);
          this.loading.set(false);
        },
        error: (err: unknown) => {
          const message = errorMessage(err);
          this.error.set(message);
          this.loading.set(false);
          this.toast.error(message, { title: 'No se pudo cargar el kardex' });
        },
      });
  }

  applyFilters(): void {
    this.page.set(1);
    this.load();
  }

  goToPage(page: number): void {
    this.page.set(page);
    this.load();
  }

  num(value: string | number | null | undefined): string {
    return formatNumber(value);
  }

  shortId(id: string | null | undefined): string {
    return shortId(id);
  }

  movementLabel(type: string): string {
    return MOVEMENT_TYPE_OPTIONS.find((option) => option.value === type)?.label ?? type;
  }
}
