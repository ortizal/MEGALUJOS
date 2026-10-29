import { Injectable } from '@angular/core';
import {
  BannerDto,
  ProductPublicDto,
  PublicPromotionDto,
  PromotionDto,
  ShowcaseHomeDto,
  TvShowcaseDto,
} from '@megalujos/models';
import { Observable, map, switchMap } from 'rxjs';
import { ApiBaseService } from './api-base';
import { withBannerUrls } from './util/banner-url';

function withTvPromotions(tv: TvShowcaseDto): TvShowcaseDto {
  const promos = tv.promos ?? [];
  const promotions: PromotionDto[] = promos.map((promo) => ({ ...promo, active: true }));
  return { ...tv, promos, promotions };
}

function withFeatured(home: ShowcaseHomeDto): ShowcaseHomeDto {
  const featured: ProductPublicDto[] = home.featured ?? home.featuredProducts ?? [];
  return { ...home, featured, featuredProducts: featured };
}

/** Vitrina pública: home, modo TV y promociones vigentes. */
@Injectable({ providedIn: 'root' })
export class PublicShowcaseService extends ApiBaseService {
  /** Contenido del home: banners (con URL resuelta), destacados y velocidad de autoplay. */
  home(): Observable<ShowcaseHomeDto> {
    return this.httpGet<ShowcaseHomeDto>('/public/showcases/home').pipe(
      switchMap((home) =>
        withBannerUrls(this.http, (path) => this.url(path), home.banners ?? []).pipe(
          map((banners) => withFeatured({ ...home, banners })),
        ),
      ),
    );
  }

  /** Alias de `home`. */
  getHome(): Observable<ShowcaseHomeDto> {
    return this.home();
  }

  /** Modo TV: loop de productos publicados y promociones vigentes. */
  tv(): Observable<TvShowcaseDto> {
    return this.httpGet<TvShowcaseDto>('/public/showcases/tv').pipe(map(withTvPromotions));
  }

  /** Alias de `tv`. */
  getTv(): Observable<TvShowcaseDto> {
    return this.tv();
  }

  /** Promociones vigentes (público). */
  promotions(): Observable<PublicPromotionDto[]> {
    return this.httpGet<PublicPromotionDto[]>('/public/promotions');
  }

  /** Alias de `promotions`. */
  getPromotions(): Observable<PublicPromotionDto[]> {
    return this.promotions();
  }
}
