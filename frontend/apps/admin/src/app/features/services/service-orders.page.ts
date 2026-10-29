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
import { Router, RouterLink } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { CustomersService, ServicesService } from '@megalujos/api-client';
import {
  CustomerDto,
  SERVICE_TYPE_LABELS,
  ServiceTypeCode,
  ServiceOrderSummaryDto,
} from '@megalujos/models';
import {
  MegaButtonComponent,
  MegaDatePickerComponent,
  MegaInputComponent,
  MegaModalComponent,
  MegaPaginationComponent,
  MegaSelectComponent,
  MegaStatusBadgeComponent,
  MegaTableColumn,
  MegaTableCellContext,
  MegaTableComponent,
  MegaTextareaComponent,
  MegaToastService,
  formatUsd,
} from '@megalujos/ui';
import { errorMessage, formatDateTime } from '../../core/util/ui';

/** Estados reales del backend con etiquetas/tonos (≠ enums de models). */
export const SERVICE_ORDER_STATUS_LABELS: Record<string, string> = {
  QUOTATION: 'Cotización',
  CONFIRMED: 'Confirmada',
  IN_PROGRESS: 'En ejecución',
  READY_FOR_DELIVERY: 'Lista para entrega',
  DELIVERED: 'Entregada',
  CANCELLED: 'Cancelada',
};

export const SERVICE_ORDER_STATUS_TONES: Record<
  string,
  'success' | 'warning' | 'danger' | 'info' | 'neutral'
> = {
  QUOTATION: 'neutral',
  CONFIRMED: 'info',
  IN_PROGRESS: 'info',
  READY_FOR_DELIVERY: 'warning',
  DELIVERED: 'success',
  CANCELLED: 'danger',
};

const STATUS_FILTER_OPTIONS = [
  { value: '', label: 'Todos los estados' },
  ...Object.entries(SERVICE_ORDER_STATUS_LABELS).map(([value, label]) => ({ value, label })),
];

const TYPE_OPTIONS = (Object.entries(SERVICE_TYPE_LABELS) as Array<[ServiceTypeCode, string]>).map(
  ([value, label]) => ({ value, label }),
);

/** Listado paginado de órdenes de servicio con creación rápida. */
@Component({
  selector: 'app-service-orders-page',
  imports: [
    FormsModule,
    RouterLink,
    MegaInputComponent,
    MegaSelectComponent,
    MegaDatePickerComponent,
    MegaTextareaComponent,
    MegaModalComponent,
    MegaTableComponent,
    MegaPaginationComponent,
    MegaStatusBadgeComponent,
    MegaButtonComponent,
  ],
  templateUrl: './service-orders.page.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ServiceOrdersPage {
  private readonly services = inject(ServicesService);
  private readonly customers = inject(CustomersService);
  private readonly toast = inject(MegaToastService);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);

  readonly statusLabels = SERVICE_ORDER_STATUS_LABELS;
  readonly statusTones = SERVICE_ORDER_STATUS_TONES;

  readonly query = signal('');
  readonly status = signal('');
  readonly page = signal(1);
  readonly size = 20;
  readonly total = signal(0);
  readonly rows = signal<ServiceOrderSummaryDto[]>([]);
  readonly loading = signal(true);

  readonly createOpen = signal(false);
  readonly customerId = signal('');
  readonly serviceType = signal('STICKER');
  readonly description = signal('');
  readonly dimensions = signal('');
  readonly scheduledAt = signal('');
  readonly laborAmount = signal('0.00');
  readonly creating = signal(false);
  readonly customerOptions = signal<Array<{ value: string; label: string }>>([]);

  readonly statusOptions = STATUS_FILTER_OPTIONS;
  readonly typeOptions = TYPE_OPTIONS;

  readonly rowKey = (row: ServiceOrderSummaryDto): string => row.id;

  private readonly statusCell = viewChild<
    TemplateRef<MegaTableCellContext<ServiceOrderSummaryDto>>
  >('statusCell');

  readonly columns = computed<MegaTableColumn<ServiceOrderSummaryDto>[]>(() => [
    { key: 'orderNumber', header: 'Orden', width: '9rem' },
    { key: 'customerName', header: 'Cliente' },
    { key: 'serviceType', header: 'Tipo', width: '9rem' },
    { key: 'status', header: 'Estado', width: '13rem', cell: this.statusCell() ?? undefined },
    { key: 'estimatedTotal', header: 'Total', align: 'end', width: '8rem' },
    { key: 'scheduledAt', header: 'Programada', width: '12rem' },
  ]);

  constructor() {
    this.load();
    this.loadCustomers();
  }

  load(): void {
    this.loading.set(true);
    this.services
      .list({
        q: this.query() || undefined,
        status: this.status() || undefined,
        page: this.page() - 1,
        size: this.size,
      })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (page) => {
          this.rows.set(
            (page.items ?? []).map((row) => ({
              ...row,
              scheduledAt: formatDateTime(row.scheduledAt),
            })),
          );
          this.total.set(page.totalElements ?? 0);
          this.loading.set(false);
        },
        error: (err: unknown) =>
          this.toast.error(errorMessage(err), { title: 'No se pudieron cargar las órdenes' }),
      });
  }

  private loadCustomers(): void {
    this.customers
      .list({ size: 100 })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (page) =>
          this.customerOptions.set(
            (page.items ?? []).map((customer: CustomerDto) => ({
              value: customer.id,
              label: `${customer.name} — ${customer.documentNumber ?? ''}`.trim(),
            })),
          ),
        error: () => this.customerOptions.set([]),
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

  openCreate(): void {
    this.customerId.set('');
    this.serviceType.set('STICKER');
    this.description.set('');
    this.dimensions.set('');
    this.scheduledAt.set('');
    this.laborAmount.set('0.00');
    this.createOpen.set(true);
  }

  create(): void {
    if (!this.customerId()) {
      this.toast.warning('Selecciona el cliente.');
      return;
    }
    if (this.description().trim().length < 5) {
      this.toast.warning('Describe el trabajo (mínimo 5 caracteres).');
      return;
    }
    this.creating.set(true);
    this.services
      .create({
        customerId: this.customerId(),
        serviceType: this.serviceType(),
        description: this.description().trim(),
        dimensions: this.dimensions().trim() || null,
        scheduledAt: this.scheduledAt() ? `${this.scheduledAt()}T09:00:00-05:00` : null,
        laborAmount: this.laborAmount() || '0.00',
        items: [],
        materials: [],
      })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (order) => {
          this.creating.set(false);
          this.createOpen.set(false);
          this.toast.success(`Orden ${order.orderNumber} creada.`);
          this.load();
        },
        error: (err: unknown) => {
          this.creating.set(false);
          this.toast.error(errorMessage(err), { title: 'No se pudo crear la orden' });
        },
      });
  }

  openDetail(row: ServiceOrderSummaryDto): void {
    void this.router.navigate(['/servicios/ordenes', row.id]);
  }

  statusLabel(code: string): string {
    return SERVICE_ORDER_STATUS_LABELS[code] ?? code;
  }

  money(value: string | number | null | undefined): string {
    return formatUsd(value ?? null);
  }
}
