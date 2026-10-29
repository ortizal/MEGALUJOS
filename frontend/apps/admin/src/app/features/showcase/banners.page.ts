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
import { MediaService, ShowcaseAdminService } from '@megalujos/api-client';
import { BannerDto } from '@megalujos/models';
import {
  MegaButtonComponent,
  MegaConfirmService,
  MegaDatePickerComponent,
  MegaInputComponent,
  MegaModalComponent,
  MegaStatusBadgeComponent,
  MegaTableColumn,
  MegaTableCellContext,
  MegaTableComponent,
  MegaToastService,
  MegaUploaderComponent,
} from '@megalujos/ui';
import { errorMessage, formatDateTime } from '../../core/util/ui';

/** Banners del home público con alta/edición y activación. */
@Component({
  selector: 'app-banners-page',
  imports: [
    FormsModule,
    MegaInputComponent,
    MegaDatePickerComponent,
    MegaModalComponent,
    MegaTableComponent,
    MegaStatusBadgeComponent,
    MegaUploaderComponent,
    MegaButtonComponent,
  ],
  templateUrl: './banners.page.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class BannersPage {
  private readonly showcase = inject(ShowcaseAdminService);
  private readonly media = inject(MediaService);
  private readonly toast = inject(MegaToastService);
  private readonly confirm = inject(MegaConfirmService);
  private readonly destroyRef = inject(DestroyRef);

  readonly rows = signal<BannerDto[]>([]);
  readonly loading = signal(true);

  readonly modalOpen = signal(false);
  readonly editingId = signal<string | null>(null);
  readonly title = signal('');
  readonly subtitle = signal('');
  readonly linkUrl = signal('');
  readonly sortOrder = signal('0');
  readonly startsAt = signal('');
  readonly endsAt = signal('');
  readonly mediaId = signal<string | null>(null);
  readonly uploading = signal(false);
  readonly saving = signal(false);

  readonly rowKey = (row: BannerDto): string => row.id;

  private readonly actionsCell = viewChild<TemplateRef<MegaTableCellContext<BannerDto>>>(
    'actionsCell',
  );

  readonly columns = computed<MegaTableColumn<BannerDto>[]>(() => [
    { key: 'title', header: 'Título' },
    { key: 'url', header: 'Imagen', width: '14rem' },
    { key: 'sortOrder', header: 'Orden', align: 'end', width: '6rem' },
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
    this.showcase
      .listBanners()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (banners) => {
          this.rows.set(
            (banners ?? []).map((banner) => ({
              ...banner,
              startsAt: formatDateTime(banner.startsAt),
              endsAt: formatDateTime(banner.endsAt),
            })),
          );
          this.loading.set(false);
        },
        error: (err: unknown) => {
          this.loading.set(false);
          this.toast.error(errorMessage(err), { title: 'No se pudieron cargar los banners' });
        },
      });
  }

  openCreate(): void {
    this.editingId.set(null);
    this.title.set('');
    this.subtitle.set('');
    this.linkUrl.set('');
    this.sortOrder.set('0');
    this.startsAt.set('');
    this.endsAt.set('');
    this.mediaId.set(null);
    this.modalOpen.set(true);
  }

  openEdit(banner: BannerDto): void {
    this.editingId.set(banner.id);
    this.title.set(banner.title ?? '');
    this.subtitle.set(banner.subtitle ?? '');
    this.linkUrl.set(banner.linkUrl ?? '');
    this.sortOrder.set(String(banner.sortOrder ?? 0));
    this.startsAt.set('');
    this.endsAt.set('');
    this.mediaId.set(banner.mediaId ?? null);
    this.modalOpen.set(true);
  }

  onFilesSelected(files: File[]): void {
    const file = files[0];
    if (!file) {
      return;
    }
    this.uploading.set(true);
    this.media
      .upload(file, true)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (media) => {
          this.uploading.set(false);
          this.mediaId.set(media.id);
          this.toast.success('Imagen lista. Guarda el banner para asociarla.');
        },
        error: (err: unknown) => {
          this.uploading.set(false);
          this.toast.error(errorMessage(err), { title: 'No se pudo subir la imagen' });
        },
      });
  }

  save(): void {
    if (!this.mediaId()) {
      this.toast.warning('Sube una imagen para el banner.');
      return;
    }
    const request = {
      title: this.title().trim() || null,
      subtitle: this.subtitle().trim() || null,
      mediaId: this.mediaId(),
      linkUrl: this.linkUrl().trim() || null,
      startsAt: this.startsAt() ? `${this.startsAt()}T00:00:00-05:00` : null,
      endsAt: this.endsAt() ? `${this.endsAt()}T23:59:59-05:00` : null,
      sortOrder: Number(this.sortOrder()) || 0,
    };
    this.saving.set(true);
    const operation$ = this.editingId()
      ? this.showcase.updateBanner(this.editingId() as string, request)
      : this.showcase.createBanner(request);
    operation$.pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: () => {
        this.saving.set(false);
        this.modalOpen.set(false);
        this.toast.success(`Banner ${this.editingId() ? 'actualizado' : 'creado'}.`);
        this.load();
      },
      error: (err: unknown) => {
        this.saving.set(false);
        this.toast.error(errorMessage(err), { title: 'No se pudo guardar el banner' });
      },
    });
  }

  async toggleActive(banner: BannerDto): Promise<void> {
    const next = !banner.active;
    const ok = await this.confirm.open({
      title: next ? 'Activar banner' : 'Desactivar banner',
      message: `El banner "${banner.title ?? 'sin título'}" quedará ${next ? 'visible' : 'oculto'}.`,
      confirmText: next ? 'Activar' : 'Desactivar',
      danger: !next,
    });
    if (!ok) {
      return;
    }
    this.showcase
      .setBannerActive(banner.id, next)
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
