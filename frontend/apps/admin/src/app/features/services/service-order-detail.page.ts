import { ChangeDetectionStrategy, Component, DestroyRef, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { CashService, MediaService, ServicesService } from '@megalujos/api-client';
import {
  PAYMENT_METHOD_CODES,
  PAYMENT_METHOD_LABELS,
  PaymentMethodCode,
  ServiceOrderDto,
} from '@megalujos/models';
import {
  MegaButtonComponent,
  MegaInputComponent,
  MegaModalComponent,
  MegaSelectComponent,
  MegaStatusBadgeComponent,
  MegaTextareaComponent,
  MegaToastService,
  MegaUploaderComponent,
  formatUsd,
} from '@megalujos/ui';
import {
  errorMessage,
  formatDateTime,
  formatNumber,
  newIdempotencyKey,
} from '../../core/util/ui';
import { SERVICE_ORDER_STATUS_LABELS, SERVICE_ORDER_STATUS_TONES } from './service-orders.page';

const TRANSITIONS: Record<string, string[]> = {
  QUOTATION: ['CONFIRMED', 'CANCELLED'],
  CONFIRMED: ['IN_PROGRESS', 'CANCELLED'],
  IN_PROGRESS: ['READY_FOR_DELIVERY', 'CANCELLED'],
  READY_FOR_DELIVERY: ['DELIVERED', 'CANCELLED'],
  DELIVERED: [],
  CANCELLED: [],
};

const PAY_OPTIONS = PAYMENT_METHOD_CODES.filter((code) => code !== 'APPLIED_ADVANCE').map(
  (code) => ({ value: code, label: PAYMENT_METHOD_LABELS[code] }),
);

/** Detalle de orden de servicio: estados, anticipos, cobro, materiales y adjuntos. */
@Component({
  selector: 'app-service-order-detail-page',
  imports: [
    FormsModule,
    RouterLink,
    MegaInputComponent,
    MegaSelectComponent,
    MegaTextareaComponent,
    MegaModalComponent,
    MegaStatusBadgeComponent,
    MegaUploaderComponent,
    MegaButtonComponent,
  ],
  templateUrl: './service-order-detail.page.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ServiceOrderDetailPage {
  private readonly services = inject(ServicesService);
  private readonly cash = inject(CashService);
  private readonly media = inject(MediaService);
  private readonly toast = inject(MegaToastService);
  private readonly route = inject(ActivatedRoute);
  private readonly destroyRef = inject(DestroyRef);

  readonly orderId = this.route.snapshot.paramMap.get('id') ?? '';
  readonly order = signal<ServiceOrderDto | null>(null);
  readonly loading = signal(true);
  readonly cashSessionId = signal<string | null>(null);

  readonly statusLabels = SERVICE_ORDER_STATUS_LABELS;
  readonly statusTones = SERVICE_ORDER_STATUS_TONES;

  readonly statusModal = signal(false);
  readonly nextStatus = signal('');
  readonly statusNotes = signal('');
  readonly statusSubmitting = signal(false);

  readonly advanceModal = signal(false);
  readonly advanceAmount = signal('0.00');
  readonly advanceMethod = signal('CASH');
  readonly advanceReference = signal('');
  readonly advanceSubmitting = signal(false);

  readonly chargeModal = signal(false);
  readonly chargeAmount = signal('0.00');
  readonly chargeMethod = signal('CASH');
  readonly chargeReference = signal('');
  readonly chargeSubmitting = signal(false);

  readonly materialQty = signal<Record<string, string>>({});
  readonly materialNotes = signal('');
  readonly materialBusy = signal(false);

  readonly uploading = signal(false);

  readonly payOptions = PAY_OPTIONS;

  constructor() {
    this.load();
    this.cash
      .activeSession()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (session) => this.cashSessionId.set(session && session.id ? session.id : null),
        error: () => this.cashSessionId.set(null),
      });
  }

  load(): void {
    this.loading.set(true);
    this.services
      .get(this.orderId)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (order) => {
          this.order.set(order);
          this.loading.set(false);
          if (!this.chargeAmount() || this.chargeAmount() === '0.00') {
            this.chargeAmount.set(String(order.remainingBalance ?? order.estimatedTotal ?? '0.00'));
          }
        },
        error: (err: unknown) => {
          this.loading.set(false);
          this.toast.error(errorMessage(err), { title: 'No se pudo cargar la orden' });
        },
      });
  }

  allowedTransitions(): string[] {
    const status = (this.order()?.status ?? '').toUpperCase();
    return TRANSITIONS[status] ?? [];
  }

  transitionLabel(code: string): string {
    return this.statusLabels[code] ?? code;
  }

  openStatusModal(): void {
    const options = this.allowedTransitions();
    this.nextStatus.set(options[0] ?? '');
    this.statusNotes.set('');
    this.statusModal.set(true);
  }

  submitStatus(): void {
    if (!this.nextStatus()) {
      this.toast.warning('Selecciona el nuevo estado.');
      return;
    }
    this.statusSubmitting.set(true);
    this.services
      .updateStatus(this.orderId, this.nextStatus(), this.statusNotes().trim() || null)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (order) => {
          this.statusSubmitting.set(false);
          this.statusModal.set(false);
          this.order.set(order);
          this.toast.success(`Estado actualizado a ${this.transitionLabel(order.status)}.`);
        },
        error: (err: unknown) => {
          this.statusSubmitting.set(false);
          this.toast.error(errorMessage(err), { title: 'No se pudo cambiar el estado' });
        },
      });
  }

  openAdvanceModal(): void {
    this.advanceAmount.set('0.00');
    this.advanceReference.set('');
    this.advanceModal.set(true);
  }

  submitAdvance(): void {
    const amount = Number(this.advanceAmount());
    if (Number.isNaN(amount) || amount <= 0) {
      this.toast.warning('Ingresa un monto válido.');
      return;
    }
    this.advanceSubmitting.set(true);
    this.services
      .addAdvance(this.orderId, {
        amount: amount.toFixed(2),
        cashSessionId: this.cashSessionId() ?? undefined,
        paymentMethod: this.advanceMethod() as PaymentMethodCode,
        reference: this.advanceReference().trim() || null,
      })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.advanceSubmitting.set(false);
          this.advanceModal.set(false);
          this.toast.success('Anticipo registrado.');
          this.load();
        },
        error: (err: unknown) => {
          this.advanceSubmitting.set(false);
          this.toast.error(errorMessage(err), { title: 'No se pudo registrar el anticipo' });
        },
      });
  }

  openChargeModal(): void {
    if (!this.cashSessionId()) {
      this.toast.warning('Abre una sesión de caja en la sección Caja para poder cobrar.');
      return;
    }
    this.chargeModal.set(true);
  }

  submitCharge(): void {
    const sessionId = this.cashSessionId();
    const amount = Number(this.chargeAmount());
    if (!sessionId) {
      this.toast.warning('No hay sesión de caja abierta.');
      return;
    }
    if (Number.isNaN(amount) || amount <= 0) {
      this.toast.warning('Ingresa un monto válido.');
      return;
    }
    this.chargeSubmitting.set(true);
    this.services
      .charge(
        this.orderId,
        {
          cashSessionId: sessionId,
          payments: [
            {
              method: this.chargeMethod(),
              amount: amount.toFixed(2),
              reference: this.chargeReference().trim() || null,
            },
          ],
        },
        newIdempotencyKey(),
      )
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (sale) => {
          this.chargeSubmitting.set(false);
          this.chargeModal.set(false);
          this.toast.success(`Venta ${sale.saleNumber} registrada.`, { title: 'Cobro exitoso' });
          this.load();
        },
        error: (err: unknown) => {
          this.chargeSubmitting.set(false);
          this.toast.error(errorMessage(err), { title: 'No se pudo cobrar' });
        },
      });
  }

  setMaterialQty(materialId: string, value: string): void {
    this.materialQty.update((map) => ({ ...map, [materialId]: value }));
  }

  consume(materialId: string): void {
    this.actOnMaterial(materialId, 'consume');
  }

  returnMaterial(materialId: string): void {
    this.actOnMaterial(materialId, 'return');
  }

  private actOnMaterial(materialId: string, action: 'consume' | 'return'): void {
    const quantity = Number(this.materialQty()[materialId] ?? '0');
    if (Number.isNaN(quantity) || quantity <= 0) {
      this.toast.warning('Ingresa una cantidad válida.');
      return;
    }
    this.materialBusy.set(true);
    const operation$ =
      action === 'consume'
        ? this.services.consumeMaterial(this.orderId, materialId, quantity, this.materialNotes() || null)
        : this.services.returnMaterial(this.orderId, materialId, quantity, this.materialNotes() || null);
    operation$.pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: (order) => {
        this.materialBusy.set(false);
        this.order.set(order);
        this.toast.success(action === 'consume' ? 'Material consumido.' : 'Material devuelto.');
      },
      error: (err: unknown) => {
        this.materialBusy.set(false);
        this.toast.error(errorMessage(err), { title: 'No se pudo registrar el movimiento' });
      },
    });
  }

  onFilesSelected(files: File[]): void {
    const file = files[0];
    if (!file) {
      return;
    }
    this.uploading.set(true);
    this.media
      .upload(file, true)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (media) =>
          this.services
            .addAttachment(this.orderId, media.id, 'IMAGE')
            .pipe(takeUntilDestroyed(this.destroyRef))
            .subscribe({
              next: () => {
                this.uploading.set(false);
                this.toast.success('Archivo adjuntado.');
                this.load();
              },
              error: (err: unknown) => {
                this.uploading.set(false);
                this.toast.error(errorMessage(err), { title: 'No se pudo adjuntar' });
              },
            }),
        error: (err: unknown) => {
          this.uploading.set(false);
          this.toast.error(errorMessage(err), { title: 'No se pudo subir el archivo' });
        },
      });
  }

  removeAttachment(mediaId: string): void {
    this.services
      .removeAttachment(this.orderId, mediaId)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.toast.success('Adjunto eliminado.');
          this.load();
        },
        error: (err: unknown) =>
          this.toast.error(errorMessage(err), { title: 'No se pudo eliminar el adjunto' }),
      });
  }

  num(value: string | number | null | undefined): string {
    return formatNumber(value);
  }

  money(value: string | number | null | undefined): string {
    return formatUsd(value ?? null);
  }

  dateTime(value: string | null | undefined): string {
    return formatDateTime(value);
  }

  typeLabel(code: string): string {
    switch (code) {
      case 'STICKER':
        return 'Sticker';
      case 'INSTALLATION':
        return 'Instalación';
      default:
        return code;
    }
  }
}
