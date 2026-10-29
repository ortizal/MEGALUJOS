import { ChangeDetectionStrategy, Component, DestroyRef, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { InventoryService } from '@megalujos/api-client';
import { StockAlertDto } from '@megalujos/models';
import {
  MegaButtonComponent,
  MegaEmptyStateComponent,
  MegaStockBadgeComponent,
  MegaToastService,
} from '@megalujos/ui';
import { errorMessage, formatNumber, shortId } from '../../core/util/ui';

/** Variantes por debajo del stock mínimo. */
@Component({
  selector: 'app-alerts-page',
  imports: [
    RouterLink,
    MegaEmptyStateComponent,
    MegaStockBadgeComponent,
    MegaButtonComponent,
  ],
  templateUrl: './alerts.page.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AlertsPage {
  private readonly inventory = inject(InventoryService);
  private readonly toast = inject(MegaToastService);
  private readonly destroyRef = inject(DestroyRef);

  readonly rows = signal<StockAlertDto[]>([]);
  readonly loading = signal(true);
  readonly error = signal<string | null>(null);

  constructor() {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.error.set(null);
    this.inventory
      .alerts()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (alerts) => {
          this.rows.set(alerts ?? []);
          this.loading.set(false);
        },
        error: (err: unknown) => {
          const message = errorMessage(err);
          this.error.set(message);
          this.loading.set(false);
          this.toast.error(message, { title: 'No se pudieron cargar las alertas' });
        },
      });
  }

  num(value: string | number | null | undefined): string {
    return formatNumber(value);
  }

  shortId(id: string | null | undefined): string {
    return shortId(id);
  }
}
