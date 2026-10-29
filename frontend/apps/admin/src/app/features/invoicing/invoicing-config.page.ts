import { ChangeDetectionStrategy, Component, DestroyRef, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { InvoicingConfigDto, InvoicingService } from '@megalujos/api-client';
import {
  MegaButtonComponent,
  MegaEmptyStateComponent,
  MegaStatusBadgeComponent,
  MegaToastService,
  formatUsd,
} from '@megalujos/ui';
import { errorMessage } from '../../core/util/ui';

/** Configuración de facturación electrónica (emisor, ambiente SRI, reglas fiscales). */
@Component({
  selector: 'app-invoicing-config-page',
  imports: [
    RouterLink,
    MegaEmptyStateComponent,
    MegaStatusBadgeComponent,
    MegaButtonComponent,
  ],
  templateUrl: './invoicing-config.page.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class InvoicingConfigPage {
  private readonly invoicing = inject(InvoicingService);
  private readonly toast = inject(MegaToastService);
  private readonly destroyRef = inject(DestroyRef);

  readonly config = signal<InvoicingConfigDto | null>(null);
  readonly loading = signal(true);
  readonly error = signal<string | null>(null);

  constructor() {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.error.set(null);
    this.invoicing
      .config()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (config) => {
          this.config.set(config);
          this.loading.set(false);
        },
        error: (err: unknown) => {
          const message = errorMessage(err);
          this.error.set(message);
          this.loading.set(false);
          this.toast.error(message, { title: 'No se pudo cargar la configuración' });
        },
      });
  }

  percent(rate: string): string {
    const numeric = Number(rate);
    if (Number.isNaN(numeric)) {
      return rate;
    }
    return `${(numeric * 100).toFixed(2)}%`;
  }

  money(value: string | number | null | undefined): string {
    return formatUsd(value ?? null);
  }
}
