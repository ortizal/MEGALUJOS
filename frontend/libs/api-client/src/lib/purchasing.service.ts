import { Injectable } from '@angular/core';
import {
  GoodsReceiptDto,
  GoodsReceiptRequest,
  PageResponse,
  PurchaseOrderDto,
  PurchaseOrderRequest,
  SupplierDto,
  SupplierRequest,
} from '@megalujos/models';
import { Observable } from 'rxjs';
import { ApiBaseService, ListParams } from './api-base';

/** Proveedores, órdenes de compra y recepciones. */
@Injectable({ providedIn: 'root' })
export class PurchasingService extends ApiBaseService {
  // --------------------------------------------------------------- proveedores
  listSuppliers(params?: ListParams & { q?: string }): Observable<PageResponse<SupplierDto>> {
    return this.httpGet<PageResponse<SupplierDto>>('/suppliers', params);
  }

  createSupplier(request: SupplierRequest): Observable<SupplierDto> {
    return this.httpPost<SupplierDto>('/suppliers', request);
  }

  updateSupplier(id: string, request: SupplierRequest): Observable<SupplierDto> {
    return this.httpPut<SupplierDto>(`/suppliers/${id}`, request);
  }

  setSupplierActive(id: string, active: boolean): Observable<SupplierDto> {
    return this.httpPatch<SupplierDto>(`/suppliers/${id}/active`, undefined, { active });
  }

  // -------------------------------------------------------- órdenes de compra
  listPurchaseOrders(params?: ListParams & { q?: string; status?: string }): Observable<PageResponse<PurchaseOrderDto>> {
    return this.httpGet<PageResponse<PurchaseOrderDto>>('/purchase-orders', params);
  }

  getPurchaseOrderById(id: string): Observable<PurchaseOrderDto> {
    return this.httpGet<PurchaseOrderDto>(`/purchase-orders/${id}`);
  }

  createPurchaseOrder(request: PurchaseOrderRequest): Observable<PurchaseOrderDto> {
    return this.httpPost<PurchaseOrderDto>('/purchase-orders', request);
  }

  updatePurchaseOrder(id: string, request: PurchaseOrderRequest): Observable<PurchaseOrderDto> {
    return this.httpPut<PurchaseOrderDto>(`/purchase-orders/${id}`, request);
  }

  /** Marca la orden como ordenada al proveedor. */
  markOrdered(id: string): Observable<PurchaseOrderDto> {
    return this.httpPost<PurchaseOrderDto>(`/purchase-orders/${id}/order`, {});
  }

  // -------------------------------------------------------------- recepciones
  createReceipt(id: string, request: GoodsReceiptRequest): Observable<GoodsReceiptDto> {
    return this.httpPost<GoodsReceiptDto>(`/purchase-orders/${id}/receipts`, request);
  }

  listReceipts(id: string, params?: ListParams): Observable<PageResponse<GoodsReceiptDto>> {
    return this.httpGet<PageResponse<GoodsReceiptDto>>(`/purchase-orders/${id}/receipts`, params);
  }
}
