import { Decimal, IsoDateTime, Uuid } from './common.model';
import { PaymentMethodCode } from './enums.model';

/** Caja física. */
export interface CashRegisterDto {
  id: Uuid;
  branchId?: Uuid | null;
  code: string;
  name: string;
  active: boolean;
}

/** Sesión de caja. */
export interface CashSessionDto {
  id: Uuid;
  registerId?: Uuid | null;
  registerName?: string | null;
  openedBy?: Uuid | null;
  openedByName?: string | null;
  closedBy?: Uuid | null;
  openedAt: IsoDateTime;
  closedAt?: IsoDateTime | null;
  openingAmount: Decimal;
  countedAmount?: Decimal | null;
  expectedAmount?: Decimal | null;
  differenceAmount?: Decimal | null;
  status: string;
  notes?: string | null;
}

/** Apertura de sesión de caja. */
export interface CashOpenRequest {
  registerId: Uuid;
  openingAmount: Decimal;
  notes?: string | null;
}

/** Movimiento de caja. */
export interface CashMovementDto {
  id: Uuid;
  sessionId?: Uuid;
  movementType: string;
  amount: Decimal;
  sourceType?: string | null;
  sourceId?: Uuid | null;
  actorId?: Uuid | null;
  actorName?: string | null;
  reason?: string | null;
  createdAt: IsoDateTime;
}

/** Solicitud de registrar movimiento de caja. */
export interface CashMovementRequest {
  movementType: string;
  amount: Decimal;
  reason?: string | null;
}

/** Conteo por medio de pago (arqueo). */
export interface CashCountLine {
  method: PaymentMethodCode | string;
  countedAmount: Decimal;
}

/** Solicitud de conteos de arqueo. */
export interface CashCountRequest {
  counts: CashCountLine[];
}

/** Cierre de caja. */
export interface CashCloseRequest {
  countedAmount?: Decimal | null;
  notes?: string | null;
  counts?: CashCountLine[];
}

/** Línea del arqueo por medio de pago. */
export interface CashReconciliationMethodLine {
  method: PaymentMethodCode | string;
  expected: Decimal;
  counted?: Decimal | null;
  difference?: Decimal | null;
}

/** Arqueo de caja por medio de pago: esperado vs contado y diferencia. */
export interface CashReconciliationDto {
  sessionId: Uuid;
  status: string;
  openingAmount: Decimal;
  lines: CashReconciliationMethodLine[];
  /** Alias de `lines` (nombre del contrato maestro). */
  items?: CashReconciliationItemDto[];
  totalExpected: Decimal;
  totalCounted?: Decimal | null;
  totalDifference?: Decimal | null;
  countedAt?: IsoDateTime | null;
}

/** Ítem de arqueo por medio de pago (nombre del contrato maestro). */
export type CashReconciliationItemDto = CashReconciliationMethodLine;
