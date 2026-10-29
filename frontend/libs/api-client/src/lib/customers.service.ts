import { Injectable } from '@angular/core';
import {
  CustomerDto,
  CustomerRequest,
  CustomerVehicleDto,
  CustomerVehicleRequest,
  PageResponse,
} from '@megalujos/models';
import { Observable } from 'rxjs';
import { ApiBaseService, ListParams } from './api-base';

/** Clientes y vehículos. */
@Injectable({ providedIn: 'root' })
export class CustomersService extends ApiBaseService {
  /** Listado paginado de clientes. */
  list(params?: ListParams & { q?: string }): Observable<PageResponse<CustomerDto>> {
    return this.httpGet<PageResponse<CustomerDto>>('/customers', params);
  }

  /** Detalle de cliente con vehículos. */
  get(id: string): Observable<CustomerDto> {
    return this.httpGet<CustomerDto>(`/customers/${id}`);
  }

  /** Alias de `get`. */
  getById(id: string): Observable<CustomerDto> {
    return this.get(id);
  }

  /** Crea un cliente. */
  create(request: CustomerRequest): Observable<CustomerDto> {
    return this.httpPost<CustomerDto>('/customers', request);
  }

  /** Actualiza un cliente. */
  update(id: string, request: CustomerRequest): Observable<CustomerDto> {
    return this.httpPut<CustomerDto>(`/customers/${id}`, request);
  }

  /** Vehículos de un cliente. */
  listVehicles(customerId: string): Observable<CustomerVehicleDto[]> {
    return this.httpGet<CustomerVehicleDto[]>(`/customers/${customerId}/vehicles`);
  }

  /** Agrega un vehículo al cliente. */
  createVehicle(customerId: string, request: CustomerVehicleRequest): Observable<CustomerVehicleDto> {
    return this.httpPost<CustomerVehicleDto>(`/customers/${customerId}/vehicles`, request);
  }

  /** Alias de `createVehicle`. */
  addVehicle(customerId: string, request: CustomerVehicleRequest): Observable<CustomerVehicleDto> {
    return this.createVehicle(customerId, request);
  }

  /** Actualiza un vehículo del cliente. */
  updateVehicle(
    customerId: string,
    vehicleId: string,
    request: CustomerVehicleRequest,
  ): Observable<CustomerVehicleDto> {
    return this.httpPut<CustomerVehicleDto>(`/customers/${customerId}/vehicles/${vehicleId}`, request);
  }

  /** Elimina un vehículo del cliente. */
  removeVehicle(customerId: string, vehicleId: string): Observable<void> {
    return this.httpDelete<void>(`/customers/${customerId}/vehicles/${vehicleId}`);
  }
}
