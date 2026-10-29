import { Injectable } from '@angular/core';
import {
  BannerDto,
  BannerRequest,
  PromotionDto,
  PromotionRequest,
  ShowcaseDto,
  ShowcaseRequest,
} from '@megalujos/models';
import { Observable, switchMap } from 'rxjs';
import { ApiBaseService } from './api-base';
import { withBannerUrls } from './util/banner-url';

/** Administración de banners, showcases y promociones de la vitrina. */
@Injectable({ providedIn: 'root' })
export class ShowcaseAdminService extends ApiBaseService {
  // ------------------------------------------------------------------ banners
  /** Lista los banners con `url` resuelta desde `mediaId`. */
  listBanners(): Observable<BannerDto[]> {
    return this.httpGet<BannerDto[]>('/banners').pipe(
      switchMap((banners) => withBannerUrls(this.http, (path) => this.url(path), banners)),
    );
  }

  createBanner(request: BannerRequest): Observable<BannerDto> {
    return this.httpPost<BannerDto>('/banners', request);
  }

  updateBanner(id: string, request: BannerRequest): Observable<BannerDto> {
    return this.httpPut<BannerDto>(`/banners/${id}`, request);
  }

  setBannerActive(id: string, active: boolean): Observable<BannerDto> {
    return this.httpPatch<BannerDto>(`/banners/${id}/active`, undefined, { active });
  }

  // ---------------------------------------------------------------- showcases
  listShowcases(): Observable<ShowcaseDto[]> {
    return this.httpGet<ShowcaseDto[]>('/showcases');
  }

  /** Crea o actualiza un showcase por su `code`. */
  updateShowcase(request: ShowcaseRequest): Observable<ShowcaseDto> {
    return this.httpPut<ShowcaseDto>('/showcases', request);
  }

  /** Alias de `updateShowcase`. */
  upsertShowcase(request: ShowcaseRequest): Observable<ShowcaseDto> {
    return this.updateShowcase(request);
  }

  /** Reemplaza los productos (variantes) de un showcase. */
  setProducts(code: string, variantIds: string[]): Observable<ShowcaseDto> {
    return this.httpPut<ShowcaseDto>(`/showcases/${code}/products`, { variantIds });
  }

  /** Alias de `setProducts`. */
  replaceShowcaseProducts(code: string, variantIds: string[]): Observable<void> {
    return this.httpPut<void>(`/showcases/${code}/products`, { variantIds });
  }

  // -------------------------------------------------------------- promociones
  promotions(): Observable<PromotionDto[]> {
    return this.httpGet<PromotionDto[]>('/promotions');
  }

  createPromotion(request: PromotionRequest): Observable<PromotionDto> {
    return this.httpPost<PromotionDto>('/promotions', request);
  }

  updatePromotion(id: string, request: PromotionRequest): Observable<PromotionDto> {
    return this.httpPut<PromotionDto>(`/promotions/${id}`, request);
  }
}
