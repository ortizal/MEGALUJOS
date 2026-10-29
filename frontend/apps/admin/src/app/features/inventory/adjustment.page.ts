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
import { InventoryService } from '@megalujos/api-client';
import { AdjustmentDto, StockBalanceDto } from '@megalujos/models';
import {
  MegaButtonComponent,
  MegaConfirmService,
  MegaEmptyStateComponent,
  MegaInputComponent,
  MegaModalComponent,
  MegaPaginationComponent,
  MegaStatusBadgeComponent,
  MegaTableColumn,
  MegaTableCellContext,
  MegaTableComponent,
  MegaTextareaComponent,
  MegaToastService,
} from '@megalujos/ui';
import { errorMessage, formatDateTime, formatNumber, shortId } from '../../core/util/ui';

/** Fila de item a ajustar (cantidad absoluta final). */
interface AdjustmentLine {
  variantId: string;
  label: string;
  currentQuantity: string;
  adjustedQuantity: string;
}

/** Ajustes de inventario: creación con cantidad absoluta final y aprobación. */
@Component({
  selector: 'app-adjustment-page',
  imports: [
    FormsModule,
    MegaInputComponent,
    MegaTextareaComponent,
    MegaModalComponent,
    MegaTableComponent,
    MegaPaginationComponent,
    MegaEmptyStateComponent,
    MegaStatusBadgeComponent,
    MegaButtonComponent,
  ],
  templateUrl: './adjustment.page.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AdjustmentPage {
  private readonly inventory = inject(InventoryService);
  private readonly toast = inject(MegaToastService);
  private readonly confirm = inject(MegaConfirmService);
  private readonly destroyRef = inject(DestroyRef);

  readonly rows = signal<AdjustmentDto[]>([]);
  readonly total = signal(0);
  readonly page = signal(1);
  readonly size = 20;
  readonly loading = signal(true);

  readonly search = signal('');
  readonly searchResults = signal<StockBalanceDto[]>([]);
  readonly searching = signal(false);

  readonly lines = signal<AdjustmentLine[]>([]);
  readonly reason = signal('');
  readonly submitting = signal(false);

  readonly rowKey = (row: AdjustmentDto): string => row.id;

  private readonly statusCell = viewChild<TemplateRef<MegaTableCellContext<AdjustmentDto>>>(
    'statusCell',
  );
  private readonly actionsCell = viewChild<TemplateRef<MegaTableCellContext<AdjustmentDto>>>(
    'actionsCell',
  );

  readonly columns = computed<MegaTableColumn<AdjustmentDto>[]>(() => [
    { key: 'adjustmentNumber', header: 'Número', width: '10rem' },
    { key: 'createdAt', header: 'Fecha', width: '12rem' },
    { key: 'status', header: 'Estado', width: '10rem', cell: this.statusCell() ?? undefined },
    { key: 'reason', header: 'Motivo' },
    {
      header: 'Acciones',
      align: 'end',
      width: '10rem',
      cell: this.actionsCell() ?? undefined,
    },
  ]);

  constructor() {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.inventory
      .listAdjustments({ page: this.page() - 1, size: this.size })
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
          this.loading.set(false);
          this.toast.error(errorMessage(err), { title: 'No se pudieron cargar los ajustes' });
        },
      });
  }

  goToPage(page: number): void {
    this.page.set(page);
    this.load();
  }

  searchVariants(): void {
    const query = this.search().trim();
    if (!query) {
      return;
    }
    this.searching.set(true);
    this.inventory
      .balances({ q: query, size: 10 })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (page) => {
          this.searchResults.set(page.items ?? []);
          this.searching.set(false);
          if (!(page.items ?? []).length) {
            this.toast.info('Sin resultados para esa búsqueda.');
          }
        },
        error: (err: unknown) => {
          this.searching.set(false);
          this.toast.error(errorMessage(err), { title: 'No se pudo buscar la variante' });
        },
      });
  }

  addLine(balance: StockBalanceDto): void {
    this.lines.update((lines) =>
      lines.some((line) => line.variantId === balance.variantId)
        ? lines
        : [
            ...lines,
            {
              variantId: balance.variantId,
              label: `${balance.sku} — ${balance.productName}`,
              currentQuantity: String(balance.quantity ?? '0'),
              adjustedQuantity: String(balance.quantity ?? '0'),
            },
          ],
    );
  }

  removeLine(variantId: string): void {
    this.lines.update((lines) => lines.filter((line) => line.variantId !== variantId));
  }

  setAdjusted(variantId: string, value: string): void {
    this.lines.update((lines) =>
      lines.map((line) => (line.variantId === variantId ? { ...line, adjustedQuantity: value } : line)),
    );
  }

  submit(): void {
    const lines = this.lines();
    if (!lines.length) {
      this.toast.warning('Agrega al menos una variante al ajuste.');
      return;
    }
    if (this.reason().trim().length < 5) {
      this.toast.warning('Describe el motivo del ajuste.');
      return;
    }
    this.submitting.set(true);
    this.inventory
      .createAdjustment({
        reason: this.reason().trim(),
        items: lines.map((line) => ({
          variantId: line.variantId,
          adjustedQuantity: line.adjustedQuantity,
        })),
      })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (adjustment) => {
          this.submitting.set(false);
          this.lines.set([]);
          this.reason.set('');
          this.searchResults.set([]);
          this.search.set('');
          this.toast.success(`Ajuste ${adjustment.adjustmentNumber} creado (borrador).`);
          this.load();
        },
        error: (err: unknown) => {
          this.submitting.set(false);
          this.toast.error(errorMessage(err), { title: 'No se pudo crear el ajuste' });
        },
      });
  }

  async approve(adjustment: AdjustmentDto): Promise<void> {
    const ok = await this.confirm.open({
      title: 'Aprobar ajuste',
      message: `El ajuste ${adjustment.adjustmentNumber} aplicará las diferencias al stock definitivamente.`,
      confirmText: 'Aprobar',
      danger: true,
    });
    if (!ok) {
      return;
    }
    this.inventory
      .approveAdjustment(adjustment.id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.toast.success('Ajuste aprobado y aplicado al stock.');
          this.load();
        },
        error: (err: unknown) =>
          this.toast.error(errorMessage(err), { title: 'No se pudo aprobar el ajuste' }),
      });
  }

  num(value: string | number | null | undefined): string {
    return formatNumber(value);
  }

  canApprove(adjustment: AdjustmentDto): boolean {
    return (adjustment.status ?? '').toUpperCase() === 'DRAFT';
  }

  shortId(id: string | null | undefined): string {
    return shortId(id);
  }
}
