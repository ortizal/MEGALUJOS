import { ChangeDetectionStrategy, Component, DestroyRef, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { CashService } from '@megalujos/api-client';
import {
  PAYMENT_METHOD_LABELS,
  PaymentMethodCode,
  CashCountLine,
  CashMovementDto,
  CashReconciliationDto,
  CashSessionDto,
} from '@megalujos/models';
import {
  MegaButtonComponent,
  MegaCashSummaryComponent,
  MegaEmptyStateComponent,
  MegaInputComponent,
  MegaModalComponent,
  MegaSelectComponent,
  MegaStatusBadgeComponent,
  MegaTextareaComponent,
  MegaToastService,
  formatUsd,
} from '@megalujos/ui';
import {
  errorMessage,
  formatDateTime,
  formatNumber,
  printHtml,
} from '../../core/util/ui';

const MOVEMENT_TYPE_OPTIONS = [
  { value: 'INCOME', label: 'Ingreso' },
  { value: 'EXPENSE', label: 'Retiro / Egreso' },
];

const METHOD_LABELS = PAYMENT_METHOD_LABELS as Record<string, string>;

/** Detalle de una sesión de caja: arqueo, movimientos y cierre. */
@Component({
  selector: 'app-cash-session-detail-page',
  imports: [
    FormsModule,
    RouterLink,
    MegaInputComponent,
    MegaSelectComponent,
    MegaTextareaComponent,
    MegaCashSummaryComponent,
    MegaEmptyStateComponent,
    MegaModalComponent,
    MegaStatusBadgeComponent,
    MegaButtonComponent,
  ],
  templateUrl: './cash-session-detail.page.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CashSessionDetailPage {
  private readonly cash = inject(CashService);
  private readonly toast = inject(MegaToastService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly route = inject(ActivatedRoute);

  readonly sessionId = this.route.snapshot.paramMap.get('id') ?? '';

  readonly session = signal<CashSessionDto | null>(null);
  readonly reconciliation = signal<CashReconciliationDto | null>(null);
  readonly movements = signal<CashMovementDto[]>([]);
  readonly loading = signal(true);
  readonly error = signal<string | null>(null);

  readonly movementType = signal('INCOME');
  readonly movementAmount = signal('0.00');
  readonly movementReason = signal('');
  readonly movementSubmitting = signal(false);

  readonly closeModal = signal(false);
  readonly countedAmount = signal('0.00');
  readonly closeNotes = signal('');
  readonly closeSubmitting = signal(false);

  readonly methodOptions = MOVEMENT_TYPE_OPTIONS;

  constructor() {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.error.set(null);
    this.cash
      .sessions({ size: 100 })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (page) => {
          const found = (page.items ?? []).find((row) => row.id === this.sessionId);
          if (found) {
            this.session.set(found);
            this.countedAmount.set(found.countedAmount ?? '0.00');
            this.loadDetail();
          } else {
            this.cash
              .activeSession()
              .pipe(takeUntilDestroyed(this.destroyRef))
              .subscribe({
                next: (active) => {
                  if (active && active.id === this.sessionId) {
                    this.session.set(active);
                    this.loadDetail();
                  } else {
                    this.loading.set(false);
                    this.error.set('No se encontró la sesión de caja solicitada.');
                  }
                },
                error: (err: unknown) => {
                  this.loading.set(false);
                  this.error.set(errorMessage(err));
                },
              });
          }
        },
        error: (err: unknown) => {
          const message = errorMessage(err);
          this.error.set(message);
          this.loading.set(false);
          this.toast.error(message, { title: 'No se pudo cargar la sesión' });
        },
      });
  }

  private loadDetail(): void {
    this.cash
      .reconciliation(this.sessionId)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (reconciliation) => {
          this.reconciliation.set(reconciliation);
          this.loading.set(false);
        },
        error: (err: unknown) => {
          this.loading.set(false);
          this.toast.error(errorMessage(err), { title: 'No se pudo cargar el arqueo' });
        },
      });
    this.cash
      .movements(this.sessionId)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (movements) =>
          this.movements.set(
            (movements ?? []).map((movement) => ({
              ...movement,
              createdAt: formatDateTime(movement.createdAt),
            })),
          ),
        error: (err: unknown) =>
          this.toast.error(errorMessage(err), { title: 'No se pudieron cargar los movimientos' }),
      });
  }

  saveCounts(): void {
    const lines = (this.reconciliation()?.items ?? this.reconciliation()?.lines ?? []).filter(
      (line) => line.counted !== null && line.counted !== undefined,
    );
    if (!lines.length) {
      this.toast.warning('Ingresa al menos un conteo por medio de pago.');
      return;
    }
    const counts: CashCountLine[] = lines.map((line) => ({
      method: line.method,
      countedAmount: String(line.counted ?? '0.00'),
    }));
    this.cash
      .saveCounts(this.sessionId, { counts })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (reconciliation) => {
          this.reconciliation.set(reconciliation);
          this.toast.success('Conteos guardados.', { title: 'Arqueo actualizado' });
        },
        error: (err: unknown) =>
          this.toast.error(errorMessage(err), { title: 'No se pudieron guardar los conteos' }),
      });
  }

  setCounted(method: string, value: string): void {
    const current = this.reconciliation();
    if (!current) {
      return;
    }
    const items = (current.items ?? current.lines ?? []).map((line) =>
      line.method === method ? { ...line, counted: value } : line,
    );
    this.reconciliation.set({ ...current, items, lines: items });
  }

  addMovement(): void {
    const amount = Number(this.movementAmount());
    if (Number.isNaN(amount) || amount <= 0) {
      this.toast.warning('Ingresa un monto válido.');
      return;
    }
    if (this.movementReason().trim().length < 3) {
      this.toast.warning('Describe el motivo del movimiento.');
      return;
    }
    this.movementSubmitting.set(true);
    this.cash
      .addMovement(this.sessionId, {
        movementType: this.movementType(),
        amount: amount.toFixed(2),
        reason: this.movementReason().trim(),
      })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.movementSubmitting.set(false);
          this.movementAmount.set('0.00');
          this.movementReason.set('');
          this.toast.success('Movimiento registrado.');
          this.loadDetail();
        },
        error: (err: unknown) => {
          this.movementSubmitting.set(false);
          this.toast.error(errorMessage(err), { title: 'No se pudo registrar el movimiento' });
        },
      });
  }

  openCloseModal(): void {
    this.closeModal.set(true);
  }

  closeSession(): void {
    const amount = Number(this.countedAmount());
    if (Number.isNaN(amount) || amount < 0) {
      this.toast.warning('Ingresa el efectivo contado.');
      return;
    }
    this.closeSubmitting.set(true);
    this.cash
      .closeSession(this.sessionId, {
        countedAmount: amount.toFixed(2),
        notes: this.closeNotes().trim() || null,
      })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (closed) => {
          this.closeSubmitting.set(false);
          this.closeModal.set(false);
          this.session.set(closed);
          this.toast.success('Caja cerrada correctamente.', { title: 'Sesión cerrada' });
          this.loadDetail();
        },
        error: (err: unknown) => {
          this.closeSubmitting.set(false);
          this.toast.error(errorMessage(err), { title: 'No se pudo cerrar la caja' });
        },
      });
  }

  printClosing(): void {
    const session = this.session();
    const reconciliation = this.reconciliation();
    if (!session) {
      return;
    }
    const rows = (reconciliation?.items ?? reconciliation?.lines ?? [])
      .map(
        (line) => `
        <tr>
          <td>${this.methodLabel(line.method)}</td>
          <td class="num">${formatNumber(line.expected)}</td>
          <td class="num">${formatNumber(line.counted ?? null)}</td>
          <td class="num">${formatNumber(line.difference ?? null)}</td>
        </tr>`,
      )
      .join('');
    printHtml(
      `Cierre de caja ${session.registerName ?? ''}`,
      `<h1 class="brand">MEGALUJOS</h1>
      <h2>Cierre de caja — ${session.registerName ?? 'Caja'}</h2>
      <p class="muted">Apertura: ${session.openedAt} · Cierre: ${session.closedAt ?? '—'}</p>
      <table>
        <thead><tr><th>Medio</th><th class="num">Esperado</th><th class="num">Contado</th><th class="num">Diferencia</th></tr></thead>
        <tbody>${rows}</tbody>
      </table>
      <p class="total">Total contado: ${formatUsd(session.countedAmount ?? null)}</p>`,
    );
  }

  methodLabel(method: string): string {
    return METHOD_LABELS[method] ?? method;
  }

  methodOptionsList(): Array<{ value: string; label: string }> {
    return Object.entries(METHOD_LABELS).map(([value, label]) => ({ value, label }));
  }

  isManualMovement(type: string): boolean {
    return type === 'INCOME' || type === 'EXPENSE' || type === 'REFUND';
  }

  differenceTone(value: string | null | undefined): 'default' | 'success' | 'danger' {
    const numeric = value === null || value === undefined || value === '' ? 0 : Number(value);
    return numeric === 0 ? 'default' : 'danger';
  }

  movementLabel(type: string): string {
    switch (type) {
      case 'INCOME':
        return 'Ingreso';
      case 'EXPENSE':
        return 'Egreso';
      case 'REFUND':
        return 'Devolución';
      case 'SALE_PAYMENT':
        return 'Cobro de venta';
      case 'ADVANCE_PAYMENT':
        return 'Anticipo';
      default:
        return type;
    }
  }

  money(value: string | number | null | undefined): string {
    return formatUsd(value ?? null);
  }

  num(value: string | number | null | undefined): string {
    return formatNumber(value);
  }

  get isOpen(): boolean {
    return (this.session()?.status ?? '').toUpperCase() === 'OPEN';
  }
}
