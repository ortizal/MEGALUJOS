import { ChangeDetectionStrategy, Component, DestroyRef, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { CatalogAdminService, MediaService } from '@megalujos/api-client';
import { BrandDto, CategoryDto, ProductImageDto } from '@megalujos/models';
import {
  MegaButtonComponent,
  MegaInputComponent,
  MegaSelectComponent,
  MegaTextareaComponent,
  MegaToastService,
  MegaUploaderComponent,
} from '@megalujos/ui';
import { errorMessage } from '../../core/util/ui';

/** Fila editable de variante en el formulario. */
interface EditableVariant {
  id: string | null;
  sku: string;
  barcode: string;
  salePrice: string;
  costPrice: string;
  minStock: string;
}

const PRODUCT_TYPE_OPTIONS = [
  { value: 'PHYSICAL', label: 'Producto físico' },
  { value: 'SERVICE', label: 'Servicio' },
];

function emptyVariant(): EditableVariant {
  return { id: null, sku: '', barcode: '', salePrice: '0.00', costPrice: '0.00', minStock: '0' };
}

/** Formulario de creación/edición de producto con variantes e imágenes. */
@Component({
  selector: 'app-product-form-page',
  imports: [
    FormsModule,
    RouterLink,
    MegaInputComponent,
    MegaTextareaComponent,
    MegaSelectComponent,
    MegaUploaderComponent,
    MegaButtonComponent,
  ],
  templateUrl: './product-form.page.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ProductFormPage {
  private readonly catalog = inject(CatalogAdminService);
  private readonly media = inject(MediaService);
  private readonly toast = inject(MegaToastService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);

  readonly productId = this.route.snapshot.paramMap.get('id');
  readonly isEdit = !!this.productId;

  readonly loading = signal(this.isEdit);
  readonly saving = signal(false);
  readonly uploading = signal(false);

  readonly sku = signal('');
  readonly name = signal('');
  readonly description = signal('');
  readonly categoryId = signal('');
  readonly brandId = signal('');
  readonly productType = signal('PHYSICAL');
  readonly unit = signal('UND');
  readonly trackStock = signal(true);
  readonly published = signal(true);

  readonly variants = signal<EditableVariant[]>([emptyVariant()]);
  readonly images = signal<ProductImageDto[]>([]);

  readonly categories = signal<CategoryDto[]>([]);
  readonly brands = signal<BrandDto[]>([]);

  readonly typeOptions = PRODUCT_TYPE_OPTIONS;

  constructor() {
    this.loadCatalog();
    if (this.productId) {
      this.loadProduct();
    }
  }

  private loadCatalog(): void {
    this.catalog
      .listCategories({ size: 200 })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (page) => this.categories.set(page.items ?? []),
        error: () => this.categories.set([]),
      });
    this.catalog
      .listBrands()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (brands) => this.brands.set(brands ?? []),
        error: () => this.brands.set([]),
      });
  }

  private loadProduct(): void {
    this.catalog
      .getProduct(this.productId as string)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (product) => {
          this.loading.set(false);
          this.sku.set(product.sku);
          this.name.set(product.name);
          this.description.set(product.description ?? '');
          this.categoryId.set(product.categoryId ?? product.category?.id ?? '');
          this.brandId.set(product.brandId ?? product.brand?.id ?? '');
          this.productType.set(product.productType || 'PHYSICAL');
          this.unit.set(product.unit ?? 'UND');
          this.trackStock.set(product.trackStock ?? true);
          this.published.set(product.published ?? true);
          this.variants.set(
            (product.variants ?? []).map((variant) => ({
              id: variant.id ?? null,
              sku: variant.sku,
              barcode: variant.barcode ?? '',
              salePrice: String(variant.salePrice ?? '0.00'),
              costPrice: String(variant.costPrice ?? '0.00'),
              minStock: String(variant.minStock ?? '0'),
            })),
          );
          if (!this.variants().length) {
            this.variants.set([emptyVariant()]);
          }
          this.images.set(product.images ?? []);
        },
        error: (err: unknown) => {
          this.loading.set(false);
          this.toast.error(errorMessage(err), { title: 'No se pudo cargar el producto' });
        },
      });
  }

  categoryOptions(): Array<{ value: string; label: string }> {
    return [
      { value: '', label: 'Sin categoría' },
      ...this.categories().map((category) => ({ value: category.id, label: category.name })),
    ];
  }

  brandOptions(): Array<{ value: string; label: string }> {
    return [
      { value: '', label: 'Sin marca' },
      ...this.brands().map((brand) => ({ value: brand.id, label: brand.name })),
    ];
  }

  addVariant(): void {
    this.variants.update((variants) => [...variants, emptyVariant()]);
  }

  removeVariant(index: number): void {
    this.variants.update((variants) =>
      variants.length > 1 ? variants.filter((_item, position) => position !== index) : variants,
    );
  }

  updateVariant(index: number, patch: Partial<EditableVariant>): void {
    this.variants.update((variants) =>
      variants.map((variant, position) => (position === index ? { ...variant, ...patch } : variant)),
    );
  }

  save(): void {
    const sku = this.sku().trim();
    const name = this.name().trim();
    if (!sku || !name) {
      this.toast.warning('SKU y nombre son obligatorios.');
      return;
    }
    const variants = this.variants()
      .map((variant) => ({
        id: variant.id ?? undefined,
        sku: variant.sku.trim(),
        barcode: variant.barcode.trim() || null,
        salePrice: variant.salePrice,
        costPrice: variant.costPrice || null,
        minStock: variant.minStock || null,
      }))
      .filter((variant) => variant.sku);

    if (!variants.length) {
      this.toast.warning('Agrega al menos una variante con SKU.');
      return;
    }

    const request = {
      sku,
      name,
      description: this.description().trim() || null,
      categoryId: this.categoryId() || null,
      brandId: this.brandId() || null,
      productType: this.productType(),
      unit: this.unit().trim() || 'UND',
      trackStock: this.productType() === 'SERVICE' ? false : this.trackStock(),
      published: this.published(),
      variants,
    };

    this.saving.set(true);
    const operation$ = this.isEdit
      ? this.catalog.updateProduct(this.productId as string, request)
      : this.catalog.createProduct(request);
    operation$.pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: () => {
        this.saving.set(false);
        this.toast.success(`Producto ${this.isEdit ? 'actualizado' : 'creado'}.`);
        void this.router.navigateByUrl('/catalogo/productos');
      },
      error: (err: unknown) => {
        this.saving.set(false);
        this.toast.error(errorMessage(err), { title: 'No se pudo guardar el producto' });
      },
    });
  }

  onFilesSelected(files: File[]): void {
    const file = files[0];
    if (!file || !this.productId) {
      return;
    }
    this.uploading.set(true);
    this.media
      .upload(file, true)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (media) => {
          this.catalog
            .addImage(this.productId as string, media.id, {
              sortOrder: this.images().length,
              isPrimary: this.images().length === 0,
            })
            .pipe(takeUntilDestroyed(this.destroyRef))
            .subscribe({
              next: (image) => {
                this.uploading.set(false);
                this.images.update((images) => [...images, image]);
                this.toast.success('Imagen agregada.');
              },
              error: (err: unknown) => {
                this.uploading.set(false);
                this.toast.error(errorMessage(err), { title: 'No se pudo asociar la imagen' });
              },
            });
        },
        error: (err: unknown) => {
          this.uploading.set(false);
          this.toast.error(errorMessage(err), { title: 'No se pudo subir la imagen' });
        },
      });
  }

  setPrimary(image: ProductImageDto): void {
    this.catalog
      .setPrimaryImage(this.productId as string, image.id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (updated) =>
          this.images.update((images) =>
            images.map((item) => ({ ...item, primary: item.id === updated.id })),
          ),
        error: (err: unknown) =>
          this.toast.error(errorMessage(err), { title: 'No se pudo fijar la imagen principal' }),
      });
  }

  removeImage(image: ProductImageDto): void {
    this.catalog
      .removeImage(this.productId as string, image.id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.images.update((images) => images.filter((item) => item.id !== image.id));
          this.toast.success('Imagen eliminada.');
        },
        error: (err: unknown) =>
          this.toast.error(errorMessage(err), { title: 'No se pudo eliminar la imagen' }),
      });
  }
}
