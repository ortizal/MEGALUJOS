import { ChangeDetectionStrategy, Component, DestroyRef, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Observable } from 'rxjs';
import { CatalogAdminService } from '@megalujos/api-client';
import { VehicleBrand, VehicleModel, VehicleVersion } from '@megalujos/models';
import {
  MegaButtonComponent,
  MegaEmptyStateComponent,
  MegaInputComponent,
  MegaModalComponent,
  MegaSelectComponent,
  MegaToastService,
} from '@megalujos/ui';
import { errorMessage } from '../../core/util/ui';

type VehicleTab = 'brands' | 'models' | 'versions';

/** Catálogo de vehículos: marcas, modelos y versiones (años). */
@Component({
  selector: 'app-vehicles-admin-page',
  imports: [
    FormsModule,
    MegaInputComponent,
    MegaSelectComponent,
    MegaModalComponent,
    MegaEmptyStateComponent,
    MegaButtonComponent,
  ],
  templateUrl: './vehicles-admin.page.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class VehiclesAdminPage {
  private readonly catalog = inject(CatalogAdminService);
  private readonly toast = inject(MegaToastService);
  private readonly destroyRef = inject(DestroyRef);

  readonly tab = signal<VehicleTab>('brands');

  readonly brands = signal<VehicleBrand[]>([]);
  readonly models = signal<VehicleModel[]>([]);
  readonly versions = signal<VehicleVersion[]>([]);

  readonly brandFilter = signal('');
  readonly modelFilter = signal('');

  readonly modalOpen = signal(false);
  readonly saving = signal(false);
  readonly editingId = signal<string | null>(null);
  readonly modalTitle = signal('');

  readonly name = signal('');
  readonly parentBrandId = signal('');
  readonly parentModelId = signal('');
  readonly yearFrom = signal('');
  readonly yearTo = signal('');

  constructor() {
    this.loadBrands();
    this.loadModels();
    this.loadVersions();
  }

  loadBrands(): void {
    this.catalog
      .listVehicleBrands()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (brands) => this.brands.set(brands ?? []),
        error: (err: unknown) =>
          this.toast.error(errorMessage(err), { title: 'No se pudieron cargar las marcas' }),
      });
  }

  loadModels(): void {
    this.catalog
      .listVehicleModels(this.brandFilter() || undefined)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (models) => this.models.set(models ?? []),
        error: (err: unknown) =>
          this.toast.error(errorMessage(err), { title: 'No se pudieron cargar los modelos' }),
      });
  }

  loadVersions(): void {
    this.catalog
      .listVehicleVersions(this.modelFilter() || undefined)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (versions) => this.versions.set(versions ?? []),
        error: (err: unknown) =>
          this.toast.error(errorMessage(err), { title: 'No se pudieron cargar las versiones' }),
      });
  }

  brandOptions(): Array<{ value: string; label: string }> {
    return [
      { value: '', label: 'Todas las marcas' },
      ...this.brands().map((brand) => ({ value: brand.id, label: brand.name })),
    ];
  }

  modelOptions(): Array<{ value: string; label: string }> {
    return [
      { value: '', label: 'Todos los modelos' },
      ...this.models().map((model) => ({
        value: model.id,
        label: model.brandName ? `${model.brandName} ${model.name}` : model.name,
      })),
    ];
  }

  openCreate(tab: VehicleTab): void {
    this.editingId.set(null);
    this.name.set('');
    this.parentBrandId.set('');
    this.parentModelId.set('');
    this.yearFrom.set('');
    this.yearTo.set('');
    this.modalTitle.set(
      tab === 'brands' ? 'Nueva marca de vehículo' : tab === 'models' ? 'Nuevo modelo' : 'Nueva versión',
    );
    this.modalOpen.set(true);
  }

  openEdit(tab: VehicleTab, item: VehicleBrand | VehicleModel | VehicleVersion): void {
    this.editingId.set(item.id);
    this.name.set(item.name);
    if (tab === 'models') {
      this.parentBrandId.set((item as VehicleModel).brandId);
    }
    if (tab === 'versions') {
      const version = item as VehicleVersion;
      this.parentModelId.set(version.modelId);
      this.yearFrom.set(version.yearFrom ? String(version.yearFrom) : '');
      this.yearTo.set(version.yearTo ? String(version.yearTo) : '');
    }
    this.modalTitle.set(
      tab === 'brands' ? 'Editar marca' : tab === 'models' ? 'Editar modelo' : 'Editar versión',
    );
    this.modalOpen.set(true);
  }

  save(): void {
    const tab = this.tab();
    const name = this.name().trim();
    if (!name) {
      this.toast.warning('El nombre es obligatorio.');
      return;
    }
    this.saving.set(true);

    let operation$: Observable<VehicleBrand | VehicleModel | VehicleVersion>;
    if (tab === 'brands') {
      operation$ = this.editingId()
        ? this.catalog.updateVehicleBrand(this.editingId() as string, { name })
        : this.catalog.createVehicleBrand({ name });
    } else if (tab === 'models') {
      if (!this.parentBrandId()) {
        this.saving.set(false);
        this.toast.warning('Selecciona la marca del modelo.');
        return;
      }
      operation$ = this.editingId()
        ? this.catalog.updateVehicleModel(this.editingId() as string, {
            brandId: this.parentBrandId(),
            name,
          })
        : this.catalog.createVehicleModel({ brandId: this.parentBrandId(), name });
    } else {
      if (!this.parentModelId()) {
        this.saving.set(false);
        this.toast.warning('Selecciona el modelo de la versión.');
        return;
      }
      const body = {
        modelId: this.parentModelId(),
        name,
        yearFrom: this.yearFrom() ? Number(this.yearFrom()) : null,
        yearTo: this.yearTo() ? Number(this.yearTo()) : null,
      };
      operation$ = this.editingId()
        ? this.catalog.updateVehicleVersion(this.editingId() as string, body)
        : this.catalog.createVehicleVersion(body);
    }

    operation$.pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: () => {
        this.saving.set(false);
        this.modalOpen.set(false);
        this.toast.success('Guardado correctamente.');
        this.reloadCurrent();
      },
      error: (err: unknown) => {
        this.saving.set(false);
        this.toast.error(errorMessage(err), { title: 'No se pudo guardar' });
      },
    });
  }

  selectTab(tab: VehicleTab): void {
    this.tab.set(tab);
    if (tab === 'models') {
      this.loadModels();
    } else if (tab === 'versions') {
      this.loadVersions();
    }
  }

  private reloadCurrent(): void {
    const tab = this.tab();
    if (tab === 'brands') {
      this.loadBrands();
    } else if (tab === 'models') {
      this.loadModels();
    } else {
      this.loadVersions();
    }
  }
}
