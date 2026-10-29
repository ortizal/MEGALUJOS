import { Component, OnDestroy, inject, signal } from '@angular/core';
import { Meta, Title } from '@angular/platform-browser';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { PublicCatalogService } from '@megalujos/api-client';
import { ProductPublicDto } from '@megalujos/models';
import {
  MegaPriceComponent,
  MegaProductGalleryComponent,
  MegaSkeletonComponent,
  MegaStockBadgeComponent,
} from '@megalujos/ui';
import { Subject, of, switchMap, takeUntil } from 'rxjs';
import { environment } from '../../../environments/environment';
import { productWhatsappMessage, waLink } from '../../shared/whatsapp.util';

@Component({
  selector: 'app-product-detail-page',
  standalone: true,
  imports: [
    RouterLink,
    MegaProductGalleryComponent,
    MegaPriceComponent,
    MegaStockBadgeComponent,
    MegaSkeletonComponent,
  ],
  styleUrl: './product-detail-page.component.scss',
  templateUrl: './product-detail-page.component.html',
})
export class ProductDetailPageComponent implements OnDestroy {
  private readonly catalog = inject(PublicCatalogService);
  private readonly route = inject(ActivatedRoute);
  private readonly title = inject(Title);
  private readonly meta = inject(Meta);
  private readonly destroy$ = new Subject<void>();

  readonly product = signal<ProductPublicDto | null>(null);
  readonly loading = signal(true);
  readonly error = signal(false);

  constructor() {
    this.title.setTitle('Producto — MEGALUJOS');

    this.route.paramMap
      .pipe(
        switchMap((params) => {
          const id = params.get('id');
          if (!id) {
            return of(null);
          }
          this.loading.set(true);
          this.error.set(false);
          return this.catalog.product(id);
        }),
        takeUntil(this.destroy$),
      )
      .subscribe({
        next: (product) => {
          this.product.set(product);
          this.loading.set(false);
          if (product) {
            this.updateMeta(product);
          } else {
            this.error.set(true);
          }
        },
        error: () => {
          this.product.set(null);
          this.error.set(true);
          this.loading.set(false);
        },
      });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  galleryImages(): { url: string; alt?: string }[] {
    const product = this.product();
    if (!product) {
      return [];
    }
    const images = [...(product.images ?? [])].sort((a, b) => {
      if (a.primary !== b.primary) {
        return a.primary ? -1 : 1;
      }
      return a.sortOrder - b.sortOrder;
    });
    return images.map((image) => ({ url: image.url, alt: image.alt ?? product.name }));
  }

  attributeEntries(product: ProductPublicDto): { key: string; value: string }[] {
    return Object.entries(product.attributes ?? {}).map(([key, value]) => ({
      key,
      value: String(value ?? '—'),
    }));
  }

  whatsappHref(): string {
    const product = this.product();
    if (!product) {
      return waLink(environment.whatsappDefaultMessage, environment.whatsappNumber);
    }
    return (
      product.whatsappUrl || waLink(productWhatsappMessage(product), environment.whatsappNumber)
    );
  }

  registerInquiry(): void {
    this.catalog.inquiry(null).subscribe({ error: () => undefined });
  }

  private updateMeta(product: ProductPublicDto): void {
    const title = `${product.name} — MEGALUJOS`;
    const description =
      (product.description ?? '').trim().slice(0, 155) ||
      `${product.name} (${product.sku}) — ${
        product.categoryName ?? 'accesorios automotrices'
      } en MEGALUJOS. Consulta disponibilidad y cotiza por WhatsApp.`;
    this.title.setTitle(title);
    this.meta.updateTag({ name: 'description', content: description });
    this.meta.updateTag({ property: 'og:title', content: title });
    this.meta.updateTag({ property: 'og:description', content: description });
    this.meta.updateTag({ property: 'og:type', content: 'product' });
  }
}
