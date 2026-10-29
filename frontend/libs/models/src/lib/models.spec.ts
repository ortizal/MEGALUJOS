import {
  CUSTOMER_DOCUMENT_TYPES,
  PAYMENT_METHOD_LABELS,
  PURCHASE_ORDER_STATUS_LABELS,
  SALE_STATUS_LABELS,
} from './enums.model';

describe('models', () => {
  it('expone etiquetas de estados en español', () => {
    expect(SALE_STATUS_LABELS['PAID']).toBe('Pagada');
    expect(PURCHASE_ORDER_STATUS_LABELS['DRAFT']).toBe('Borrador');
  });

  it('expone etiquetas de medios de pago', () => {
    expect(PAYMENT_METHOD_LABELS['CASH']).toBe('Efectivo');
    expect(PAYMENT_METHOD_LABELS['CARD']).toBe('Tarjeta');
  });

  it('incluye los tipos de documento admitidos', () => {
    expect(CUSTOMER_DOCUMENT_TYPES).toContain('CEDULA');
    expect(CUSTOMER_DOCUMENT_TYPES).toContain('RUC');
    expect(CUSTOMER_DOCUMENT_TYPES).toContain('CONSUMIDOR_FINAL');
  });
});
