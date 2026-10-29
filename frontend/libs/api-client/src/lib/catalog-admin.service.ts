import { Injectable } from '@angular/core';
import {
  BrandDto,
  CategoryDto,
  PageResponse,
  ProductAdminDto,
  ProductImageDto,
  VehicleBrand,
  VehicleCompatibilityDto,
  VehicleModel,
  VehicleVersion,
  VariantLookupDto,
} from '@megalujos/models';
import { Observable, map } from 'rxjs';
import { ApiBaseService, ListParams } from './api-base';

/** Opciones de listado de productos (admin). */
export interface ProductListParams extends ListParams {
  q?: string;
  categoryId?: string;
  brandId?: string;
  published?: boolean;
  active?: boolean;
  productType?: string;
}

/** Solicitud de creación/actualización de producto (admin). */
export interface ProductAdminRequest {
  sku: string;
  name: string;
  categoryId?: string | null;
  brandId?: string | null;
  productType?: string;
  unit?: string;
  trackStock?: boolean;
  published?: boolean;
  description?: string | null;
  variants?: Array<{
    id?: string;
    sku: string;
    barcode?: string | null;
    attributes?: Record<string, string | number | boolean | null>;
    salePrice: string;
    costPrice?: string | null;
    minStock?: string | null;
  }>;
}

/** Solicitud de creación/actualización de categoría. */
export interface CategoryRequest {
  name: string;
  slug?: string;
  parentId?: string | null;
  description?: string | null;
  sortOrder?: number;
}

/** Solicitud de creación/actualización de marca. */
export interface BrandRequest {
  name: string;
  active?: boolean;
}

function normalizeProduct(raw: ProductAdminDto): ProductAdminDto {
  const category =
    raw.category ??
    (raw.categoryId ? { id: raw.categoryId, name: raw.categoryName ?? '' } : { id: '', name: '' });
  const brand =
    raw.brand ?? (raw.brandId ? { id: raw.brandId, name: raw.brandName ?? '' } : undefined);
  return { ...raw, category, brand };
}

/**
 * Catálogo de administración: categorías (árbol), marcas, productos, imágenes,
 * vehículos y lookup de variantes por código de barras.
 */
@Injectable({ providedIn: 'root' })
export class CatalogAdminService extends ApiBaseService {
  // ------------------------------------------------------------- categorías
  /** Árbol de categorías (paginado por raíces; incluye inactivas). */
  categoryTree(params?: ListParams): Observable<PageResponse<CategoryDto>> {
    return this.getPaged<CategoryDto>('/categories', params).pipe(
      map((page) => ({ ...page, items: (page.items ?? []).map((item) => this.normalizeCategory(item)) })),
    );
  }

  /** Listado de categorías (mismo endpoint; alias de `categoryTree`). */
  listCategories(params?: ListParams): Observable<PageResponse<CategoryDto>> {
    return this.categoryTree(params);
  }

  /** Crea una categoría. */
  createCategory(body: CategoryRequest): Observable<CategoryDto> {
    return this.httpPost<CategoryDto>('/categories', body);
  }

  /** Actualiza una categoría. */
  updateCategory(id: string, body: CategoryRequest): Observable<CategoryDto> {
    return this.httpPut<CategoryDto>(`/categories/${id}`, body);
  }

  /** Activa/desactiva una categoría. */
  setCategoryActive(id: string, active: boolean): Observable<CategoryDto> {
    return this.httpPatch<CategoryDto>(`/categories/${id}/active`, undefined, { active });
  }

  // ----------------------------------------------------------------- marcas
  /** Marcas de catálogo (incluye inactivas). */
  listBrands(params?: ListParams & { q?: string }): Observable<BrandDto[]> {
    return this.httpGet<BrandDto[]>('/brands', params);
  }

  /** Crea una marca. */
  createBrand(body: BrandRequest): Observable<BrandDto> {
    return this.httpPost<BrandDto>('/brands', body);
  }

  /** Actualiza una marca. */
  updateBrand(id: string, body: BrandRequest): Observable<BrandDto> {
    return this.httpPut<BrandDto>(`/brands/${id}`, body);
  }

  /** Activa/desactiva una marca. */
  setBrandActive(id: string, active: boolean): Observable<BrandDto> {
    return this.httpPatch<BrandDto>(`/brands/${id}/active`, undefined, { active });
  }

  // -------------------------------------------------------------- productos
  /** Productos (paginado) con variantes e imágenes. */
  listProducts(params?: ProductListParams): Observable<PageResponse<ProductAdminDto>> {
    return this.getPaged<ProductAdminDto>('/products', params).pipe(
      map((page) => ({ ...page, items: page.items.map(normalizeProduct) })),
    );
  }

  /** Detalle de producto. */
  getProduct(id: string): Observable<ProductAdminDto> {
    return this.httpGet<ProductAdminDto>(`/products/${id}`).pipe(map(normalizeProduct));
  }

  /** Crea un producto con sus variantes. */
  createProduct(body: ProductAdminRequest): Observable<ProductAdminDto> {
    return this.httpPost<ProductAdminDto>('/products', body).pipe(map(normalizeProduct));
  }

