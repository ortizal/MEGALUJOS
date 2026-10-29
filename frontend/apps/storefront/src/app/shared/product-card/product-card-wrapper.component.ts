import { Component, computed, inject, input } from '@angular/core';
import { Router } from '@angular/router';
import { ProductPublicDto } from '@megalujos/models';
import { MegaProductCardAvailability, MegaProductCardComponent } from '@megalujos/ui';
import { environment } from '../../../environments/environment';
import { productWhatsappMessage, waLink } from '../whatsapp.util';

const AVAILABILITY_MAP: Record<string, MegaProductCardAvailability> = {
  AVAILABLE: 'in_stock',
  LOW_STOCK: 'low_stock',
  OUT_OF_STOCK: 'out_of_stock',
  ON_REQUEST: 'unknown',
};

/** Traduce el código de disponibilidad público al vocabulario de MegaProductCard. */
export function mapCardAvailability(code: string | null | undefined): MegaProductCardAvailability {
  return AVAILABILITY_MAP[String(code ?? '').toUpperCase()] ?? 'unknown';
}

/** Imagen principal de un producto público (o la primera disponible). */
export function primaryImageUrl(product: ProductPublicDto): string | null {
  const images = product.images ?? [];
  const primary = images.find((image) => image.primary) ?? images[0];
  return primary?.url ?? null;
}

/**
 * Adaptador de `MegaProductCard` para la vitrina: navega al detalle
 * y abre WhatsApp con mensaje prellenado para el producto.
 */
@Component({
  selector: 'app-product-card',
  standalone: true,
  imports: [MegaProductCardComponent],
  template: `
    <mega-product-card
      [name]="product().name"
      [sku]="product().sku"
      [imageUrl]="imageUrl()"
      [price]="product().price"
      [availability]="availability()"
      [actionText]="actionText()"
      [clickable]="true"
      (view)="openDetail()"
      (action)="openWhatsapp()"
    />
  `,
})
export class ProductCardWrapperComponent {
  /** Producto público de la vitrina. */
  product = input.required<ProductPublicDto>();

  /** Texto del botón de acción (por defecto "Consultar"). */
  actionText = input('Consultar');

  private readonly router = inject(Router);

  readonly imageUrl = computed(() => primaryImageUrl(this.product()));
  readonly availability = computed(() => mapCardAvailability(this.product().availability));

  openDetail(): void {
    void this.router.navigate(['/producto', this.product().id]);
  }

  openWhatsapp(): void {
    const product = this.product();
    const url =
      product.whatsappUrl || waLink(productWhatsappMessage(product), environment.whatsappNumber);
    window.open(url, '_blank', 'noopener');
  }
}
