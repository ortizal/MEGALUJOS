import { Decimal, IsoDateTime, Uuid } from './common.model';
import { ProductPublicDto } from './catalog.model';

/** Banner de vitrina. */
export interface BannerDto {
  id: Uuid;
  title?: string | null;
  subtitle?: string | null;
  /** URL de la imagen (contrato maestro; si el backend solo envía `mediaId`, la vitrina resuelve el alias). */
  url?: string | null;
  mediaId?: Uuid | null;
  imageUrl?: string | null;
  linkUrl?: string | null;
  startsAt?: IsoDateTime | null;
  endsAt?: IsoDateTime | null;
  sortOrder: number;
  active: boolean;
}

/** Solicitud de banner. */
export interface BannerRequest {
  title?: string | null;
  subtitle?: string | null;
  mediaId?: Uuid | null;
  linkUrl?: string | null;
  startsAt?: IsoDateTime | null;
  endsAt?: IsoDateTime | null;
  sortOrder?: number;
}

/** Carrusel/showcase configurado. */
export interface ShowcaseDto {
  id: Uuid;
  code: string;
  title?: string | null;
  description?: string | null;
  autoplaySeconds: number;
  active: boolean;
  productIds?: Uuid[];
}

/** Solicitud de showcase (upsert por code). */
export interface ShowcaseRequest {
  code: string;
  title?: string | null;
  description?: string | null;
  autoplaySeconds?: number;
  active?: boolean;
}

/** Contenido del home público. */
export interface ShowcaseHomeDto {
  banners: BannerDto[];
  showcaseTitle?: string | null;
  autoplaySeconds: number;
  featured: ProductPublicDto[];
  featuredProducts?: ProductPublicDto[];
}

/** Ítem del modo TV. */
export interface TvShowcaseItemDto {
  id: Uuid;
  name: string;
  sku?: string | null;
  price: Decimal;
  imageUrl?: string | null;
  availability?: string | null;
  category?: string | null;
}

/** Modo TV: loop continuo de productos publicados y promociones. */
export interface TvShowcaseDto {
  autoplaySeconds: number;
  items: TvShowcaseItemDto[];
  /** Promociones vigentes (campo `promos` del backend). */
  promos: PublicPromotionDto[];
  /** Alias del contrato maestro (`promotions`); lo normaliza PublicShowcaseService. */
  promotions: PromotionDto[];
}

/** Promoción (admin). */
export interface PromotionDto {
  id: Uuid;
  name: string;
  description?: string | null;
  promoType: string;
  value: Decimal;
  startsAt?: IsoDateTime | null;
  endsAt?: IsoDateTime | null;
  active: boolean;
  productId?: Uuid | null;
  categoryId?: Uuid | null;
}

/** Promoción (público). */
export interface PublicPromotionDto {
  id: Uuid;
  name: string;
  description?: string | null;
  promoType: string;
  value: Decimal;
  startsAt?: IsoDateTime | null;
  endsAt?: IsoDateTime | null;
  productId?: Uuid | null;
  categoryId?: Uuid | null;
}

/** Solicitud de promoción. */
export interface PromotionRequest {
  name: string;
  description?: string | null;
  promoType: string;
  value: Decimal;
  startsAt?: IsoDateTime | null;
  endsAt?: IsoDateTime | null;
  active?: boolean;
  productId?: Uuid | null;
  categoryId?: Uuid | null;
}
