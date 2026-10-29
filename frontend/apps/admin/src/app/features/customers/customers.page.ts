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
import { CustomersService } from '@megalujos/api-client';
import { CUSTOMER_DOCUMENT_TYPES, CustomerDto } from '@megalujos/models';
import {
  MegaButtonComponent,
  MegaInputComponent,
  MegaModalComponent,
  MegaPaginationComponent,
  MegaSelectComponent,
  MegaTableColumn,
  MegaTableCellContext,
  MegaTableComponent,
  MegaTextareaComponent,
  MegaToastService,
} from '@megalujos/ui';
import { errorMessage } from '../../core/util/ui';

const DOCUMENT_TYPE_OPTIONS = CUSTOMER_DOCUMENT_TYPES.map((value) => ({
  value,
  label:
    value === 'CEDULA'
      ? 'Cédula'
      : value === 'RUC'
        ? 'RUC'
        : value === 'PASAPORTE'
          ? 'Pasaporte'
          : 'Consumidor final',
}));

/** Clientes con alta/edición y listado paginado. */
@Component({
  selector: 'app-customers-page',
  imports: [
    FormsModule,
    MegaInputComponent,
    MegaSelectComponent,
    MegaTextareaComponent,
    MegaModalComponent,
    MegaTableComponent,
    MegaPaginationComponent,
    MegaButtonComponent,
  ],
  templateUrl: './customers.page.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CustomersPage {
  private readonly customersService = inject(CustomersService);
  private readonly toast = inject(MegaToastService);
  private readonly destroyRef = inject(DestroyRef);

  readonly query = signal('');
  readonly page = signal(1);
  readonly size = 20;
  readonly total = signal(0);
  readonly rows = signal<CustomerDto[]>([]);
  readonly loading = signal(true);

  readonly modalOpen = signal(false);
  readonly editingId = signal<string | null>(null);
  readonly documentType = signal('CEDULA');
  readonly documentNumber = signal('');
  readonly name = signal('');
  readonly email = signal('');
  readonly phone = signal('');
  readonly address = signal('');
  readonly saving = signal(false);

  readonly documentTypeOptions = DOCUMENT_TYPE_OPTIONS;

  readonly rowKey = (row: CustomerDto): string => row.id;

  private readonly actionsCell = viewChild<TemplateRef<MegaTableCellContext<CustomerDto>>>(
    'actionsCell',
  );

  readonly columns = computed<MegaTableColumn<CustomerDto>[]>(() => [
    { key: 'documentNumber', header: 'Identificación', width: '12rem' },
    { key: 'name', header: 'Nombre' },
    { key: 'phone', header: 'Teléfono', width: '11rem' },
    { key: 'email', header: 'Email' },
    {
      header: 'Acciones',
      align: 'end',
      width: '9rem',
      cell: this.actionsCell() ?? undefined,
    },
  ]);

  constructor() {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.customersService
      .list({ q: this.query() || undefined, page: this.page() - 1, size: this.size })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (page) => {
          this.rows.set(page.items ?? []);
          this.total.set(page.totalElements ?? 0);
          this.loading.set(false);
        },
        error: (err: unknown) =>
          this.toast.error(errorMessage(err), { title: 'No se pudieron cargar los clientes' }),
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
    this.documentType.set('CEDULA');
    this.documentNumber.set('');
    this.name.set('');
    this.email.set('');
    this.phone.set('');
    this.address.set('');
    this.modalOpen.set(true);
  }

  openEdit(customer: CustomerDto): void {
    this.editingId.set(customer.id);
    this.documentType.set(String(customer.documentType ?? 'CEDULA'));
    this.documentNumber.set(customer.documentNumber ?? '');
    this.name.set(customer.name);
    this.email.set(customer.email ?? '');
    this.phone.set(customer.phone ?? '');
    this.address.set(customer.address ?? '');
    this.modalOpen.set(true);
  }

  save(): void {
    const name = this.name().trim();
    const documentNumber = this.documentNumber().trim();
    if (!name || !documentNumber) {
      this.toast.warning('Nombre e identificación son obligatorios.');
      return;
    }
    const request = {
      documentType: this.documentType(),
      documentNumber,
      name,
      email: this.email().trim() || null,
      phone: this.phone().trim() || null,
      address: this.address().trim() || null,
    };
    this.saving.set(true);
    const operation$ = this.editingId()
      ? this.customersService.update(this.editingId() as string, request)
      : this.customersService.create(request);
    operation$.pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: () => {
        this.saving.set(false);
        this.modalOpen.set(false);
        this.toast.success(`Cliente ${this.editingId() ? 'actualizado' : 'creado'}.`);
        this.load();
      },
      error: (err: unknown) => {
        this.saving.set(false);
        this.toast.error(errorMessage(err), { title: 'No se pudo guardar el cliente' });
      },
    });
  }
}
