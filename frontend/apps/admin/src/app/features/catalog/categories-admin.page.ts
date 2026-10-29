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
import { CategoryDto } from '@megalujos/models';
import {
  MegaButtonComponent,
  MegaConfirmService,
  MegaInputComponent,
  MegaModalComponent,
  MegaSelectComponent,
  MegaTableColumn,
  MegaTableCellContext,
  MegaTableComponent,
  MegaTextareaComponent,
  MegaToastService,
} from '@megalujos/ui';
import { errorMessage } from '../../core/util/ui';

function flattenCategories(categories: CategoryDto[], depth = 0): Array<CategoryDto & { depth: number }> {
  return categories.flatMap((category) => [
    { ...category, depth },
    ...flattenCategories(category.children ?? [], depth + 1),
  ]);
}

/** Árbol de categorías con alta/edición y activación. */
@Component({
  selector: 'app-categories-admin-page',
  imports: [
    FormsModule,
    MegaInputComponent,
    MegaSelectComponent,
    MegaTextareaComponent,
    MegaModalComponent,
    MegaTableComponent,
    MegaButtonComponent,
  ],
  templateUrl: './categories-admin.page.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CategoriesAdminPage {
  private readonly catalog = inject(CatalogAdminService);
  private readonly toast = inject(MegaToastService);
  private readonly confirm = inject(MegaConfirmService);
  private readonly destroyRef = inject(DestroyRef);

  readonly rows = signal<Array<CategoryDto & { depth: number }>>([]);
  readonly loading = signal(true);

  readonly modalOpen = signal(false);
  readonly editingId = signal<string | null>(null);
  readonly name = signal('');
  readonly slug = signal('');
  readonly description = signal('');
  readonly sortOrder = signal('0');
  readonly parentId = signal('');
  readonly saving = signal(false);

  readonly rowKey = (row: CategoryDto & { depth: number }): string => row.id;

  private readonly actionsCell = viewChild<
    TemplateRef<MegaTableCellContext<CategoryDto & { depth: number }>>
  >('actionsCell');

  readonly columns = computed<
    MegaTableColumn<CategoryDto & { depth: number }>[]
  >(() => [
    { key: 'name', header: 'Categoría' },
    { key: 'slug', header: 'Slug', width: '14rem' },
    { key: 'sortOrder', header: 'Orden', align: 'end', width: '6rem' },
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
      .listCategories({ size: 500 })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (page) => {
          this.rows.set(flattenCategories(page.items ?? []));
          this.loading.set(false);
        },
        error: (err: unknown) => {
          this.loading.set(false);
          this.toast.error(errorMessage(err), { title: 'No se pudieron cargar las categorías' });
        },
      });
  }

  openCreate(): void {
    this.editingId.set(null);
    this.name.set('');
    this.slug.set('');
    this.description.set('');
    this.sortOrder.set('0');
    this.parentId.set('');
    this.modalOpen.set(true);
  }

  openEdit(category: CategoryDto & { depth: number }): void {
    this.editingId.set(category.id);
    this.name.set(category.name);
    this.slug.set(category.slug);
    this.description.set(category.description ?? '');
    this.sortOrder.set(String(category.sortOrder ?? 0));
    this.parentId.set(category.parentId ?? '');
    this.modalOpen.set(true);
  }

  save(): void {
    const name = this.name().trim();
    if (!name) {
      this.toast.warning('El nombre es obligatorio.');
      return;
    }
    const request = {
      name,
      slug: this.slug().trim() || undefined,
      description: this.description().trim() || null,
      sortOrder: Number(this.sortOrder()) || 0,
      parentId: this.parentId() || null,
    };
    this.saving.set(true);
    const operation$ = this.editingId()
      ? this.catalog.updateCategory(this.editingId() as string, request)
      : this.catalog.createCategory(request);
    operation$.pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: () => {
        this.saving.set(false);
        this.modalOpen.set(false);
        this.toast.success(`Categoría ${this.editingId() ? 'actualizada' : 'creada'}.`);
        this.load();
      },
      error: (err: unknown) => {
        this.saving.set(false);
        this.toast.error(errorMessage(err), { title: 'No se pudo guardar la categoría' });
      },
    });
  }

  async toggleActive(category: CategoryDto): Promise<void> {
    const next = !category.active;
    const ok = await this.confirm.open({
      title: next ? 'Activar categoría' : 'Desactivar categoría',
      message: `La categoría "${category.name}" quedará ${next ? 'visible' : 'oculta'}.`,
      confirmText: next ? 'Activar' : 'Desactivar',
      danger: !next,
    });
    if (!ok) {
      return;
    }
    this.catalog
      .setCategoryActive(category.id, next)
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

  parentOptions(): Array<{ value: string; label: string }> {
    return [
      { value: '', label: 'Sin padre (raíz)' },
      ...this.rows()
        .filter((row) => row.id !== this.editingId())
        .map((row) => ({ value: row.id, label: row.name })),
    ];
  }
}
