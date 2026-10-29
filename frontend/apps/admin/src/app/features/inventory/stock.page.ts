import { ChangeDetectionStrategy, Component, DestroyRef, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { InventoryService } from '@megalujos/api-client';
import { StockBalanceDto } from '@megalujos/models';
import {
  MegaButtonComponent,
  MegaEmptyStateComponent,
  MegaInputComponent,
  MegaPaginationComponent,
  MegaStockBadgeComponent,
  MegaTableComponent,
  MegaToastService,
  formatUsd,
} from '@megalujos/ui';
import { errorMessage, formatNumber, shortId } from '../../core/util/ui';

/** Saldos de stock por variante y almacén. */
@Component({
  selector: 'app-stock-page',
  imports: [
    FormsModule,
    MegaInputComponent,
    MegaTableComponent,
    MegaPaginationComponent,
    MegaEmptyStateComponent,
    MegaStockBadgeComponent,
    MegaButtonComponent,
  ],
  templateUrl: './stock.page.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class StockPage {
  private readonly inventory = inject(InventoryService);
  private readonly toast = inject(MegaToastService);
  private readonly destroyRef = inject(DestroyRef);

  readonly query = signal('');
  readonly lowStockOnly = signal(false);
  readonly page = signal(1);
  readonly size = 20;
  readonly total = signal(0);
  readonly rows = signal<StockBalanceDto[]>([]);
  readonly loading = signal(true);
  readonly error = signal<string | null>(null);

  constructor() {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.error.set(null);
    this.inventory
      .balances({
        q: this.query() || undefined,
        lowStockOnly: this.lowStockOnly() || undefined,
        page: this.page() - 1,
        size: this.size,
      })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (page) => {
          this.rows.set(page.items ?? []);
          this.total.set(page.totalElements ?? 0);
          this.loading.set(false);
        },
        error: (err: unknown) => {
          const message = errorMessage(err);
          this.error.set(message);
          this.loading.set(false);
          this.toast.error(message, { title: 'No se pudo cargar el stock' });
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

  money(value: string | number | null | undefined): string {
    return formatUsd(value ?? null);
  }

  shortId(id: string): string {
    return shortId(id);
  }
}
