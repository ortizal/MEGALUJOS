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
import { AuditAdminService } from '@megalujos/api-client';
import { AuditLogDto } from '@megalujos/models';
import {
  MegaButtonComponent,
  MegaEmptyStateComponent,
  MegaInputComponent,
  MegaPaginationComponent,
  MegaStatusBadgeComponent,
  MegaTableColumn,
  MegaTableCellContext,
  MegaTableComponent,
  MegaToastService,
} from '@megalujos/ui';
import { errorMessage, formatDateTime, shortId } from '../../core/util/ui';

/** Consulta paginada del registro de auditoría con filtros por actor, entidad y acción. */
@Component({
  selector: 'app-audit-page',
  imports: [
    FormsModule,
    MegaButtonComponent,
    MegaEmptyStateComponent,
    MegaInputComponent,
    MegaPaginationComponent,
    MegaStatusBadgeComponent,
    MegaTableComponent,
  ],
  templateUrl: './audit.page.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AuditPage {
  private readonly audit = inject(AuditAdminService);
  private readonly toast = inject(MegaToastService);
  private readonly destroyRef = inject(DestroyRef);

  readonly actorId = signal('');
  readonly entityType = signal('');
  readonly action = signal('');
  readonly page = signal(1);
  readonly size = 15;
  readonly total = signal(0);
  readonly rows = signal<AuditLogDto[]>([]);
  readonly loading = signal(true);
  readonly error = signal<string | null>(null);

  readonly rowKey = (row: AuditLogDto): string => row.id;

  readonly actorCell = viewChild<TemplateRef<MegaTableCellContext<AuditLogDto>>>('actorCell');
  readonly entityCell = viewChild<TemplateRef<MegaTableCellContext<AuditLogDto>>>('entityCell');

  readonly columns = computed<MegaTableColumn<AuditLogDto>[]>(() => [
    { key: 'occurredAt', header: 'Fecha', width: '14rem' },
    { header: 'Actor', width: '12rem', cell: this.actorCell() ?? undefined },
    { key: 'action', header: 'Acción' },
    { header: 'Entidad', width: '15rem', cell: this.entityCell() ?? undefined },
    { key: 'entityId', header: 'ID entidad', width: '13rem' },
  ]);

  constructor() {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.error.set(null);
    this.audit
      .list({
        actorId: this.actorId() || undefined,
        entityType: this.entityType() || undefined,
        action: this.action() || undefined,
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
          this.toast.error(message, { title: 'No se pudo cargar la auditoría' });
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
    this.actorId.set('');
    this.entityType.set('');
    this.action.set('');
    this.page.set(1);
    this.load();
  }

  readonly formatDateTime = formatDateTime;
  readonly shortId = shortId;
}
