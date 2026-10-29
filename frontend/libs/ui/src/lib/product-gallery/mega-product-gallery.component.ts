import { Component, computed, input, output, signal } from '@angular/core';

/** Imagen de la galería. */
export interface MegaGalleryImage {
  /** URL de la imagen. */
  url: string;
  /** Texto alternativo. */
  alt?: string;
}

/**
 * Galería de imágenes de producto: imagen principal, miniaturas,
 * navegación anterior/siguiente y teclado (←/→). UI pura.
 */
@Component({
  selector: 'mega-product-gallery',
  standalone: true,
  styleUrl: './mega-product-gallery.component.scss',
  template: `
    <div class="mega-gallery" (keydown.left)="previous()" (keydown.right)="next()" tabindex="0">
      <div class="mega-gallery__stage">
        @if (currentImage(); as image) {
          <img [src]="image.url" [attr.alt]="image.alt ?? name()" />
        } @else {
          <span class="mega-gallery__placeholder" aria-hidden="true">🖼️</span>
        }

        @if (images().length > 1) {
          <button
            type="button"
            class="mega-gallery__nav mega-gallery__nav--prev"
            aria-label="Imagen anterior"
            (click)="previous()"
          >
            ‹
          </button>
          <button
            type="button"
            class="mega-gallery__nav mega-gallery__nav--next"
            aria-label="Imagen siguiente"
            (click)="next()"
          >
            ›
          </button>
        }
      </div>

      @if (images().length > 1) {
        <ul class="mega-gallery__thumbs">
          @for (image of images(); track image.url; let i = $index) {
            <li>
              <button
                type="button"
                class="mega-gallery__thumb"
                [class.mega-gallery__thumb--active]="i === index()"
                [attr.aria-label]="'Ver imagen ' + (i + 1)"
                [attr.aria-current]="i === index() ? 'true' : null"
                (click)="select(i)"
              >
                <img [src]="image.url" [attr.alt]="image.alt ?? ''" loading="lazy" />
              </button>
            </li>
          }
        </ul>
      }
    </div>
  `,
})
export class MegaProductGalleryComponent {
  /** Lista de imágenes (vacío = placeholder). */
  images = input<MegaGalleryImage[]>([]);

  /** Nombre del producto (usado como alt de fallback). */
  name = input('Producto');

  /** Emite al cambiar de imagen. */
  indexChange = output<number>();

  /** Índice activo. */
  readonly index = signal(0);

  /** Imagen activa. */
  readonly currentImage = computed(() => this.images()[this.index()] ?? null);

  /** Selecciona una imagen por índice. */
  select(index: number): void {
    const total = this.images().length;
    if (total === 0) {
      if (this.index() !== 0) {
        this.index.set(0);
        this.indexChange.emit(0);
      }
      return;
    }
    const normalized = ((index % total) + total) % total;
    if (normalized !== this.index()) {
      this.index.set(normalized);
      this.indexChange.emit(normalized);
    }
  }

  previous(): void {
    this.select(this.index() - 1);
  }

  next(): void {
    this.select(this.index() + 1);
  }
}
