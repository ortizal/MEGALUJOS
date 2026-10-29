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
import { CatalogAdminService } from '@megalujos/api-client';
import { BrandDto, CategoryDto, ProductAdminDto } from '@megalujos/models';
import {
  MegaButtonComponent,
  MegaConfirmService,
  MegaEmptyStateComponent,
  MegaInputComponent,
  MegaPaginationComponent,
  MegaSelectComponent,
  MegaStatusBadgeComponent,
  MegaTableColumn,
  MegaTableCellContext,
  MegaTableComponent,
  MegaToastService,
  formatUsd,
} from '@megalujos/ui';
import { errorMessage } from '../../core/util/ui';

function flattenCategories(categories: CategoryDto[], depth = 0): CategoryDto[] {
  return categories.flatMap((category) => [
    { ...category, name: `${'— '.repeat(depth)}${category.name}` },
    ...flattenCategories(category.children ?? [], depth + 1),
  ]);
}

/** Listado administrativo de productos con filtros y publicación. */
@Component({
  selector: 'app-products-admin-page',
  imports: [
    FormsModule,
    RouterLink,
    MegaInputComponent,
    MegaSelectComponent,
    MegaTableComponent,
    MegaPaginationComponent,
    MegaEmptyStateComponent,
    MegaStatusBadgeComponent,
    MegaButtonComponent,
  ],
  templateUrl: './products-admin.page.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ProductsAdminPage {
  private readonly catalog = inject(CatalogAdminService);
  private readonly toast = inject(MegaToastService);
  private readonly confirm = inject(MegaConfirmService);
  private readonly destroyRef = inject(DestroyRef);

  readonly query = signal('');
  readonly categoryId = signal('');
  readonly brandId = signal('');
  readonly published = signal('');
  readonly page = signal(1);
  readonly size = 20;
  readonly total = signal(0);
  readonly rows = signal<ProductAdminDto[]>([]);
  readonly loading = signal(true);
  readonly error = signal<string | null>(null);

  readonly categories = signal<CategoryDto[]>([]);
  readonly brands = signal<BrandDto[]>([]);

  readonly rowKey = (row: ProductAdminDto): string => row.id;

  readonly publishedCell = viewChild<
    TemplateRef<MegaTableCellContext<ProductAdminDto>>
  >('publishedCell');
  readonly actionsCell = viewChild<
    TemplateRef<MegaTableCellContext<ProductAdminDto>>
  >('actionsCell');

  readonly columns = computed<MegaTableColumn<ProductAdminDto>[]>(() => [
    { key: 'sku', header: 'SKU', width: '9rem' },
    { key: 'name', header: 'Producto' },
    { key: 'categoryName', header: 'Categoría', width: '12rem' },
    { key: 'brandName', header: 'Marca', width: '10rem' },
    {
      key: 'published',
      header: 'Visible',
      width: '9rem',
      cell: this.publishedCell() ?? undefined,
    },
    {
      header: 'Acciones',
      align: 'end',
      width: '18rem',
      cell: this.actionsCell() ?? undefined,
    },
  ]);

  constructor() {
    this.load();
    this.loadFilters();
  }

  load(): void {
    this.loading.set(true);
    this.error.set(null);
    this.catalog
      .listProducts({
        q: this.query() || undefined,
        categoryId: this.categoryId() || undefined,
        brandId: this.brandId() || undefined,
        published: this.published() === '' ? undefined : this.published() === 'true',
        page: this.page() - 1,
        size: this.size,
      })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (page) => {
          this.rows.set(page.items ?? []);
          this.total.set(page.totalElements ?? 0);
          this.loading.set(false);
        },
        error: (err: unknown) => {
          const message = errorMessage(err);
          this.error.set(message);
          this.loading.set(false);
          this.toast.error(message, { title: 'No se pudieron cargar los productos' });
        },
      });
  }

  private loadFilters(): void {
    this.catalog
      .listCategories({ size: 200 })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (page) => this.categories.set(flattenCategories(page.items ?? [])),
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

  categoryOptions(): Array<{ value: string; label: string }> {
    return [
      { value: '', label: 'Todas las categorías' },
      ...this.categories().map((category) => ({ value: category.id, label: category.name })),
    ];
  }

  brandOptions(): Array<{ value: string; label: string }> {
    return [
      { value: '', label: 'Todas las marcas' },
      ...this.brands().map((brand) => ({ value: brand.id, label: brand.name })),
    ];
  }

  get publishedOptions(): Array<{ value: string; label: string }> {
    return [
      { value: '', label: 'Publicados y no publicados' },
      { value: 'true', label: 'Solo publicados' },
      { value: 'false', label: 'Solo no publicados' },
    ];
  }

  applyFilters(): void {
    this.page.set(1);
    this.load();
  }

  goToPage(page: number): void {
    this.page.set(page);
    this.load();
  }

  async togglePublished(product: ProductAdminDto): Promise<void> {
    const next = !product.published;
    const ok = await this.confirm.open({
      title: next ? 'Publicar producto' : 'Despublicar producto',
      message: next
        ? `El producto "${product.name}" será visible en la vitrina.`
        : `El producto "${product.name}" dejará de mostrarse en la vitrina.`,
      confirmText: next ? 'Publicar' : 'Despublicar',
      danger: !next,
    });
    if (!ok) {
      return;
    }
    this.catalog
      .publish(product.id, next)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.toast.success(next ? 'Producto publicado.' : 'Producto despublicado.');
          this.load();
        },
        error: (err: unknown) =>
          this.toast.error(errorMessage(err), { title: 'No se pudo cambiar la publicación' }),
      });
  }

  primaryPrice(product: ProductAdminDto): string {
    const variant = (product.variants ?? []).find((item) => item.salePrice) ?? product.variants?.[0];
    return variant ? formatUsd(variant.salePrice) : '—';
  }
}
