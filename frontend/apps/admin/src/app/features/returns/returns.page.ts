import { ChangeDetectionStrategy, Component, DestroyRef, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { PosService } from '@megalujos/api-client';
import {
  PAYMENT_METHOD_CODES,
  PAYMENT_METHOD_LABELS,
  PaymentMethodCode,
  SaleDetailDto,
} from '@megalujos/models';
import {
  MegaButtonComponent,
  MegaEmptyStateComponent,
  MegaInputComponent,
  MegaModalComponent,
  MegaSelectComponent,
  MegaStatusBadgeComponent,
  MegaTextareaComponent,
  MegaToastService,
  formatUsd,
} from '@megalujos/ui';
import { errorMessage, formatDateTime, newIdempotencyKey } from '../../core/util/ui';

/** Selección de una línea a devolver. */
interface ReturnLine {
  saleItemId: string;
  description: string;
  quantity: string;
  maxQuantity: number;
  restock: boolean;
}

const REFUND_METHODS = PAYMENT_METHOD_CODES.filter((code) => code !== 'APPLIED_ADVANCE').map(
  (code) => ({ value: code, label: PAYMENT_METHOD_LABELS[code] }),
);

/** Devoluciones: busca una venta y registra el retorno de dinero y stock. */
@Component({
  selector: 'app-returns-page',
  imports: [
    FormsModule,
    MegaInputComponent,
    MegaSelectComponent,
    MegaTextareaComponent,
    MegaButtonComponent,
    MegaEmptyStateComponent,
    MegaStatusBadgeComponent,
    MegaModalComponent,
  ],
  templateUrl: './returns.page.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ReturnsPage {
  private readonly pos = inject(PosService);
  private readonly toast = inject(MegaToastService);
  private readonly destroyRef = inject(DestroyRef);

  readonly search = signal('');
  readonly searching = signal(false);
  readonly sale = signal<SaleDetailDto | null>(null);
  readonly lines = signal<ReturnLine[]>([]);
  readonly reason = signal('');
  readonly paymentMethod = signal<string>('CASH');
  readonly submitting = signal(false);
  readonly result = signal<string | null>(null);

  readonly methodOptions = REFUND_METHODS;

  searchSale(): void {
    const number = this.search().trim();
    if (!number) {
      this.toast.warning('Ingresa el número de venta.');
      return;
    }
    this.searching.set(true);
    this.sale.set(null);
    this.lines.set([]);
    this.pos
      .listSales({ q: number, size: 10 })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (page) => {
          const match = (page.items ?? [])[0];
          if (!match) {
            this.searching.set(false);
            this.toast.info('No se encontró ninguna venta con ese número.');
            return;
          }
          this.loadDetail(match.saleId ?? match.id);
        },
        error: (err: unknown) => {
          this.searching.set(false);
          this.toast.error(errorMessage(err), { title: 'No se pudo buscar la venta' });
        },
      });
  }

  private loadDetail(saleId: string): void {
    this.pos
      .getSale(saleId)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (detail) => {
          this.sale.set(detail);
          this.searching.set(false);
          this.lines.set(this.buildLines(detail));
          this.reason.set('');
        },
        error: (err: unknown) => {
          this.searching.set(false);
          this.toast.error(errorMessage(err), { title: 'No se pudo abrir la venta' });
        },
      });
  }

  private buildLines(detail: SaleDetailDto): ReturnLine[] {
    const returned = new Map<string, number>();
    for (const saleReturn of detail.returns ?? []) {
      for (const item of saleReturn.items ?? []) {
        returned.set(item.saleItemId, (returned.get(item.saleItemId) ?? 0) + Number(item.quantity));
      }
    }
    return (detail.items ?? [])
      .map((item) => {
        const already = returned.get(item.id) ?? 0;
        const max = Number(item.quantity) - already;
        return {
          saleItemId: item.id,
          description: item.description,
          quantity: max > 0 ? '1' : '0',
          maxQuantity: max,
          restock: true,
        };
      })
      .filter((line) => line.maxQuantity > 0);
  }

  setQuantity(saleItemId: string, value: string): void {
    this.lines.update((lines) =>
      lines.map((line) =>
        line.saleItemId === saleItemId
          ? { ...line, quantity: String(Math.max(0, Number(value) || 0)) }
          : line,
      ),
    );
  }

  setRestock(saleItemId: string, value: boolean): void {
    this.lines.update((lines) =>
      lines.map((line) => (line.saleItemId === saleItemId ? { ...line, restock: value } : line)),
    );
  }

  selectedLines(): ReturnLine[] {
    return this.lines().filter((line) => Number(line.quantity) > 0);
  }

  estimatedTotal(): number {
    const sale = this.sale();
    if (!sale) {
      return 0;
    }
    return this.selectedLines().reduce((acc, line) => {
      const item = (sale.items ?? []).find((detail) => detail.id === line.saleItemId);
      if (!item) {
        return acc;
      }
      const unit = (Number(item.unitPrice) - Number(item.discount) / Number(item.quantity)) as number;
      const taxFactor = Number(item.quantity) > 0 ? Number(item.taxAmount) / Number(item.quantity) : 0;
      return acc + Number(line.quantity) * (unit + taxFactor);
    }, 0);
  }

  submit(): void {
    const sale = this.sale();
    if (!sale) {
      return;
    }
    const items = this.selectedLines();
    if (!items.length) {
      this.toast.warning('Selecciona al menos una cantidad a devolver.');
      return;
    }
    if (this.reason().trim().length < 5) {
      this.toast.warning('Describe el motivo de la devolución.');
      return;
    }

    this.submitting.set(true);
    this.pos
      .createReturn(
        sale.saleId,
        {
          reason: this.reason().trim(),
          paymentMethod: this.paymentMethod(),
          items: items.map((line) => ({
            saleItemId: line.saleItemId,
            quantity: line.quantity,
            restock: line.restock,
          })),
        },
        newIdempotencyKey(),
      )
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (saleReturn) => {
          this.submitting.set(false);
          this.result.set(saleReturn.returnNumber);
          this.toast.success(`Devolución ${saleReturn.returnNumber} registrada`, {
            title: 'Devolución confirmada',
          });
          this.loadDetail(sale.saleId);
        },
        error: (err: unknown) => {
          this.submitting.set(false);
          this.toast.error(errorMessage(err), { title: 'No se pudo registrar la devolución' });
        },
      });
  }

  reset(): void {
    this.sale.set(null);
    this.lines.set([]);
    this.search.set('');
    this.result.set(null);
  }

  money(value: string | number | null | undefined): string {
    return formatUsd(value ?? null);
  }

  dateTime(value: string | null | undefined): string {
    return formatDateTime(value);
  }

  get detail(): SaleDetailDto | null {
    return this.sale();
  }
}
