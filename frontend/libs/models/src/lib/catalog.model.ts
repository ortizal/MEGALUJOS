import { Decimal, IsoDateTime, Uuid } from './common.model';
import { StockAvailability } from './enums.model';

/** Imagen de producto (admin). */
export interface ProductImageDto {
  id: Uuid;
  mediaId: Uuid;
  url: string;
  sortOrder: number;
  primary: boolean;
}

/** Imagen de producto (público). */
export interface ProductPublicImageDto {
  url: string;
  alt?: string;
  primary: boolean;
  sortOrder: number;
}

/** Variante de producto (admin). */
export interface VariantDto {
  id?: Uuid | null;
  sku: string;
  barcode?: string | null;
  attributes?: Record<string, string | number | boolean | null>;
  salePrice: Decimal;
  costPrice?: Decimal | null;
  minStock?: Decimal | null;
  active?: boolean;
}

/** Producto para el panel de administración (con variantes e imágenes). */
export interface ProductAdminDto {
  id: Uuid;
  sku: string;
  name: string;
  description?: string | null;
  category: { id: Uuid; name: string };
  brand?: { id: Uuid; name: string } | null;
  categoryId: Uuid | null;
  categoryName?: string | null;
  categorySlug?: string | null;
  brandId: Uuid | null;
  brandName?: string | null;
  productType: string;
  unit?: string | null;
  trackStock: boolean;
  published: boolean;
  active: boolean;
  createdAt?: IsoDateTime | null;
  updatedAt?: IsoDateTime | null;
  variants: VariantDto[];
  images: ProductImageDto[];
}

/** Alias del contrato backend ProductResponse. */
export type ProductResponse = ProductAdminDto;

/** Producto de la vitrina pública. */
export interface ProductPublicDto {
  id: Uuid;
  sku: string;
  name: string;
  description?: string | null;
  productType: string;
  categoryName?: string | null;
  categorySlug?: string | null;
  brandName?: string | null;
  price: Decimal;
  availableStock: Decimal;
  availability: StockAvailability | string;
  attributes: Record<string, string>;
  images: ProductPublicImageDto[];
  whatsappUrl: string;
  /** Ruta pública del producto (incluida por el backend). */
  slug?: string | null;
  currency?: 'USD';
}

/** Categoría de catálogo (puede ser anidada). */
export interface CategoryDto {
  id: Uuid;
  name: string;
  slug: string;
  description?: string | null;
  sortOrder: number;
  active: boolean;
  parentId?: Uuid | null;
  parent?: CategoryDto | null;
  createdAt?: IsoDateTime | null;
  children?: CategoryDto[];
}

/** Marca de catálogo. */
export interface BrandDto {
  id: Uuid;
  name: string;
  logoMediaId?: Uuid | null;
  logoUrl?: string | null;
  active: boolean;
  createdAt?: IsoDateTime | null;
}

/** Marca de vehículo. */
export interface VehicleBrand {
  id: Uuid;
  name: string;
}

/** Modelo de vehículo. */
export interface VehicleModel {
  id: Uuid;
  brandId: Uuid;
  brandName?: string | null;
  name: string;
}

/** Versión de vehículo (años). */
export interface VehicleVersion {
  id: Uuid;
  modelId: Uuid;
  modelName?: string | null;
  name: string;
  yearFrom?: number | null;
  yearTo?: number | null;
}

/** Alias del contrato (nombres del documento maestro). */
export type VehicleBrandDto = VehicleBrand;
export type VehicleModelDto = VehicleModel;
export type VehicleVersionDto = VehicleVersion;

/** Compatibilidad de variante con vehículo. */
export interface VehicleCompatibilityDto {
  vehicleVersionId: Uuid;
  vehicleVersionName?: string | null;
  verified: boolean;
  notes?: string | null;
}

/** Resultado de lookup de variante por código de barras. */
export interface VariantLookupDto {
  variantId: Uuid;
  sku: string;
  barcode?: string | null;
  salePrice: Decimal;
  attributes?: Record<string, string | number | boolean | null>;
  product: {
    id: Uuid;
    sku: string;
    name: string;
    productType?: string | null;
    unit?: string | null;
    categoryName?: string | null;
    brandName?: string | null;
    published: boolean;
    active: boolean;
  };
}

/** Registro de consulta WhatsApp desde la vitrina. */
export interface CatalogInquiryRequest {
  variantId?: Uuid | null;
  customerPhone?: string | null;
}
