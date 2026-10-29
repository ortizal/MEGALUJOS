import { Injectable } from '@angular/core';
import {
  BrandDto,
  CatalogInquiryRequest,
  CategoryDto,
  PageResponse,
  ProductPublicDto,
} from '@megalujos/models';
import { Observable } from 'rxjs';
import { ApiBaseService, ListParams } from './api-base';

/** Parámetros de búsqueda del catálogo público. */
export interface PublicProductParams extends ListParams {
  /** Texto libre de búsqueda (el backend lo recibe como `search`). */
  q?: string;
  categoryId?: string;
  brandId?: string;
  minPrice?: string;
  maxPrice?: string;
  sort?: string;
}

/** Catálogo público de la vitrina (solo productos publicados). */
@Injectable({ providedIn: 'root' })
export class PublicCatalogService extends ApiBaseService {
  /** Productos publicados con precios públicos y disponibilidad. */
  products(params?: PublicProductParams): Observable<PageResponse<ProductPublicDto>> {
    const { q, ...rest } = params ?? {};
    return this.getPaged<ProductPublicDto>('/public/catalog/products', {
      ...rest,
      search: q,
    });
  }

  /** Detalle público de un producto. */
  product(id: string): Observable<ProductPublicDto> {
    return this.httpGet<ProductPublicDto>(`/public/catalog/products/${id}`);
  }

  /** Categorías activas. */
  categories(): Observable<CategoryDto[]> {
    return this.httpGet<CategoryDto[]>('/public/catalog/categories');
  }

  /** Marcas activas. */
  brands(): Observable<BrandDto[]> {
    return this.httpGet<BrandDto[]>('/public/catalog/brands');
  }

  /** Registra una consulta WhatsApp (de un producto o de la vitrina). */
  inquiry(variantId?: string | null, customerPhone?: string | null): Observable<void> {
    const request: CatalogInquiryRequest = { variantId, customerPhone };
    return this.httpPost<void>('/public/catalog/inquiries', request);
  }
}
