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
import { CatalogAdminService } from '@megalujos/api-client';
import { BrandDto } from '@megalujos/models';
import {
  MegaButtonComponent,
  MegaConfirmService,
  MegaInputComponent,
  MegaModalComponent,
  MegaTableColumn,
  MegaTableCellContext,
  MegaTableComponent,
  MegaToastService,
} from '@megalujos/ui';
import { errorMessage, formatDate } from '../../core/util/ui';

/** Marcas de catálogo con alta/edición y activación. */
@Component({
  selector: 'app-brands-admin-page',
  imports: [
    FormsModule,
    MegaInputComponent,
    MegaModalComponent,
    MegaTableComponent,
    MegaButtonComponent,
  ],
  templateUrl: './brands-admin.page.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class BrandsAdminPage {
  private readonly catalog = inject(CatalogAdminService);
  private readonly toast = inject(MegaToastService);
  private readonly confirm = inject(MegaConfirmService);
  private readonly destroyRef = inject(DestroyRef);

  readonly rows = signal<BrandDto[]>([]);
  readonly loading = signal(true);

  readonly modalOpen = signal(false);
  readonly editingId = signal<string | null>(null);
  readonly name = signal('');
  readonly saving = signal(false);

  readonly rowKey = (row: BrandDto): string => row.id;

  private readonly actionsCell = viewChild<TemplateRef<MegaTableCellContext<BrandDto>>>(
    'actionsCell',
  );

  readonly columns = computed<MegaTableColumn<BrandDto>[]>(() => [
    { key: 'name', header: 'Marca' },
    { key: 'createdAt', header: 'Creada', width: '11rem' },
    { key: 'active', header: 'Activa', width: '7rem' },
    {
      header: 'Acciones',
      align: 'end',
      width: '14rem',
      cell: this.actionsCell() ?? undefined,
    },
  ]);

  constructor() {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.catalog
      .listBrands()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (brands) => {
          this.rows.set(
            (brands ?? []).map((brand) => ({
              ...brand,
              createdAt: formatDate(brand.createdAt),
            })),
          );
          this.loading.set(false);
        },
        error: (err: unknown) => {
          this.loading.set(false);
          this.toast.error(errorMessage(err), { title: 'No se pudieron cargar las marcas' });
        },
      });
  }

  openCreate(): void {
    this.editingId.set(null);
    this.name.set('');
    this.modalOpen.set(true);
  }

  openEdit(brand: BrandDto): void {
    this.editingId.set(brand.id);
    this.name.set(brand.name);
    this.modalOpen.set(true);
  }

  save(): void {
    const name = this.name().trim();
    if (!name) {
      this.toast.warning('El nombre es obligatorio.');
      return;
    }
    this.saving.set(true);
    const operation$ = this.editingId()
      ? this.catalog.updateBrand(this.editingId() as string, { name })
      : this.catalog.createBrand({ name });
    operation$.pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: () => {
        this.saving.set(false);
        this.modalOpen.set(false);
        this.toast.success(`Marca ${this.editingId() ? 'actualizada' : 'creada'}.`);
        this.load();
      },
      error: (err: unknown) => {
        this.saving.set(false);
        this.toast.error(errorMessage(err), { title: 'No se pudo guardar la marca' });
      },
    });
  }

  async toggleActive(brand: BrandDto): Promise<void> {
    const next = !brand.active;
    const ok = await this.confirm.open({
      title: next ? 'Activar marca' : 'Desactivar marca',
      message: `La marca "${brand.name}" quedará ${next ? 'activa' : 'inactiva'}.`,
      confirmText: next ? 'Activar' : 'Desactivar',
      danger: !next,
    });
    if (!ok) {
      return;
    }
    this.catalog
      .setBrandActive(brand.id, next)
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
