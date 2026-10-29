import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  TemplateRef,
  computed,
  inject,
  signal,
  viewChild,
} from '@angular/core';
import { FormsModule } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { InvoicingService } from '@megalujos/api-client';
import {
  ELECTRONIC_DOCUMENT_STATUS_LABELS,
  ElectronicDocumentDetailDto,
  ElectronicDocumentDto,
  ElectronicDocumentStatus,
} from '@megalujos/models';
import {
  MegaButtonComponent,
  MegaConfirmService,
  MegaDatePickerComponent,
  MegaEmptyStateComponent,
  MegaInputComponent,
  MegaModalComponent,
  MegaPaginationComponent,
  MegaSelectComponent,
  MegaStatusBadgeComponent,
  MegaTableColumn,
  MegaTableCellContext,
  MegaTableComponent,
  MegaTextareaComponent,
  MegaToastService,
} from '@megalujos/ui';
import {
  downloadBlob,
  errorMessage,
  endOfDayIso,
  formatDateTime,
  newIdempotencyKey,
  startOfDayIso,
} from '../../core/util/ui';

const STATUS_OPTIONS = [
  { value: '', label: 'Todos los estados' },
  ...Object.entries(ELECTRONIC_DOCUMENT_STATUS_LABELS).map(([value, label]) => ({ value, label })),
];

const TYPE_OPTIONS = [
  { value: '', label: 'Todos los tipos' },
  { value: 'INVOICE', label: 'Factura' },
  { value: 'CREDIT_NOTE', label: 'Nota de crédito' },
];

/** Documentos electrónicos: estado SRI, eventos, RIDE y notas de crédito. */
@Component({
  selector: 'app-documents-page',
  imports: [
    FormsModule,
    MegaInputComponent,
    MegaSelectComponent,
    MegaDatePickerComponent,
    MegaTextareaComponent,
    MegaModalComponent,
    MegaTableComponent,
    MegaPaginationComponent,
    MegaEmptyStateComponent,
    MegaStatusBadgeComponent,
    MegaButtonComponent,
  ],
  templateUrl: './documents.page.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DocumentsPage {
  private readonly invoicing = inject(InvoicingService);
  private readonly toast = inject(MegaToastService);
  private readonly confirm = inject(MegaConfirmService);
  private readonly destroyRef = inject(DestroyRef);

  readonly status = signal('');
  readonly documentType = signal('');
  readonly dateFrom = signal('');
  readonly dateTo = signal('');
  readonly page = signal(1);
  readonly size = 20;
  readonly total = signal(0);
  readonly rows = signal<ElectronicDocumentDto[]>([]);
  readonly loading = signal(true);

  readonly selected = signal<ElectronicDocumentDetailDto | null>(null);

  readonly creditModal = signal(false);
  readonly creditReasonCode = signal('1');
  readonly creditReason = signal('');
  readonly creditSubmitting = signal(false);

  readonly statusOptions = STATUS_OPTIONS;
  readonly typeOptions = TYPE_OPTIONS;

  readonly rowKey = (row: ElectronicDocumentDto): string => row.id;

  private readonly statusCell = viewChild<
    TemplateRef<MegaTableCellContext<ElectronicDocumentDto>>
  >('statusCell');
  private readonly actionsCell = viewChild<
    TemplateRef<MegaTableCellContext<ElectronicDocumentDto>>
  >('actionsCell');

  readonly columns = computed<MegaTableColumn<ElectronicDocumentDto>[]>(() => [
    { key: 'createdAt', header: 'Fecha', width: '12rem' },
    { key: 'documentType', header: 'Tipo', width: '9rem' },
    { key: 'sequenceNumber', header: 'Secuencial', align: 'end', width: '9rem' },
    { key: 'status', header: 'Estado', width: '11rem', cell: this.statusCell() ?? undefined },
    { key: 'lastError', header: 'Último error' },
    {
      header: 'Acciones',
      align: 'end',
      width: '16rem',
      cell: this.actionsCell() ?? undefined,
    },
  ]);

  constructor() {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.invoicing
      .listDocuments({
        status: this.status() || undefined,
        documentType: this.documentType() || undefined,
        dateFrom: this.dateFrom() ? startOfDayIso(this.dateFrom()) : undefined,
        dateTo: this.dateTo() ? endOfDayIso(this.dateTo()) : undefined,
        page: this.page() - 1,
        size: this.size,
      })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (page) => {
          this.rows.set(
            (page.items ?? []).map((row) => ({ ...row, createdAt: formatDateTime(row.createdAt) })),
          );
          this.total.set(page.totalElements ?? 0);
          this.loading.set(false);
        },
        error: (err: unknown) =>
          this.toast.error(errorMessage(err), { title: 'No se pudieron cargar los documentos' }),
      });
  }

  applyFilters(): void {
    this.page.set(1);
    this.load();
  }

  goToPage(page: number): void {
    this.page.set(page);
    this.load();
  }

  openDetail(document: ElectronicDocumentDto): void {
    this.invoicing
      .getDocument(document.id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (detail) => this.selected.set(detail),
        error: (err: unknown) =>
          this.toast.error(errorMessage(err), { title: 'No se pudo abrir el documento' }),
      });
  }

  async retry(document: ElectronicDocumentDto): Promise<void> {
    const ok = await this.confirm.open({
      title: 'Reintentar envío al SRI',
      message: `Se reintentará el procesamiento del documento secuencial ${document.sequenceNumber}.`,
      confirmText: 'Reintentar',
    });
    if (!ok) {
      return;
    }
    this.invoicing
      .retry(document.id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.toast.success('Documento enviado a la cola de reintentos.');
          this.load();
        },
        error: (err: unknown) =>
          this.toast.error(errorMessage(err), { title: 'No se pudo reintentar' }),
      });
  }

  downloadRide(document: ElectronicDocumentDto): void {
    this.invoicing
      .ride(document.id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (blob) => downloadBlob(blob, `RIDE-${document.sequenceNumber}.pdf`),
        error: (err: unknown) =>
          this.toast.error(errorMessage(err), { title: 'No se pudo descargar el RIDE' }),
      });
  }

  openCreditModal(): void {
    this.creditReasonCode.set('1');
    this.creditReason.set('');
    this.creditModal.set(true);
  }

  submitCreditNote(saleId: string | null | undefined): void {
    if (!saleId) {
      this.toast.warning('El documento no tiene venta asociada.');
      return;
    }
    if (this.creditReason().trim().length < 5) {
      this.toast.warning('Describe el motivo de la nota de crédito.');
      return;
    }
    this.creditSubmitting.set(true);
    this.invoicing
      .creditNote(
        {
          saleId,
          reasonCode: this.creditReasonCode(),
          reason: this.creditReason().trim(),
        },
        newIdempotencyKey(),
      )
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.creditSubmitting.set(false);
          this.creditModal.set(false);
          this.toast.success('Nota de crédito enviada a la cola.');
          this.load();
        },
        error: (err: unknown) => {
          this.creditSubmitting.set(false);
          this.toast.error(errorMessage(err), { title: 'No se pudo emitir la nota de crédito' });
        },
      });
  }

  statusLabel(code: string): string {
    return ELECTRONIC_DOCUMENT_STATUS_LABELS[code as ElectronicDocumentStatus] ?? code;
  }

  canRetry(document: ElectronicDocumentDto): boolean {
    const status = (document.status ?? '').toUpperCase();
    return status === 'ERROR' || status === 'REJECTED';
  }
}
