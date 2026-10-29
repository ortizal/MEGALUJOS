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
import { StockBalanceDto, StockCountDto } from '@megalujos/models';
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
import { errorMessage, formatDateTime, formatNumber } from '../../core/util/ui';

/** Fila de conteo físico (cantidad contada absoluta). */
interface CountLine {
  variantId: string;
  label: string;
  systemQuantity: string;
  countedQuantity: string;
}

/** Conteos físicos: creación, completado y aprobación de diferencias. */
@Component({
  selector: 'app-counts-page',
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
  templateUrl: './counts.page.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CountsPage {
  private readonly inventory = inject(InventoryService);
  private readonly toast = inject(MegaToastService);
  private readonly confirm = inject(MegaConfirmService);
  private readonly destroyRef = inject(DestroyRef);

  readonly rows = signal<StockCountDto[]>([]);
  readonly total = signal(0);
  readonly page = signal(1);
  readonly size = 20;
  readonly loading = signal(true);

  readonly search = signal('');
  readonly searchResults = signal<StockBalanceDto[]>([]);
  readonly searching = signal(false);
  readonly lines = signal<CountLine[]>([]);
  readonly notes = signal('');
  readonly submitting = signal(false);

  readonly selected = signal<StockCountDto | null>(null);

  readonly rowKey = (row: StockCountDto): string => row.id;

  private readonly statusCell = viewChild<TemplateRef<MegaTableCellContext<StockCountDto>>>(
    'statusCell',
  );
  private readonly actionsCell = viewChild<TemplateRef<MegaTableCellContext<StockCountDto>>>(
    'actionsCell',
  );

  readonly columns = computed<MegaTableColumn<StockCountDto>[]>(() => [
    { key: 'countNumber', header: 'Número', width: '10rem' },
    { key: 'createdAt', header: 'Fecha', width: '12rem' },
    { key: 'status', header: 'Estado', width: '10rem', cell: this.statusCell() ?? undefined },
    { key: 'notes', header: 'Notas' },
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
    this.inventory
      .listCounts({ page: this.page() - 1, size: this.size })
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
          this.toast.error(errorMessage(err), { title: 'No se pudieron cargar los conteos' });
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
        },
        error: (err: unknown) => {
          this.searching.set(false);
          this.toast.error(errorMessage(err), { title: 'No se pudo buscar' });
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
              systemQuantity: String(balance.quantity ?? '0'),
              countedQuantity: String(balance.quantity ?? '0'),
            },
          ],
    );
  }

  removeLine(variantId: string): void {
    this.lines.update((lines) => lines.filter((line) => line.variantId !== variantId));
  }

  setCounted(variantId: string, value: string): void {
    this.lines.update((lines) =>
      lines.map((line) => (line.variantId === variantId ? { ...line, countedQuantity: value } : line)),
    );
  }

  submit(): void {
    const lines = this.lines();
    if (!lines.length) {
      this.toast.warning('Agrega al menos una variante al conteo.');
      return;
    }
    this.submitting.set(true);
    this.inventory
      .createCount({
        notes: this.notes().trim() || null,
        items: lines.map((line) => ({
          variantId: line.variantId,
          countedQuantity: line.countedQuantity,
        })),
      })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (count) => {
          this.submitting.set(false);
          this.lines.set([]);
          this.notes.set('');
          this.searchResults.set([]);
          this.search.set('');
          this.toast.success(`Conteo ${count.countNumber} creado.`);
          this.load();
        },
        error: (err: unknown) => {
          this.submitting.set(false);
          this.toast.error(errorMessage(err), { title: 'No se pudo crear el conteo' });
        },
      });
  }

  openDetail(count: StockCountDto): void {
    this.inventory
      .getCount(count.id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (detail) => this.selected.set(detail),
        error: (err: unknown) =>
          this.toast.error(errorMessage(err), { title: 'No se pudo abrir el conteo' }),
      });
  }

  complete(count: StockCountDto): void {
    this.inventory
      .completeCount(count.id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.toast.success('Conteo completado. Listo para aprobación.');
          this.load();
        },
        error: (err: unknown) =>
          this.toast.error(errorMessage(err), { title: 'No se pudo completar el conteo' }),
      });
  }

  async approve(count: StockCountDto): Promise<void> {
    const ok = await this.confirm.open({
      title: 'Aprobar conteo',
      message: `El conteo ${count.countNumber} aplicará las diferencias al stock.`,
      confirmText: 'Aprobar',
      danger: true,
    });
    if (!ok) {
      return;
    }
    this.inventory
      .approveCount(count.id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.toast.success('Conteo aprobado y aplicado.');
          this.load();
        },
        error: (err: unknown) =>
          this.toast.error(errorMessage(err), { title: 'No se pudo aprobar el conteo' }),
      });
  }

  canComplete(count: StockCountDto): boolean {
    return (count.status ?? '').toUpperCase() === 'OPEN';
  }

  canApprove(count: StockCountDto): boolean {
    return (count.status ?? '').toUpperCase() === 'COUNTED';
  }

  num(value: string | number | null | undefined): string {
    return formatNumber(value);
  }
}
