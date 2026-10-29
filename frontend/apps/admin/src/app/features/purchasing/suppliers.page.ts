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
import { PurchasingService } from '@megalujos/api-client';
import { SupplierDto } from '@megalujos/models';
import {
  MegaButtonComponent,
  MegaConfirmService,
  MegaInputComponent,
  MegaModalComponent,
  MegaPaginationComponent,
  MegaTableColumn,
  MegaTableCellContext,
  MegaTableComponent,
  MegaTextareaComponent,
  MegaToastService,
} from '@megalujos/ui';
import { errorMessage, formatDate } from '../../core/util/ui';

/** Proveedores con alta/edición y activación. */
@Component({
  selector: 'app-suppliers-page',
  imports: [
    FormsModule,
    MegaInputComponent,
    MegaTextareaComponent,
    MegaModalComponent,
    MegaTableComponent,
    MegaPaginationComponent,
    MegaButtonComponent,
  ],
  templateUrl: './suppliers.page.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SuppliersPage {
  private readonly purchasing = inject(PurchasingService);
  private readonly toast = inject(MegaToastService);
  private readonly confirm = inject(MegaConfirmService);
  private readonly destroyRef = inject(DestroyRef);

  readonly query = signal('');
  readonly page = signal(1);
  readonly size = 20;
  readonly total = signal(0);
  readonly rows = signal<SupplierDto[]>([]);
  readonly loading = signal(true);

  readonly modalOpen = signal(false);
  readonly editingId = signal<string | null>(null);
  readonly taxId = signal('');
  readonly name = signal('');
  readonly contactName = signal('');
  readonly email = signal('');
  readonly phone = signal('');
  readonly address = signal('');
  readonly saving = signal(false);

  readonly rowKey = (row: SupplierDto): string => row.id;

  private readonly actionsCell = viewChild<TemplateRef<MegaTableCellContext<SupplierDto>>>(
    'actionsCell',
  );

  readonly columns = computed<MegaTableColumn<SupplierDto>[]>(() => [
    { key: 'name', header: 'Proveedor' },
    { key: 'taxId', header: 'RUC', width: '12rem' },
    { key: 'contactName', header: 'Contacto', width: '12rem' },
    { key: 'phone', header: 'Teléfono', width: '11rem' },
    { key: 'active', header: 'Activo', width: '7rem' },
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
    this.purchasing
      .listSuppliers({ q: this.query() || undefined, page: this.page() - 1, size: this.size })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (page) => {
          this.rows.set(
            (page.items ?? []).map((row) => ({ ...row, createdAt: formatDate(row.createdAt) })),
          );
          this.total.set(page.totalElements ?? 0);
          this.loading.set(false);
        },
        error: (err: unknown) =>
          this.toast.error(errorMessage(err), { title: 'No se pudieron cargar los proveedores' }),
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

  openCreate(): void {
    this.editingId.set(null);
    this.taxId.set('');
    this.name.set('');
    this.contactName.set('');
    this.email.set('');
    this.phone.set('');
    this.address.set('');
    this.modalOpen.set(true);
  }

  openEdit(supplier: SupplierDto): void {
    this.editingId.set(supplier.id);
    this.taxId.set(supplier.taxId ?? '');
    this.name.set(supplier.name);
    this.contactName.set(supplier.contactName ?? '');
    this.email.set(supplier.email ?? '');
    this.phone.set(supplier.phone ?? '');
    this.address.set(supplier.address ?? '');
    this.modalOpen.set(true);
  }

  save(): void {
    const name = this.name().trim();
    if (!name) {
      this.toast.warning('El nombre es obligatorio.');
      return;
    }
    const request = {
      taxId: this.taxId().trim() || null,
      name,
      contactName: this.contactName().trim() || null,
      email: this.email().trim() || null,
      phone: this.phone().trim() || null,
      address: this.address().trim() || null,
    };
    this.saving.set(true);
    const operation$ = this.editingId()
      ? this.purchasing.updateSupplier(this.editingId() as string, request)
      : this.purchasing.createSupplier(request);
    operation$.pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: () => {
        this.saving.set(false);
        this.modalOpen.set(false);
        this.toast.success(`Proveedor ${this.editingId() ? 'actualizado' : 'creado'}.`);
        this.load();
      },
      error: (err: unknown) => {
        this.saving.set(false);
        this.toast.error(errorMessage(err), { title: 'No se pudo guardar el proveedor' });
      },
    });
  }

  async toggleActive(supplier: SupplierDto): Promise<void> {
    const next = !supplier.active;
    const ok = await this.confirm.open({
      title: next ? 'Activar proveedor' : 'Desactivar proveedor',
      message: `El proveedor "${supplier.name}" quedará ${next ? 'activo' : 'inactivo'}.`,
      confirmText: next ? 'Activar' : 'Desactivar',
      danger: !next,
    });
    if (!ok) {
      return;
    }
    this.purchasing
      .setSupplierActive(supplier.id, next)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.toast.success('Estado actualizado.');
          this.load();
        },
        error: (err: unknown) =>
          this.toast.error(errorMessage(err), { title: 'No se pudo cambiar el estado' }),
      });
  }
}