  /** Actualiza cabecera y variantes de un producto. */
  updateProduct(id: string, body: ProductAdminRequest): Observable<ProductAdminDto> {
    return this.httpPut<ProductAdminDto>(`/products/${id}`, body).pipe(map(normalizeProduct));
  }

  /** Publica/despublica un producto de forma explícita. */
  publish(id: string, published: boolean): Observable<ProductAdminDto> {
    return this.httpPatch<ProductAdminDto>(`/products/${id}/publish`, undefined, { published }).pipe(
      map(normalizeProduct),
    );
  }

  /** Alias de `publish`. */
  setPublished(id: string, published: boolean): Observable<ProductAdminDto> {
    return this.publish(id, published);
  }

  // ---------------------------------------------------------------- imágenes
  /** Asocia un media ya subido a un producto. */
  addImage(
    productId: string,
    mediaId: string,
    options?: { sortOrder?: number; isPrimary?: boolean },
  ): Observable<ProductImageDto> {
    return this.httpPost<ProductImageDto>(`/products/${productId}/images`, undefined, {
      params: this.params({
        mediaId,
        sortOrder: options?.sortOrder ?? 0,
        isPrimary: options?.isPrimary ?? false,
      }),
    });
  }

  /** Elimina la asociación de una imagen. */
  removeImage(productId: string, imageId: string): Observable<void> {
    return this.httpDelete<void>(`/products/${productId}/images/${imageId}`);
  }

  /** Marca una imagen como principal. */
  setPrimaryImage(productId: string, imageId: string): Observable<ProductImageDto> {
    return this.httpPut<ProductImageDto>(`/products/${productId}/images/${imageId}/primary`, {});
  }

  // -------------------------------------------------------------- vehículos
  /** Marcas de vehículo. */
  listVehicleBrands(): Observable<VehicleBrand[]> {
    return this.httpGet<VehicleBrand[]>('/vehicle-brands');
  }

  /** Crea una marca de vehículo. */
  createVehicleBrand(body: { name: string }): Observable<VehicleBrand> {
    return this.httpPost<VehicleBrand>('/vehicle-brands', body);
  }

  /** Actualiza una marca de vehículo. */
  updateVehicleBrand(id: string, body: { name: string }): Observable<VehicleBrand> {
    return this.httpPut<VehicleBrand>(`/vehicle-brands/${id}`, body);
  }

  /** Modelos de vehículo (opcionalmente por marca). */
  listVehicleModels(brandId?: string): Observable<VehicleModel[]> {
    return this.httpGet<VehicleModel[]>('/vehicle-models', { brandId });
  }

  /** Crea un modelo de vehículo. */
  createVehicleModel(body: { brandId: string; name: string }): Observable<VehicleModel> {
    return this.httpPost<VehicleModel>('/vehicle-models', body);
  }

  /** Actualiza un modelo de vehículo. */
  updateVehicleModel(id: string, body: { brandId?: string; name: string }): Observable<VehicleModel> {
    return this.httpPut<VehicleModel>(`/vehicle-models/${id}`, body);
  }

  /** Versiones de vehículo (opcionalmente por modelo). */
  listVehicleVersions(modelId?: string): Observable<VehicleVersion[]> {
    return this.httpGet<VehicleVersion[]>('/vehicle-versions', { modelId });
  }

  /** Crea una versión de vehículo. */
  createVehicleVersion(body: {
    modelId: string;
    name: string;
    yearFrom?: number | null;
    yearTo?: number | null;
  }): Observable<VehicleVersion> {
    return this.httpPost<VehicleVersion>('/vehicle-versions', body);
  }

  /** Actualiza una versión de vehículo. */
  updateVehicleVersion(
    id: string,
    body: { modelId?: string; name: string; yearFrom?: number | null; yearTo?: number | null },
  ): Observable<VehicleVersion> {
    return this.httpPut<VehicleVersion>(`/vehicle-versions/${id}`, body);
  }

  // --------------------------------------------------------------- variantes
  /** Lookup de variante por código de barras (POS/escáner). */
  variantLookup(barcode: string): Observable<VariantLookupDto> {
    return this.httpGet<VariantLookupDto>('/variants/lookup', { barcode });
  }

  /** Alias de `variantLookup`. */
  lookupVariantByBarcode(barcode: string): Observable<VariantLookupDto> {
    return this.variantLookup(barcode);
  }

  /** Reemplaza la compatibilidad con vehículos de una variante. */
  replaceVariantCompatibility(
    variantId: string,
    items: Array<{ vehicleVersionId: string; verified?: boolean; notes?: string | null }>,
  ): Observable<VehicleCompatibilityDto[]> {
    return this.httpPut<VehicleCompatibilityDto[]>(`/variants/${variantId}/compatibility`, { items });
  }

  private normalizeCategory(category: CategoryDto): CategoryDto {
    if (!category.children?.length) {
      return category;
    }
    return {
      ...category,
      children: category.children.map((child) => this.normalizeCategory(child)),
    };
  }
}
