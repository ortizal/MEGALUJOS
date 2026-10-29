import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  computed,
  inject,
  signal,
  viewChild,
  TemplateRef,
} from '@angular/core';
import { FormsModule } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { PrintService } from '@megalujos/api-client';
import { PrintJobDto } from '@megalujos/models';
import {
  MegaButtonComponent,
  MegaConfirmService,
  MegaEmptyStateComponent,
  MegaInputComponent,
  MegaPaginationComponent,
  MegaSelectComponent,
  MegaStatusBadgeComponent,
  MegaTableColumn,
  MegaTableCellContext,
  MegaTableComponent,
  MegaToastService,
} from '@megalujos/ui';
import { errorMessage, formatDateTime } from '../../core/util/ui';

/** Cola de trabajos de impresión: listado, filtro por estado y marcado como impreso (auditoría de reimpresiones). */
@Component({
  selector: 'app-print-jobs-page',
  imports: [
    FormsModule,
    MegaButtonComponent,
    MegaEmptyStateComponent,
    MegaInputComponent,
    MegaPaginationComponent,
    MegaSelectComponent,
    MegaStatusBadgeComponent,
    MegaTableComponent,
  ],
  templateUrl: './print-jobs.page.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PrintJobsPage {
  private readonly print = inject(PrintService);
  private readonly toast = inject(MegaToastService);
  private readonly confirm = inject(MegaConfirmService);
  private readonly destroyRef = inject(DestroyRef);

  readonly status = signal('');
  readonly page = signal(1);
  readonly size = 15;
  readonly total = signal(0);
  readonly rows = signal<PrintJobDto[]>([]);
  readonly loading = signal(true);
  readonly error = signal<string | null>(null);

  readonly rowKey = (row: PrintJobDto): string => row.id;

  readonly statusCell = viewChild<TemplateRef<MegaTableCellContext<PrintJobDto>>>('statusCell');
  readonly actionsCell = viewChild<TemplateRef<MegaTableCellContext<PrintJobDto>>>('actionsCell');

  readonly columns = computed<MegaTableColumn<PrintJobDto>[]>(() => [
    { key: 'createdAt', header: 'Creado', width: '14rem' },
    { key: 'jobType', header: 'Tipo' },
    { key: 'sourceType', header: 'Origen', width: '11rem' },
    { key: 'sourceId', header: 'ID origen', width: '12rem' },
    { key: 'format', header: 'Formato', width: '9rem' },
    { header: 'Estado', width: '11rem', cell: this.statusCell() ?? undefined },
    { key: 'copies', header: 'Copias', width: '7rem', align: 'end' },
    { header: 'Acciones', align: 'end', width: '13rem', cell: this.actionsCell() ?? undefined },
  ]);

  readonly statusOptions = [
    { value: '', label: 'Todos' },
    { value: 'QUEUED', label: 'En cola' },
    { value: 'PRINTED', label: 'Impreso' },
    { value: 'FAILED', label: 'Fallido' },
  ];

  constructor() {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.error.set(null);
    this.print
      .list({
        status: this.status() || undefined,
        page: this.page() - 1,
        size: this.size,
      })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (page) => {
          this.rows.set(page.items ?? []);
          this.total.set(page.totalElements ?? 0);
          this.loading.set(false);
        },
        error: (err) => {
          const message = errorMessage(err);
          this.error.set(message);
          this.loading.set(false);
          this.toast.error(message, { title: 'No se pudieron cargar los trabajos de impresión' });
        },
      });
  }

  goToPage(page: number): void {
    this.page.set(page);
    this.load();
  }

  applyFilters(): void {
    this.page.set(1);
    this.load();
  }

  clearFilters(): void {
    this.status.set('');
    this.page.set(1);
    this.load();
  }

  markPrinted(job: PrintJobDto): void {
    this.confirm
      .ask(`¿Marcar el trabajo "${job.jobType}" como impreso?`)
      .then((ok) => {
        if (!ok) {
          return;
        }
        this.print
          .markPrinted(job.id)
          .pipe(takeUntilDestroyed(this.destroyRef))
          .subscribe({
            next: () => {
              this.toast.success('Trabajo marcado como impreso.');
              this.load();
            },
            error: (err) => this.toast.error(errorMessage(err), { title: 'No se pudo marcar el trabajo' }),
          });
      });
  }

  readonly formatDateTime = formatDateTime;
}
