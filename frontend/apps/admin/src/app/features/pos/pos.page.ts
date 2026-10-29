import { ChangeDetectionStrategy, Component, DestroyRef, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { debounceTime, distinctUntilChanged, Subject } from 'rxjs';
import {
  CatalogAdminService,
  CashService,
  InvoicingService,
  PosService,
} from '@megalujos/api-client';
import {
  CategoryDto,
  ConfirmSaleRequest,
  PAYMENT_METHOD_CODES,
  PAYMENT_METHOD_LABELS,
  PaymentMethodCode,
  ProductAdminDto,
  SaleResponseDto,
  VariantDto,
} from '@megalujos/models';
import {
  MegaButtonComponent,
  MegaCurrencyInputComponent,
  MegaEmptyStateComponent,
  MegaInputComponent,
  MegaModalComponent,
  MegaProductCardComponent,
  MegaSelectComponent,
  MegaSkeletonComponent,
  MegaToastService,
  formatUsd,
} from '@megalujos/ui';
import { errorMessage, newIdempotencyKey, printHtml } from '../../core/util/ui';

/** Línea del carrito POS (el servidor recalcula precio e impuestos). */
interface CartLine {
  variantId: string;
  sku: string;
  name: string;
  unitPrice: string;
  quantity: number;
  discount: string;
}

/** Medios de pago admitidos en el POS (sin anticipos aplicados). */
const POS_METHODS: PaymentMethodCode[] = ['CASH', 'TRANSFER', 'CARD', 'OTHER'];

/**
 * Punto de venta: catálogo con búsqueda por texto/código de barras,
 * carrito con descuento por línea, cobro idempotente y ticket imprimible.
 */
@Component({
  selector: 'app-pos-page',
  imports: [
    FormsModule,
    RouterLink,
    MegaInputComponent,
    MegaSelectComponent,
    MegaCurrencyInputComponent,
    MegaProductCardComponent,
    MegaSkeletonComponent,
    MegaEmptyStateComponent,
    MegaModalComponent,
    MegaButtonComponent,
  ],
  templateUrl: './pos.page.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PosPage {
  private readonly catalog = inject(CatalogAdminService);
  private readonly cash = inject(CashService);
  private readonly pos = inject(PosService);
  private readonly invoicing = inject(InvoicingService);
  private readonly toast = inject(MegaToastService);

  private readonly destroyRef = inject(DestroyRef);
  private readonly searchSubject = new Subject<string>();

  readonly query = signal('');
  readonly barcode = signal('');
  readonly categoryId = signal('');
  readonly products = signal<ProductAdminDto[]>([]);
  readonly productsLoading = signal(true);
  readonly categories = signal<CategoryDto[]>([]);

  readonly cart = signal<CartLine[]>([]);
  readonly paymentMethod = signal<string>('CASH');
  readonly paymentAmount = signal('');
  readonly paymentReference = signal('');
  readonly requestInvoice = signal(false);

  readonly session = signal<{ id: string; registerName?: string | null } | null>(null);
  readonly sessionChecked = signal(false);
  readonly taxRate = signal<number | null>(null);

  readonly charging = signal(false);
  readonly result = signal<SaleResponseDto | null>(null);
  readonly variantPicker = signal<{ name: string; variants: VariantDto[] } | null>(null);

  readonly methodOptions = POS_METHODS.map((code) => ({
    value: code,
    label: PAYMENT_METHOD_LABELS[code],
  }));

  readonly categoryOptions = signal<Array<{ value: string; label: string }>>([
    { value: '', label: 'Todas las categorías' },
  ]);

  constructor() {
    this.searchSubject
      .pipe(debounceTime(250), distinctUntilChanged(), takeUntilDestroyed(this.destroyRef))
      .subscribe((value) => {
        this.query.set(value);
        this.loadProducts();
      });

    this.loadCategories();
    this.loadProducts();
    this.loadSession();
    this.loadTaxRate();
  }

  onSearch(value: string): void {
    this.searchSubject.next(value);
  }

  onCategoryChange(value: string): void {
    this.categoryId.set(value);
    this.loadProducts();
  }

  onBarcode(value: string): void {
    const code = value.trim();
    if (!code) {
      return;
    }
    this.catalog.variantLookup(code).subscribe({
      next: (lookup) => {
        this.barcode.set('');
        this.addVariant(lookup.variantId, lookup.sku, lookup.product.name, String(lookup.salePrice));
        this.toast.success(`${lookup.product.name} agregado al carrito`);
      },
      error: () => {
        this.barcode.set('');
        this.query.set(code);
        this.loadProducts();
        this.toast.info('Código no encontrado; se buscó como texto.');
      },
    });
  }

  loadProducts(): void {
    this.productsLoading.set(true);
    this.catalog
      .listProducts({
        q: this.query() || undefined,
        categoryId: this.categoryId() || undefined,
        size: 24,
      })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (page) => {
          this.products.set(page.items ?? []);
          this.productsLoading.set(false);
        },
        error: (error: unknown) => {
          this.products.set([]);
          this.productsLoading.set(false);
          this.toast.error(errorMessage(error), { title: 'No se pudo cargar el catálogo' });
        },
      });
  }

  primaryUrl(product: ProductAdminDto): string | null {
    const images = product.images ?? [];
    const primary = images.find((image) => image.primary) ?? images[0];
    return primary?.url ?? null;
  }

  firstPrice(product: ProductAdminDto): string | null {
    const variant = (product.variants ?? []).find((item) => item.id) ?? product.variants?.[0];
    return variant ? String(variant.salePrice) : null;
  }

  addToCart(product: ProductAdminDto): void {
    const variants = (product.variants ?? []).filter((variant) => variant.id);
    if (variants.length === 0) {
      this.toast.warning('El producto no tiene variantes registradas.');
      return;
    }
    if (variants.length === 1) {
      const only = variants[0];
      this.addVariant(String(only.id), only.sku, product.name, String(only.salePrice));
      return;
    }
    this.variantPicker.set({ name: product.name, variants });
  }

  pickVariant(variant: VariantDto): void {
    const picker = this.variantPicker();
    if (!picker || !variant.id) {
      return;
    }
    this.addVariant(String(variant.id), variant.sku, picker.name, String(variant.salePrice));
    this.variantPicker.set(null);
  }

  addVariant(variantId: string, sku: string, name: string, unitPrice: string): void {
    this.cart.update((lines) => {
      const existing = lines.find((line) => line.variantId === variantId);
      if (existing) {
        return lines.map((line) =>
          line.variantId === variantId ? { ...line, quantity: line.quantity + 1 } : line,
        );
      }
      return [...lines, { variantId, sku, name, unitPrice, quantity: 1, discount: '0' }];
    });
    if (!this.paymentAmount()) {
      this.paymentAmount.set(this.estimatedTotal().toFixed(2));
    }
  }

  setQuantity(variantId: string, quantity: number | string): void {
    const parsed = Number(quantity);
    const value = Number.isFinite(parsed) ? Math.max(1, Math.floor(parsed)) : 1;
    this.cart.update((lines) =>
      lines.map((line) => (line.variantId === variantId ? { ...line, quantity: value } : line)),
    );
    this.syncPaymentAmount();
  }

  setDiscount(variantId: string, discount: string): void {
    this.cart.update((lines) =>
      lines.map((line) => (line.variantId === variantId ? { ...line, discount: discount || '0' } : line)),
    );
    this.syncPaymentAmount();
  }

  removeLine(variantId: string): void {
    this.cart.update((lines) => lines.filter((line) => line.variantId !== variantId));
    this.syncPaymentAmount();
  }

  clearCart(): void {
    this.cart.set([]);
    this.paymentAmount.set('');
    this.paymentReference.set('');
  }

  lineTotal(line: CartLine): number {
    const base = Number(line.unitPrice) * line.quantity - Number(line.discount || 0);
    return Number.isNaN(base) ? 0 : base;
  }

  subtotal(): number {
    return this.cart().reduce((acc, line) => acc + Number(line.unitPrice) * line.quantity, 0);
  }

  discountTotal(): number {
    return this.cart().reduce((acc, line) => acc + Number(line.discount || 0), 0);
  }

  netTotal(): number {
    return this.subtotal() - this.discountTotal();
  }

  estimatedTax(): number {
    const rate = this.taxRate();
    if (rate === null) {
      return 0;
    }
    return this.cart().reduce((acc, line) => acc + Math.round(this.lineTotal(line) * rate * 100) / 100, 0);
  }

  estimatedTotal(): number {
    return this.netTotal() + this.estimatedTax();
  }

  money(value: string | number | null | undefined): string {
    return formatUsd(value);
  }

  cobrar(): void {
    const session = this.session();
    if (!session) {
      this.toast.warning('No hay una sesión de caja abierta.');
      return;
    }
    const lines = this.cart();
    if (!lines.length) {
      this.toast.warning('Agrega al menos un producto al carrito.');
      return;
    }
    const amount = Number(this.paymentAmount() || this.estimatedTotal());
    if (!Number.isFinite(amount) || amount <= 0) {
      this.toast.warning('Ingresa el monto a cobrar.');
      return;
    }

    const request: ConfirmSaleRequest = {
      cashSessionId: session.id,
      items: lines.map((line) => ({
        variantId: line.variantId,
        quantity: String(line.quantity),
        discountAmount: line.discount || '0',
      })),
      payments: [
        {
          method: this.paymentMethod(),
          amount: amount.toFixed(2),
          reference: this.paymentReference() || null,
        },
      ],
      requestInvoice: this.requestInvoice(),
    };

    this.charging.set(true);
    this.pos.confirmSale(request, newIdempotencyKey()).subscribe({
      next: (sale) => {
        this.charging.set(false);
        this.result.set(sale);
        this.cart.set([]);
        this.paymentAmount.set('');
        this.paymentReference.set('');
        this.requestInvoice.set(false);
        this.loadProducts();
        this.toast.success(`Venta ${sale.saleNumber} registrada`, { title: 'Venta confirmada' });
      },
      error: (error: unknown) => {
        this.charging.set(false);
        this.toast.error(errorMessage(error), { title: 'No se pudo confirmar la venta' });
      },
    });
  }

  printTicket(): void {
    const sale = this.result();
    if (!sale) {
      return;
    }
    const rows = sale.items
      .map(
        (item) => `<tr>
          <td>${item.description}</td>
          <td class="num">${item.quantity}</td>
          <td class="num">${formatUsd(item.unitPrice)}</td>
          <td class="num">${formatUsd(item.taxAmount)}</td>
          <td class="num">${formatUsd(item.lineTotal)}</td>
        </tr>`,
      )
      .join('');
    const payments = sale.payments
      .map(
        (payment) =>
          `<tr><td>${PAYMENT_METHOD_LABELS[payment.method as PaymentMethodCode] ?? payment.method}</td><td class="num">${formatUsd(payment.amount)}</td></tr>`,
      )
      .join('');
    printHtml(
      `Ticket ${sale.saleNumber}`,
      `<h1 class="brand">MEGALUJOS</h1>
      <p class="muted">Venta ${sale.saleNumber} · ${sale.status}</p>
      <h2>Detalle</h2>
      <table>
        <thead><tr><th>Producto</th><th class="num">Cant.</th><th class="num">P. unit.</th><th class="num">IVA</th><th class="num">Total</th></tr></thead>
        <tbody>${rows}</tbody>
      </table>
      <h2>Pagos</h2>
      <table><tbody>${payments}</tbody></table>
      <p class="total">Total: ${formatUsd(sale.grandTotal)}</p>
      <p class="muted">Impuestos: ${formatUsd(sale.taxTotal)} · Subtotal: ${formatUsd(sale.subtotal)}</p>`,
    );
  }

  private loadCategories(): void {
    this.catalog
      .categoryTree({ size: 200 })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (page) => {
          const flat: Array<{ value: string; label: string }> = [
            { value: '', label: 'Todas las categorías' },
          ];
          const walk = (items: CategoryDto[], depth: number): void => {
            for (const item of items) {
              flat.push({ value: item.id, label: `${'— '.repeat(depth)}${item.name}` });
              if (item.children?.length) {
                walk(item.children, depth + 1);
              }
            }
          };
          walk(page.items ?? [], 0);
          this.categoryOptions.set(flat);
        },
        error: () => this.categoryOptions.set([{ value: '', label: 'Todas las categorías' }]),
      });
  }

  private loadSession(): void {
    this.cash
      .activeSession()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (session) => {
          this.session.set((session as unknown as { id: string } | null) ?? null);
          this.sessionChecked.set(true);
        },
        error: () => {
          this.session.set(null);
          this.sessionChecked.set(true);
        },
      });
  }

  private loadTaxRate(): void {
    this.invoicing
      .config()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (config) => {
          const rule = (config.taxRules ?? []).find((item) => item.defaultRule) ?? (config.taxRules ?? [])[0];
          this.taxRate.set(rule ? Number(rule.rate) : null);
        },
        error: () => this.taxRate.set(null),
      });
  }

  private syncPaymentAmount(): void {
    if (!this.paymentAmount()) {
      return;
    }
    this.paymentAmount.set(this.estimatedTotal().toFixed(2));
  }
}
