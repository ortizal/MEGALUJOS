import { ChangeDetectionStrategy, Component, DestroyRef, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Observable } from 'rxjs';
import {
  CashClosingDto,
  DailySalesDto,
  MarginDto,
  PopularProductDto,
  SalesByCategoryDto,
  ServiceStatusCountDto,
  TaxSummaryDto,
} from '@megalujos/models';
import { ReportingService } from '@megalujos/api-client';
import {
  MegaButtonComponent,
  MegaDatePickerComponent,
  MegaEmptyStateComponent,
  MegaToastService,
  formatUsd,
} from '@megalujos/ui';
import { daysAgoIso, downloadCsv, errorMessage, formatNumber, todayIso } from '../../core/util/ui';

/** Reportes comerciales con rango de fechas y exportación CSV. */
@Component({
  selector: 'app-reports-page',
  imports: [
    FormsModule,
    MegaDatePickerComponent,
    MegaEmptyStateComponent,
    MegaButtonComponent,
  ],
  templateUrl: './reports.page.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ReportsPage {
  private readonly reporting = inject(ReportingService);
  private readonly toast = inject(MegaToastService);
  private readonly destroyRef = inject(DestroyRef);

  readonly dateFrom = signal(daysAgoIso(30));
  readonly dateTo = signal(todayIso());
  readonly loading = signal(false);
  readonly loaded = signal(false);

  readonly dailySales = signal<DailySalesDto[]>([]);
  readonly popular = signal<PopularProductDto[]>([]);
  readonly byCategory = signal<SalesByCategoryDto[]>([]);
  readonly taxSummary = signal<TaxSummaryDto[]>([]);
  readonly margin = signal<MarginDto[]>([]);
  readonly serviceStatus = signal<ServiceStatusCountDto[]>([]);
  readonly cashClosings = signal<CashClosingDto[]>([]);

  load(): void {
    const range = { dateFrom: this.dateFrom(), dateTo: this.dateTo() };
    this.loading.set(true);
    this.loaded.set(false);
    this.pending = 7;

    this.run(this.reporting.dailySales(range), this.dailySales.set);
    this.run(this.reporting.popularProducts({ ...range, limit: 15 }), this.popular.set);
    this.run(this.reporting.salesByCategory(range), this.byCategory.set);
    this.run(this.reporting.taxSummary(range), this.taxSummary.set);
    this.run(this.reporting.margin(range), this.margin.set);
    this.run(this.reporting.servicesByStatus(), this.serviceStatus.set);
    this.run(this.reporting.cashClosings(range), this.cashClosings.set);
  }

  private pending = 0;

  private run<T>(source: Observable<T[]>, setter: (rows: T[]) => void): void {
    source.pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: (rows) => setter(rows ?? []),
      error: (err: unknown) => {
        setter([]);
        this.toast.error(errorMessage(err));
        this.tick();
      },
      complete: () => this.tick(),
    });
  }

  private tick(): void {
    this.pending = Math.max(0, this.pending - 1);
    if (this.pending === 0) {
      this.loading.set(false);
      this.loaded.set(true);
    }
  }

  exportDailySales(): void {
    downloadCsv(
      this.dailySales().map((row) => ({
        fecha: row.date,
        cantidad: row.count,
        subtotal: row.subtotal,
        impuestos: row.taxTotal,
        total: row.total,
      })),
      `ventas-diarias-${this.dateFrom()}_${this.dateTo()}.csv`,
    );
  }

  exportPopular(): void {
    downloadCsv(
      this.popular().map((row) => ({
        producto: row.productName,
        cantidad: row.quantitySold,
        ingresos: row.totalRevenue,
      })),
      `productos-populares-${this.dateFrom()}_${this.dateTo()}.csv`,
    );
  }

  totalOf(rows: DailySalesDto[]): string {
    return formatUsd(rows.reduce((acc, row) => acc + (Number(row.total) || 0), 0));
  }

  num(value: string | number | null | undefined): string {
    return formatNumber(value);
  }

  money(value: string | number | null | undefined): string {
    return formatUsd(value ?? null);
  }
}
