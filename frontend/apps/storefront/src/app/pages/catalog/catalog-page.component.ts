import { Component, OnDestroy, inject, signal } from '@angular/core';
import { ActivatedRoute, Params, Router } from '@angular/router';
import { PublicCatalogService } from '@megalujos/api-client';
import { BrandDto, CategoryDto, PageResponse, ProductPublicDto } from '@megalujos/models';
import {
  MegaEmptyStateComponent,
  MegaPaginationComponent,
  MegaSkeletonComponent,
} from '@megalujos/ui';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { Subject, debounceTime, distinctUntilChanged, map, takeUntil } from 'rxjs';
import { ProductCardWrapperComponent } from '../../shared/product-card/product-card-wrapper.component';

interface CatalogFilters {
  q: string;
  category: string;
  brand: string;
  sort: string;
  minPrice: string;
  maxPrice: string;
  page: number;
}

const EMPTY_FILTERS: CatalogFilters = {
  q: '',
  category: '',
  brand: '',
  sort: '',
  minPrice: '',
  maxPrice: '',
  page: 1,
};

const SORT_OPTIONS = [
  { value: '', label: 'Relevancia' },
  { value: 'price,asc', label: 'Precio: menor a mayor' },
  { value: 'price,desc', label: 'Precio: mayor a menor' },
  { value: 'name,asc', label: 'Nombre: A–Z' },
  { value: 'name,desc', label: 'Nombre: Z–A' },
];

const PAGE_SIZE = 12;

@Component({
  selector: 'app-catalog-page',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    MegaPaginationComponent,
    MegaSkeletonComponent,
    MegaEmptyStateComponent,
    ProductCardWrapperComponent,
  ],
  styleUrl: './catalog-page.component.scss',
  templateUrl: './catalog-page.component.html',
})
export class CatalogPageComponent implements OnDestroy {
  private readonly catalog = inject(PublicCatalogService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly destroy$ = new Subject<void>();

  readonly pageSize = PAGE_SIZE;
  readonly sortOptions = SORT_OPTIONS;

  readonly loading = signal(true);
  readonly error = signal(false);
  readonly products = signal<ProductPublicDto[]>([]);
  readonly total = signal(0);
  readonly page = signal(1);
  readonly categories = signal<CategoryDto[]>([]);
  readonly brands = signal<BrandDto[]>([]);
  readonly filters = signal<CatalogFilters>(EMPTY_FILTERS);

  readonly searchControl = new FormControl('', { nonNullable: true });

  constructor() {
    this.loadFacets();

    this.searchControl.valueChanges
      .pipe(debounceTime(300), distinctUntilChanged(), takeUntil(this.destroy$))
      .subscribe((value) => {
        this.patchQuery({ q: value.trim() || null, page: null });
      });

    this.route.queryParams
      .pipe(
        map((params) => this.toFilters(params)),
        distinctUntilChanged((a, b) => JSON.stringify(a) === JSON.stringify(b)),
        takeUntil(this.destroy$),
      )
      .subscribe((filters) => {
        this.filters.set(filters);
        this.page.set(filters.page);
        this.searchControl.setValue(filters.q, { emitEvent: false });
        this.loadProducts(filters);
      });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  retry(): void {
    this.loadProducts(this.filters());
  }

  selectCategory(id: string | null): void {
    const current = this.filters().category;
    this.patchQuery({ category: id && id !== current ? id : null, page: null });
  }

  selectBrand(value: string): void {
    this.patchQuery({ brand: value || null, page: null });
  }

  selectSort(value: string): void {
    this.patchQuery({ sort: value || null, page: null });
  }

  applyPrices(minPrice: string, maxPrice: string): void {
    this.patchQuery({
      minPrice: minPrice.trim() || null,
      maxPrice: maxPrice.trim() || null,
      page: null,
    });
  }

  goToPage(page: number): void {
    this.patchQuery({ page: page <= 1 ? null : page });
  }

  clearFilters(): void {
    void this.router.navigate([], { relativeTo: this.route });
  }

  private loadProducts(filters: CatalogFilters): void {
    this.loading.set(true);
    this.error.set(false);
    this.catalog
      .products({
        q: filters.q || undefined,
        categoryId: filters.category || undefined,
        brandId: filters.brand || undefined,
        minPrice: filters.minPrice || undefined,
        maxPrice: filters.maxPrice || undefined,
        sort: filters.sort || undefined,
        page: filters.page - 1,
        size: this.pageSize,
      })
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (result) => this.applyPage(result),
        error: () => {
          this.products.set([]);
          this.total.set(0);
          this.error.set(true);
          this.loading.set(false);
        },
      });
  }

  private loadFacets(): void {
    this.catalog
      .categories()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (categories) => this.categories.set(categories),
        error: () => this.categories.set([]),
      });
    this.catalog
      .brands()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (brands) => this.brands.set(brands),
        error: () => this.brands.set([]),
      });
  }

  private patchQuery(params: Params): void {
    void this.router.navigate([], {
      relativeTo: this.route,
      queryParams: params,
      queryParamsHandling: 'merge',
    });
  }

  private toFilters(params: Params): CatalogFilters {
    const page = Number(params['page']);
    return {
      q: String(params['q'] ?? ''),
      category: String(params['category'] ?? ''),
      brand: String(params['brand'] ?? ''),
      sort: String(params['sort'] ?? ''),
      minPrice: String(params['minPrice'] ?? ''),
      maxPrice: String(params['maxPrice'] ?? ''),
      page: Number.isFinite(page) && page >= 1 ? Math.floor(page) : 1,
    };
  }

  private applyPage(result: PageResponse<ProductPublicDto> | null): void {
    if (!result) {
      this.products.set([]);
      this.total.set(0);
      this.error.set(true);
      this.loading.set(false);
      return;
    }
    this.products.set(result.items ?? []);
    this.total.set(result.totalElements ?? 0);
    this.error.set(false);
    this.loading.set(false);
  }
}
