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
import { CashService } from '@megalujos/api-client';
import {
  CASH_SESSION_STATUS_LABELS,
  CashRegisterDto,
  CashSessionDto,
  CashSessionStatus,
} from '@megalujos/models';
import {
  MegaButtonComponent,
  MegaEmptyStateComponent,
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

const SESSION_STATUS_OPTIONS = [
  { value: '', label: 'Todas las sesiones' },
  { value: 'OPEN', label: CASH_SESSION_STATUS_LABELS.OPEN },
  { value: 'CLOSED', label: CASH_SESSION_STATUS_LABELS.CLOSED },
];

/** Historial de sesiones de caja y apertura de una nueva sesión. */
@Component({
  selector: 'app-cash-sessions-page',
  imports: [
    FormsModule,
    RouterLink,
    MegaInputComponent,
    MegaSelectComponent,
    MegaTextareaComponent,
    MegaTableComponent,
    MegaPaginationComponent,
    MegaEmptyStateComponent,
    MegaModalComponent,
    MegaStatusBadgeComponent,
    MegaButtonComponent,
  ],
  templateUrl: './cash-sessions.page.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CashSessionsPage {
  private readonly cash = inject(CashService);
  private readonly toast = inject(MegaToastService);
  private readonly destroyRef = inject(DestroyRef);

  readonly rows = signal<CashSessionDto[]>([]);
  readonly total = signal(0);
  readonly page = signal(1);
  readonly size = 20;
  readonly status = signal('');
  readonly loading = signal(true);
  readonly error = signal<string | null>(null);

  readonly active = signal<CashSessionDto | null>(null);
  readonly registers = signal<CashRegisterDto[]>([]);

  readonly openModal = signal(false);
  readonly registerId = signal('');
  readonly openingAmount = signal('0.00');
  readonly notes = signal('');
  readonly submitting = signal(false);

  readonly statusOptions = SESSION_STATUS_OPTIONS;

  readonly rowKey = (row: CashSessionDto): string => row.id;

  private readonly statusCell = viewChild<TemplateRef<MegaTableCellContext<CashSessionDto>>>(
    'statusCell',
  );
  private readonly actionsCell = viewChild<TemplateRef<MegaTableCellContext<CashSessionDto>>>(
    'actionsCell',
  );

  readonly columns = computed<MegaTableColumn<CashSessionDto>[]>(() => [
    { key: 'openedAt', header: 'Apertura', width: '12rem' },
    { key: 'registerName', header: 'Caja' },
    { key: 'openingAmount', header: 'Fondo', align: 'end', width: '8rem' },
    { key: 'countedAmount', header: 'Contado', align: 'end', width: '8rem' },
    {
      key: 'status',
      header: 'Estado',
      width: '10rem',
      cell: this.statusCell() ?? undefined,
    },
    {
      header: 'Acciones',
      align: 'end',
      width: '10rem',
      cell: this.actionsCell() ?? undefined,
    },
  ]);

  constructor() {
    this.load();
    this.loadRegisters();
    this.loadActive();
  }

  load(): void {
    this.loading.set(true);
    this.error.set(null);
    this.cash
      .sessions({ status: this.status() || undefined, page: this.page() - 1, size: this.size })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (page) => {
          this.rows.set(
            (page.items ?? []).map((row) => ({ ...row, openedAt: formatDateTime(row.openedAt) })),
          );
          this.total.set(page.totalElements ?? 0);
          this.loading.set(false);
        },
        error: (err: unknown) => {
          const message = errorMessage(err);
          this.error.set(message);
          this.loading.set(false);
          this.toast.error(message, { title: 'No se pudieron cargar las sesiones' });
        },
      });
  }

  private loadRegisters(): void {
    this.cash
      .registers()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (registers) => {
          const list = (registers ?? []).filter((register) => register.active);
          this.registers.set(list);
          if (list.length === 1) {
            this.registerId.set(list[0].id);
          }
        },
        error: (err: unknown) =>
          this.toast.error(errorMessage(err), { title: 'No se pudieron cargar las cajas' }),
      });
  }

  private loadActive(): void {
    this.cash
      .activeSession()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (session) => this.active.set(session && session.id ? session : null),
        error: () => this.active.set(null),
      });
  }

  registerOptions(): Array<{ value: string; label: string }> {
    return this.registers().map((register) => ({ value: register.id, label: register.name }));
  }

  openOpenModal(): void {
    this.openingAmount.set('0.00');
    this.notes.set('');
    this.openModal.set(true);
  }

  submitOpen(): void {
    const registerId = this.registerId();
    if (!registerId) {
      this.toast.warning('Selecciona una caja.');
      return;
    }
    const amount = Number(this.openingAmount());
    if (Number.isNaN(amount) || amount < 0) {
      this.toast.warning('Ingresa el fondo inicial.');
      return;
    }
    this.submitting.set(true);
    this.cash
      .openSession({
        registerId,
        openingAmount: amount.toFixed(2),
        notes: this.notes().trim() || null,
      })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (session) => {
          this.submitting.set(false);
          this.openModal.set(false);
          this.toast.success('Caja abierta correctamente.', { title: 'Sesión abierta' });
          this.active.set(session);
          this.load();
        },
        error: (err: unknown) => {
          this.submitting.set(false);
          this.toast.error(errorMessage(err), { title: 'No se pudo abrir la caja' });
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

  statusLabel(code: string): string {
    return CASH_SESSION_STATUS_LABELS[code as CashSessionStatus] ?? code;
  }

  money(value: string | number | null | undefined): string {
    return formatUsd(value ?? null);
  }
}
