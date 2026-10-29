import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  TemplateRef,
  computed,
  inject,
  signal,
  viewChild,
} from '@angular/core';
import { FormsModule } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { InvoicingService, PosService } from '@megalujos/api-client';
import {
  PAYMENT_METHOD_LABELS,
  PaymentMethodCode,
  SALE_STATUS_LABELS,
  SaleDetailDto,
  SaleStatusCode,
  SaleSummaryDto,
} from '@megalujos/models';
import {
  MegaButtonComponent,
  MegaDatePickerComponent,
  MegaEmptyStateComponent,
  MegaInputComponent,
  MegaModalComponent,
  MegaPaginationComponent,
  MegaSelectComponent,
  MegaStatusBadgeComponent,
  MegaTableColumn,
  MegaTableCellContext,
  MegaTableComponent,
  MegaToastService,
  formatUsd,
} from '@megalujos/ui';
import {
  downloadBlob,
  endOfDayIso,
  errorMessage,
  formatDate,
  formatDateTime,
  newIdempotencyKey,
  startOfDayIso,
} from '../../core/util/ui';

const STATUS_OPTIONS = [
  { value: '', label: 'Todos los estados' },
  ...Object.entries(SALE_STATUS_LABELS).map(([value, label]) => ({ value, label })),
];

/** Historial de ventas paginado con detalle en modal y emisión de factura. */
@Component({
  selector: 'app-sales-list-page',
  imports: [
    FormsModule,
    MegaInputComponent,
    MegaSelectComponent,
    MegaDatePickerComponent,
    MegaTableComponent,
    MegaPaginationComponent,
    MegaEmptyStateComponent,
    MegaModalComponent,
    MegaStatusBadgeComponent,
    MegaButtonComponent,
  ],
  templateUrl: './sales-list.page.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SalesListPage {
  private readonly pos = inject(PosService);
  private readonly invoicing = inject(InvoicingService);
  private readonly toast = inject(MegaToastService);
  private readonly destroyRef = inject(DestroyRef);

  readonly query = signal('');
  readonly status = signal('');
  readonly dateFrom = signal('');
  readonly dateTo = signal('');
  readonly page = signal(1);
  readonly size = 20;
  readonly total = signal(0);
  readonly rows = signal<SaleSummaryDto[]>([]);
  readonly loading = signal(true);
  readonly error = signal<string | null>(null);
  readonly exporting = signal(false);

  readonly selected = signal<SaleDetailDto | null>(null);
  readonly detailLoading = signal(false);

  readonly statusOptions = STATUS_OPTIONS;

  readonly rowKey = (row: SaleSummaryDto): string => row.saleId ?? row.id;

  private readonly statusCell = viewChild<TemplateRef<MegaTableCellContext<SaleSummaryDto>>>(
    'statusCell',
  );
  private readonly actionsCell = viewChild<TemplateRef<MegaTableCellContext<SaleSummaryDto>>>(
    'actionsCell',
  );

  readonly columns = computed<MegaTableColumn<SaleSummaryDto>[]>(() => [
    { key: 'createdAt', header: 'Fecha', width: '10rem' },
    { key: 'saleNumber', header: 'Venta', width: '9rem' },
    { key: 'customerName', header: 'Cliente' },
    {
      key: 'status',
      header: 'Estado',
      width: '11rem',
      cell: this.statusCell() ?? undefined,
    },
    { key: 'grandTotal', header: 'Total', align: 'end', width: '8rem' },
    {
      header: 'Acciones',
      align: 'end',
      width: '16rem',
      cell: this.actionsCell() ?? undefined,
    },
  ]);

  constructor() {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.error.set(null);
    this.pos
      .listSales({
        q: this.query() || undefined,
        status: this.status() || undefined,
        dateFrom: this.dateFrom() ? startOfDayIso(this.dateFrom()) : undefined,
        dateTo: this.dateTo() ? endOfDayIso(this.dateTo()) : undefined,
        page: this.page() - 1,
        size: this.size,
      })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (page) => {
          this.rows.set((page.items ?? []).map((row) => ({ ...row, createdAt: formatDateTime(row.createdAt) })));
          this.total.set(page.totalElements ?? 0);
          this.loading.set(false);
        },
        error: (err: unknown) => {
          const message = errorMessage(err);
          this.error.set(message);
          this.loading.set(false);
          this.toast.error(message, { title: 'No se pudieron cargar las ventas' });
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

  openDetail(sale: SaleSummaryDto | SaleDetailDto): void {
    this.selected.set(null);
    this.detailLoading.set(true);
    this.pos.getSale(sale.saleId ?? (sale as SaleSummaryDto).id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (detail) => {
          this.selected.set(detail);
          this.detailLoading.set(false);
        },
        error: (err: unknown) => {
          this.detailLoading.set(false);
          this.toast.error(errorMessage(err), { title: 'No se pudo abrir la venta' });
        },
      });
  }

  closeDetail(): void {
    this.selected.set(null);
  }

  canInvoice(sale: SaleDetailDto | null): boolean {
    if (!sale) {
      return false;
    }
    const status = (sale.invoiceStatus ?? '').toUpperCase();
    return !status || status === 'NONE' || status === 'NOT_REQUESTED' || status === 'PENDING';
  }

  issueInvoice(sale: SaleDetailDto): void {
    this.invoicing
      .issue(sale.saleId, 'INVOICE', newIdempotencyKey())
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.toast.success('Documento enviado a la cola de emisión.', {
            title: 'Facturación solicitada',
          });
          this.openDetail(sale);
        },
        error: (err: unknown) =>
          this.toast.error(errorMessage(err), { title: 'No se pudo emitir el documento' }),
      });
  }

  downloadRide(sale: SaleDetailDto): void {
    this.invoicing
      .listDocuments({ size: 100 })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (page) => {
          const document = (page.items ?? []).find(
            (item) => item.saleId === sale.saleId && item.status === 'AUTHORIZED',
          );
          if (!document) {
            this.toast.info('La venta aún no tiene un documento autorizado.');
            return;
          }
          this.invoicing
            .ride(document.id)
            .pipe(takeUntilDestroyed(this.destroyRef))
            .subscribe({
              next: (blob) => downloadBlob(blob, `RIDE-${sale.saleNumber}.pdf`),
              error: (err: unknown) =>
                this.toast.error(errorMessage(err), { title: 'No se pudo descargar el RIDE' }),
            });
        },
        error: (err: unknown) =>
          this.toast.error(errorMessage(err), { title: 'No se pudieron consultar documentos' }),
      });
  }

  statusLabel(code: string): string {
    return SALE_STATUS_LABELS[code as SaleStatusCode] ?? code;
  }

  money(value: string | number | null | undefined): string {
    return formatUsd(value ?? null);
  }

  date(value: string | null | undefined): string {
    return formatDate(value);
  }

  dateTime(value: string | null | undefined): string {
    return formatDateTime(value);
  }

  paymentLabel(method: string): string {
    return PAYMENT_METHOD_LABELS[method as PaymentMethodCode] ?? method;
  }

  get detail(): SaleDetailDto | null {
    return this.selected();
  }
}
