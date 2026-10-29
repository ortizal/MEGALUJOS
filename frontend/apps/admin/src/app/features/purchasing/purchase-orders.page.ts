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
import { RouterLink } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { CatalogAdminService, PurchasingService } from '@megalujos/api-client';
import {
  PURCHASE_ORDER_STATUS_LABELS,
  PurchaseOrderDto,
  SupplierDto,
} from '@megalujos/models';
import {
  MegaButtonComponent,
  MegaConfirmService,
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

interface EditablePoLine {
  variantId: string;
  label: string;
  quantity: string;
  unitCost: string;
}

const STATUS_OPTIONS = [
  { value: '', label: 'Todos los estados' },
  ...Object.entries(PURCHASE_ORDER_STATUS_LABELS).map(([value, label]) => ({ value, label })),
];

/** Órdenes de compra: creación, envío al proveedor y recepción de mercadería. */
@Component({
  selector: 'app-purchase-orders-page',
  imports: [
    FormsModule,
    RouterLink,
    MegaInputComponent,
    MegaSelectComponent,
    MegaTextareaComponent,
    MegaModalComponent,
    MegaTableComponent,
    MegaPaginationComponent,
    MegaStatusBadgeComponent,
    MegaButtonComponent,
  ],
  templateUrl: './purchase-orders.page.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PurchaseOrdersPage {
  private readonly purchasing = inject(PurchasingService);
  private readonly catalog = inject(CatalogAdminService);
  private readonly toast = inject(MegaToastService);
  private readonly confirm = inject(MegaConfirmService);
  private readonly destroyRef = inject(DestroyRef);

  readonly query = signal('');
  readonly status = signal('');
  readonly page = signal(1);
  readonly size = 20;
  readonly total = signal(0);
  readonly rows = signal<PurchaseOrderDto[]>([]);
  readonly loading = signal(true);

  readonly supplierOptions = signal<Array<{ value: string; label: string }>>([]);

  readonly createOpen = signal(false);
  readonly supplierId = signal('');
  readonly notes = signal('');
  readonly lines = signal<EditablePoLine[]>([]);
  readonly productSearch = signal('');
  readonly productResults = signal<Array<{ variantId: string; label: string }>>([]);
  readonly searching = signal(false);
  readonly creating = signal(false);

  readonly receiptOpen = signal(false);
  readonly receiptOrder = signal<PurchaseOrderDto | null>(null);
  readonly receiptQtys = signal<Record<string, string>>({});
  readonly receiptNotes = signal('');
  readonly receiptSubmitting = signal(false);

  readonly statusOptions = STATUS_OPTIONS;

  readonly rowKey = (row: PurchaseOrderDto): string => row.id;

  private readonly statusCell = viewChild<TemplateRef<MegaTableCellContext<PurchaseOrderDto>>>(
    'statusCell',
  );
  private readonly actionsCell = viewChild<TemplateRef<MegaTableCellContext<PurchaseOrderDto>>>(
    'actionsCell',
  );

  readonly columns = computed<MegaTableColumn<PurchaseOrderDto>[]>(() => [
    { key: 'orderNumber', header: 'Orden', width: '9rem' },
    { key: 'supplierName', header: 'Proveedor' },
    { key: 'status', header: 'Estado', width: '12rem', cell: this.statusCell() ?? undefined },
    { key: 'total', header: 'Total', align: 'end', width: '9rem' },
    { key: 'createdAt', header: 'Creada', width: '12rem' },
    {
      header: 'Acciones',
      align: 'end',
      width: '18rem',
      cell: this.actionsCell() ?? undefined,
    },
  ]);

  constructor() {
    this.load();
    this.purchasing
      .listSuppliers({ size: 100 })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (page) =>
          this.supplierOptions.set(
            (page.items ?? [])
              .filter((supplier: SupplierDto) => supplier.active)
              .map((supplier: SupplierDto) => ({ value: supplier.id, label: supplier.name })),
          ),
        error: () => this.supplierOptions.set([]),
      });
  }

  load(): void {
    this.loading.set(true);
    this.purchasing
      .listPurchaseOrders({
        q: this.query() || undefined,
        status: this.status() || undefined,
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
        error: (err: unknown) =>
          this.toast.error(errorMessage(err), { title: 'No se pudieron cargar las órdenes' }),
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
    this.supplierId.set(this.supplierOptions()[0]?.value ?? '');
    this.notes.set('');
    this.lines.set([]);
    this.productResults.set([]);
    this.productSearch.set('');
    this.createOpen.set(true);
  }

  searchProducts(): void {
    const query = this.productSearch().trim();
    if (!query) {
      return;
    }
    this.searching.set(true);
    this.catalog
      .listProducts({ q: query, size: 8 })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (page) => {
          const variants: Array<{ variantId: string; label: string }> = [];
          for (const product of page.items ?? []) {
            for (const variant of product.variants ?? []) {
              if (variant.id) {
                variants.push({
                  variantId: variant.id,
                  label: `${product.name} — ${variant.sku} (${formatUsd(variant.salePrice)})`,
                });
              }
            }
          }
          this.productResults.set(variants.slice(0, 12));
          this.searching.set(false);
        },
        error: (err: unknown) => {
          this.searching.set(false);
          this.toast.error(errorMessage(err), { title: 'No se pudo buscar productos' });
        },
      });
  }

  addLine(variant: { variantId: string; label: string }): void {
    this.lines.update((lines) =>
      lines.some((line) => line.variantId === variant.variantId)
        ? lines
        : [...lines, { variantId: variant.variantId, label: variant.label, quantity: '1', unitCost: '0.00' }],
    );
  }

  removeLine(variantId: string): void {
    this.lines.update((lines) => lines.filter((line) => line.variantId !== variantId));
  }

  updateLine(variantId: string, patch: Partial<EditablePoLine>): void {
    this.lines.update((lines) =>
      lines.map((line) => (line.variantId === variantId ? { ...line, ...patch } : line)),
    );
  }

  create(): void {
    if (!this.supplierId()) {
      this.toast.warning('Selecciona el proveedor.');
      return;
    }
    const lines = this.lines();
    if (!lines.length) {
      this.toast.warning('Agrega al menos un ítem.');
      return;
    }
    this.creating.set(true);
    this.purchasing
      .createPurchaseOrder({
        supplierId: this.supplierId(),
        notes: this.notes().trim() || null,
        items: lines.map((line) => ({
          variantId: line.variantId,
          quantity: line.quantity,
          unitCost: line.unitCost,
        })),
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

  async markOrdered(order: PurchaseOrderDto): Promise<void> {
    const ok = await this.confirm.open({
      title: 'Marcar como ordenada',
      message: `La orden ${order.orderNumber} se marcará como enviada al proveedor.`,
      confirmText: 'Marcar',
    });
    if (!ok) {
      return;
    }
    this.purchasing
      .markOrdered(order.id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.toast.success('Orden actualizada.');
          this.load();
        },
        error: (err: unknown) =>
          this.toast.error(errorMessage(err), { title: 'No se pudo actualizar la orden' }),
      });
  }

  openReceipt(order: PurchaseOrderDto): void {
    this.receiptOrder.set(order);
    this.receiptQtys.set(
      Object.fromEntries((order.items ?? []).map((item) => [item.id, String(item.quantity ?? '0')])),
    );
    this.receiptNotes.set('');
    this.receiptOpen.set(true);
  }

  submitReceipt(): void {
    const order = this.receiptOrder();
    if (!order) {
      return;
    }
    const items = (order.items ?? [])
      .map((item) => ({ orderItemId: item.id, quantity: this.receiptQtys()[item.id] ?? '0' }))
      .filter((item) => Number(item.quantity) > 0);
    if (!items.length) {
      this.toast.warning('Ingresa al menos una cantidad recibida.');
      return;
    }
    this.receiptSubmitting.set(true);
    this.purchasing
      .createReceipt(order.id, { items, notes: this.receiptNotes().trim() || null })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (receipt) => {
          this.receiptSubmitting.set(false);
          this.receiptOpen.set(false);
          this.toast.success(`Recepción ${receipt.receiptNumber} registrada.`);
          this.load();
        },
        error: (err: unknown) => {
          this.receiptSubmitting.set(false);
          this.toast.error(errorMessage(err), { title: 'No se pudo registrar la recepción' });
        },
      });
  }

  setReceiptQty(orderItemId: string, value: string): void {
    this.receiptQtys.update((map) => ({ ...map, [orderItemId]: value }));
  }

  canOrder(order: PurchaseOrderDto): boolean {
    return (order.status ?? '').toUpperCase() === 'DRAFT';
  }

  canReceive(order: PurchaseOrderDto): boolean {
    const status = (order.status ?? '').toUpperCase();
    return status === 'ORDERED' || status === 'PARTIALLY_RECEIVED';
  }

  money(value: string | number | null | undefined): string {
    return formatUsd(value ?? null);
  }
}
