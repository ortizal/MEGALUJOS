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
import { ShowcaseAdminService } from '@megalujos/api-client';
import { PromotionDto } from '@megalujos/models';
import {
  MegaButtonComponent,
  MegaDatePickerComponent,
  MegaInputComponent,
  MegaModalComponent,
  MegaSelectComponent,
  MegaStatusBadgeComponent,
  MegaTableColumn,
  MegaTableCellContext,
  MegaTableComponent,
  MegaTextareaComponent,
  MegaToastService,
  formatUsd,
} from '@megalujos/ui';
import { errorMessage, formatDateTime } from '../../core/util/ui';

const PROMO_TYPE_OPTIONS = [
  { value: 'PERCENTAGE', label: 'Porcentaje (%)' },
  { value: 'FIXED_AMOUNT', label: 'Monto fijo (USD)' },
];

/** Promociones de la vitrina con alta/edición. */
@Component({
  selector: 'app-promotions-page',
  imports: [
    FormsModule,
    MegaInputComponent,
    MegaSelectComponent,
    MegaDatePickerComponent,
    MegaTextareaComponent,
    MegaModalComponent,
    MegaTableComponent,
    MegaStatusBadgeComponent,
    MegaButtonComponent,
  ],
  templateUrl: './promotions.page.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PromotionsPage {
  private readonly showcase = inject(ShowcaseAdminService);
  private readonly toast = inject(MegaToastService);
  private readonly destroyRef = inject(DestroyRef);

  readonly rows = signal<PromotionDto[]>([]);
  readonly loading = signal(true);

  readonly modalOpen = signal(false);
  readonly editingId = signal<string | null>(null);
  readonly name = signal('');
  readonly description = signal('');
  readonly promoType = signal('PERCENTAGE');
  readonly value = signal('0');
  readonly startsAt = signal('');
  readonly endsAt = signal('');
  readonly active = signal(true);
  readonly saving = signal(false);

  readonly promoTypeOptions = PROMO_TYPE_OPTIONS;

  readonly rowKey = (row: PromotionDto): string => row.id;

  private readonly statusCell = viewChild<TemplateRef<MegaTableCellContext<PromotionDto>>>(
    'statusCell',
  );
  private readonly actionsCell = viewChild<TemplateRef<MegaTableCellContext<PromotionDto>>>(
    'actionsCell',
  );

  readonly columns = computed<MegaTableColumn<PromotionDto>[]>(() => [
    { key: 'name', header: 'Promoción' },
    { key: 'promoType', header: 'Tipo', width: '11rem' },
    { key: 'value', header: 'Valor', align: 'end', width: '8rem' },
    { key: 'active', header: 'Activa', width: '8rem', cell: this.statusCell() ?? undefined },
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
    this.showcase
      .promotions()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (promotions) => {
          this.rows.set(
            (promotions ?? []).map((promo) => ({
              ...promo,
              startsAt: formatDateTime(promo.startsAt),
              endsAt: formatDateTime(promo.endsAt),
            })),
          );
          this.loading.set(false);
        },
        error: (err: unknown) => {
          this.loading.set(false);
          this.toast.error(errorMessage(err), { title: 'No se pudieron cargar las promociones' });
        },
      });
  }

  openCreate(): void {
    this.editingId.set(null);
    this.name.set('');
    this.description.set('');
    this.promoType.set('PERCENTAGE');
    this.value.set('0');
    this.startsAt.set('');
    this.endsAt.set('');
    this.active.set(true);
    this.modalOpen.set(true);
  }

  openEdit(promotion: PromotionDto): void {
    this.editingId.set(promotion.id);
    this.name.set(promotion.name);
    this.description.set(promotion.description ?? '');
    this.promoType.set(promotion.promoType);
    this.value.set(String(promotion.value ?? '0'));
    this.startsAt.set('');
    this.endsAt.set('');
    this.active.set(promotion.active ?? true);
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
      description: this.description().trim() || null,
      promoType: this.promoType(),
      value: this.value(),
      startsAt: this.startsAt() ? `${this.startsAt()}T00:00:00-05:00` : null,
      endsAt: this.endsAt() ? `${this.endsAt()}T23:59:59-05:00` : null,
      active: this.active(),
    };
    this.saving.set(true);
    const operation$ = this.editingId()
      ? this.showcase.updatePromotion(this.editingId() as string, request)
      : this.showcase.createPromotion(request);
    operation$.pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: () => {
        this.saving.set(false);
        this.modalOpen.set(false);
        this.toast.success(`Promoción ${this.editingId() ? 'actualizada' : 'creada'}.`);
        this.load();
      },
      error: (err: unknown) => {
        this.saving.set(false);
        this.toast.error(errorMessage(err), { title: 'No se pudo guardar la promoción' });
      },
    });
  }

  promoValue(promotion: PromotionDto): string {
    return promotion.promoType === 'PERCENTAGE'
      ? `${promotion.value}%`
      : formatUsd(promotion.value);
  }
}
