import { IsoDateTime, Uuid } from './common.model';
import { CustomerDocumentType } from './enums.model';

/** Vehículo de un cliente. */
export interface CustomerVehicleDto {
  id: Uuid;
  customerId?: Uuid | null;
  plate?: string | null;
  vehicleVersionId?: Uuid | null;
  brandName?: string | null;
  modelName?: string | null;
  year?: number | null;
  color?: string | null;
  notes?: string | null;
  createdAt?: IsoDateTime | null;
}

/** Solicitud de vehículo de cliente. */
export interface CustomerVehicleRequest {
  plate?: string | null;
  vehicleVersionId?: Uuid | null;
  brandName?: string | null;
  modelName?: string | null;
  year?: number | null;
  color?: string | null;
  notes?: string | null;
}

/** Cliente. */
export interface CustomerDto {
  id: Uuid;
  documentType: CustomerDocumentType | string;
  documentNumber?: string | null;
  name: string;
  email?: string | null;
  phone?: string | null;
  address?: string | null;
  createdAt?: IsoDateTime | null;
  vehicles: CustomerVehicleDto[];
}

/** Solicitud de creación/actualización de cliente. */
export interface CustomerRequest {
  documentType: CustomerDocumentType | string;
  documentNumber: string;
  name: string;
  email?: string | null;
  phone?: string | null;
  address?: string | null;
}
