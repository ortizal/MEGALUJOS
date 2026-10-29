import { Component, booleanAttribute, input, output } from '@angular/core';
import { MegaPriceComponent } from '../price/mega-price.component';

/** Estado de disponibilidad mostrado como badge en la tarjeta. */
export type MegaProductCardAvailability = 'in_stock' | 'low_stock' | 'out_of_stock' | 'unknown';

const AVAILABILITY_LABEL: Record<MegaProductCardAvailability, string> = {
  in_stock: 'Disponible',
  low_stock: 'Últimas unidades',
  out_of_stock: 'Agotado',
  unknown: 'Consultar',
};

/**
 * Tarjeta de producto para la vitrina: imagen, nombre, precio (USD),
 * disponibilidad y acción principal. UI pura: el contenedor decide la navegación.
 */
@Component({
  selector: 'mega-product-card',
  standalone: true,
  imports: [MegaPriceComponent],
  styleUrl: './mega-product-card.component.scss',
  template: `
    <article class="mega-product-card" [class.mega-product-card--clickable]="clickable()">
      @if (badge()) {
        <span class="mega-product-card__badge">{{ badge() }}</span>
      }

      <div class="mega-product-card__media" (click)="view.emit()">
        @if (imageUrl()) {
          <img [src]="imageUrl()" [attr.alt]="name()" loading="lazy" />
        } @else {
          <span class="mega-product-card__placeholder" aria-hidden="true">📦</span>
        }
      </div>

      <div class="mega-product-card__body">
        @if (sku()) {
          <span class="mega-product-card__sku">{{ sku() }}</span>
        }
        <h3 class="mega-product-card__name" (click)="view.emit()">{{ name() }}</h3>

        <div class="mega-product-card__meta">
          <mega-price [value]="price()" [compareAt]="compareAt()" size="md" />
          <span
            class="mega-product-card__availability"
            [attr.data-availability]="availability()"
          >
            {{ availabilityLabel() }}
          </span>
        </div>

        <button type="button" class="mega-product-card__action" (click)="action.emit()">
          {{ actionText() }}
        </button>
      </div>
    </article>
  `,
})
export class MegaProductCardComponent {
  /** Nombre del producto. */
  name = input('Producto');

  /** SKU visible (opcional). */
  sku = input<string | null>(null);

  /** URL de la imagen principal. */
  imageUrl = input<string | null>(null);

  /** Precio de venta (decimal string). */
  price = input<string | number | null>(null);

  /** Precio anterior tachado (promoción). */
  compareAt = input<string | number | null>(null);

  /** Badge flotante (ej. "¡Nuevo!", "-15%"). */
  badge = input<string | null>(null);

  /** Estado de disponibilidad. */
  availability = input<MegaProductCardAvailability>('unknown');

  /** Texto del botón de acción. */
  actionText = input('Ver detalle');

  /** true cuando el contenedor registró un listener de `view`/`action`. */
  clickable = input(false, { transform: booleanAttribute });

  /** Emite al pulsar la imagen o el nombre (ver detalle). */
  view = output<void>();

  /** Emite al pulsar la acción principal. */
  action = output<void>();

  /** Etiqueta legible de la disponibilidad. */
  availabilityLabel(): string {
    return AVAILABILITY_LABEL[this.availability()] ?? AVAILABILITY_LABEL.unknown;
  }
}
