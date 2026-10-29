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
import { CatalogAdminService, ShowcaseAdminService } from '@megalujos/api-client';
import { ShowcaseDto } from '@megalujos/models';
import {
  MegaButtonComponent,
  MegaInputComponent,
  MegaModalComponent,
  MegaStatusBadgeComponent,
  MegaTableColumn,
  MegaTableCellContext,
  MegaTableComponent,
  MegaToastService,
} from '@megalujos/ui';
import { errorMessage } from '../../core/util/ui';

/** Showcases (carruseles) del home: edición y productos destacados. */
@Component({
  selector: 'app-showcases-page',
  imports: [
    FormsModule,
    MegaInputComponent,
    MegaModalComponent,
    MegaTableComponent,
    MegaStatusBadgeComponent,
    MegaButtonComponent,
  ],
  templateUrl: './showcases.page.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ShowcasesPage {
  private readonly showcase = inject(ShowcaseAdminService);
  private readonly catalog = inject(CatalogAdminService);
  private readonly toast = inject(MegaToastService);
  private readonly destroyRef = inject(DestroyRef);

  readonly rows = signal<ShowcaseDto[]>([]);
  readonly loading = signal(true);

  readonly modalOpen = signal(false);
  readonly editingCode = signal<string | null>(null);
  readonly code = signal('');
  readonly title = signal('');
  readonly description = signal('');
  readonly autoplaySeconds = signal('6');
  readonly active = signal(true);
  readonly saving = signal(false);

  readonly productIds = signal<string[]>([]);
  readonly productSearch = signal('');
  readonly productResults = signal<Array<{ variantId: string; label: string }>>([]);
  readonly searching = signal(false);
  readonly replacing = signal(false);

  readonly rowKey = (row: ShowcaseDto): string => row.id;

  private readonly actionsCell = viewChild<TemplateRef<MegaTableCellContext<ShowcaseDto>>>(
    'actionsCell',
  );

  readonly columns = computed<MegaTableColumn<ShowcaseDto>[]>(() => [
    { key: 'code', header: 'Código', width: '10rem' },
    { key: 'title', header: 'Título' },
    { key: 'autoplaySeconds', header: 'Autoplay (s)', align: 'end', width: '10rem' },
    { key: 'active', header: 'Activo', width: '7rem' },
    {
      header: 'Acciones',
      align: 'end',
      width: '20rem',
      cell: this.actionsCell() ?? undefined,
    },
  ]);

  constructor() {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.showcase
      .listShowcases()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (showcases) => {
          this.rows.set(showcases ?? []);
          this.loading.set(false);
        },
        error: (err: unknown) => {
          this.loading.set(false);
          this.toast.error(errorMessage(err), { title: 'No se pudieron cargar los showcases' });
        },
      });
  }

  openEdit(showcase: ShowcaseDto): void {
    this.editingCode.set(showcase.code);
    this.code.set(showcase.code);
    this.title.set(showcase.title ?? '');
    this.description.set(showcase.description ?? '');
    this.autoplaySeconds.set(String(showcase.autoplaySeconds ?? 6));
    this.active.set(showcase.active ?? true);
    this.productIds.set(showcase.productIds ?? []);
    this.productResults.set([]);
    this.productSearch.set('');
    this.modalOpen.set(true);
  }

  save(): void {
    if (!this.code().trim()) {
      this.toast.warning('El código es obligatorio.');
      return;
    }
    this.saving.set(true);
    this.showcase
      .updateShowcase({
        code: this.code().trim(),
        title: this.title().trim() || null,
        description: this.description().trim() || null,
        autoplaySeconds: Number(this.autoplaySeconds()) || 6,
        active: this.active(),
      })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.saving.set(false);
          this.toast.success('Showcase guardado.');
          this.load();
        },
        error: (err: unknown) => {
          this.saving.set(false);
          this.toast.error(errorMessage(err), { title: 'No se pudo guardar el showcase' });
        },
      });
  }

  searchProducts(): void {
    const query = this.productSearch().trim();
    if (!query) {
      return;
    }
    this.searching.set(true);
    this.catalog
      .listProducts({ q: query, size: 8, published: true })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (page) => {
          const variants: Array<{ variantId: string; label: string }> = [];
          for (const product of page.items ?? []) {
            for (const variant of product.variants ?? []) {
              if (variant.id) {
                variants.push({ variantId: variant.id, label: `${product.name} — ${variant.sku}` });
              }
            }
          }
          this.productResults.set(variants.slice(0, 12));
          this.searching.set(false);
        },
        error: (err: unknown) => {
          this.searching.set(false);
          this.toast.error(errorMessage(err), { title: 'No se pudo buscar productos' });
        },
      });
  }

  addProduct(variant: { variantId: string }): void {
    this.productIds.update((ids) =>
      ids.includes(variant.variantId) ? ids : [...ids, variant.variantId],
    );
  }

  removeProduct(variantId: string): void {
    this.productIds.update((ids) => ids.filter((id) => id !== variantId));
  }

  saveProducts(): void {
    const showcaseCode = this.editingCode() ?? this.code().trim();
    if (!showcaseCode) {
      return;
    }
    this.replacing.set(true);
    this.showcase
      .setProducts(showcaseCode, this.productIds())
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.replacing.set(false);
          this.toast.success('Productos del showcase actualizados.');
          this.load();
        },
        error: (err: unknown) => {
          this.replacing.set(false);
          this.toast.error(errorMessage(err), { title: 'No se pudieron actualizar los productos' });
        },
      });
  }
}
