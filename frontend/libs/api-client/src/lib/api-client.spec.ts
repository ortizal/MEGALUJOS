import { HttpErrorResponse } from '@angular/common/http';
import { API_BASE_PATH, apiUrl, defaultApiConfig } from './api-config';
import { ApiError, isApiError, toApiError } from './util/api-error';

describe('api-config', () => {
  it('usa /api/v1 por defecto', () => {
    expect(API_BASE_PATH).toBe('/api/v1');
    expect(defaultApiConfig().baseUrl).toBe('/api/v1');
    expect(apiUrl('/products')).toBe('/api/v1/products');
    expect(apiUrl('products')).toBe('/api/v1/products');
  });

  it('normaliza trailing slash en baseUrl', () => {
    expect(apiUrl('/sales', { baseUrl: '/api/v1/' })).toBe('/api/v1/sales');
  });
});

describe('ApiError', () => {
  it('normaliza problem+json', () => {
    const error = toApiError(
      new HttpErrorResponse({
        status: 400,
        error: {
          type: 'about:blank',
          title: 'Validación',
          status: 400,
          detail: 'Campos inválidos',
          traceId: 'abc-123',
          fieldErrors: { sku: ['SKU obligatorio'] },
        },
      }),
    );
    expect(isApiError(error)).toBe(true);
    expect(error.status).toBe(400);
    expect(error.traceId).toBe('abc-123');
    expect(error.isValidation).toBe(true);
    expect(error.fieldErrors?.['sku']).toEqual(['SKU obligatorio']);
  });

  it('marca offline cuando no hay respuesta', () => {
    const error = toApiError(new HttpErrorResponse({ status: 0 }));
    expect(error.isOffline).toBe(true);
    expect(error).toBeInstanceOf(ApiError);
  });
});
