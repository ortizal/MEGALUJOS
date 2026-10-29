import { ChangeDetectionStrategy, Component, DestroyRef, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { InvoicingService } from '@megalujos/api-client';
import { IssuerDto } from '@megalujos/models';
import {
  MegaButtonComponent,
  MegaEmptyStateComponent,
  MegaInputComponent,
  MegaSelectComponent,
  MegaTextareaComponent,
  MegaToastService,
} from '@megalujos/ui';
import { errorMessage } from '../../core/util/ui';

const ENVIRONMENT_OPTIONS = [
  { value: 'TEST', label: 'Pruebas (SRI 1)' },
  { value: 'PRODUCTION', label: 'Producción (SRI 2)' },
];

/** Emisor tributario SRI (RUC, establecimiento, punto de emisión, ambiente). */
@Component({
  selector: 'app-issuers-page',
  imports: [
    FormsModule,
    MegaInputComponent,
    MegaSelectComponent,
    MegaTextareaComponent,
    MegaEmptyStateComponent,
    MegaButtonComponent,
  ],
  templateUrl: './issuers.page.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class IssuersPage {
  private readonly invoicing = inject(InvoicingService);
  private readonly toast = inject(MegaToastService);
  private readonly destroyRef = inject(DestroyRef);

  readonly loading = signal(true);
  readonly saving = signal(false);
  readonly issuer = signal<IssuerDto | null>(null);

  readonly taxId = signal('');
  readonly legalName = signal('');
  readonly tradeName = signal('');
  readonly establishmentCode = signal('');
  readonly emissionPointCode = signal('');
  readonly environment = signal('TEST');
  readonly mainAddress = signal('');

  readonly environmentOptions = ENVIRONMENT_OPTIONS;

  constructor() {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.invoicing
      .issuers()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (raw) => {
          const issuer = Array.isArray(raw) ? (raw[0] ?? null) : raw;
          this.issuer.set(issuer && issuer.id ? issuer : null);
          if (issuer) {
            this.taxId.set(issuer.taxId ?? '');
            this.legalName.set(issuer.legalName ?? '');
            this.tradeName.set(issuer.tradeName ?? '');
            this.establishmentCode.set(issuer.establishmentCode ?? '');
            this.emissionPointCode.set(issuer.emissionPointCode ?? '');
            this.environment.set(issuer.environment ?? 'TEST');
            this.mainAddress.set(issuer.mainAddress ?? '');
          }
          this.loading.set(false);
        },
        error: () => {
          this.loading.set(false);
        },
      });
  }

  save(): void {
    const taxId = this.taxId().trim();
    const legalName = this.legalName().trim();
    if (!taxId || !legalName) {
      this.toast.warning('RUC y razón social son obligatorios.');
      return;
    }
    if (!this.establishmentCode().trim() || !this.emissionPointCode().trim()) {
      this.toast.warning('Establecimiento y punto de emisión son obligatorios.');
      return;
    }
    this.saving.set(true);
    this.invoicing
      .updateIssuer({
        taxId,
        legalName,
        tradeName: this.tradeName().trim() || null,
        establishmentCode: this.establishmentCode().trim(),
        emissionPointCode: this.emissionPointCode().trim(),
        environment: this.environment(),
        mainAddress: this.mainAddress().trim() || null,
      })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (issuer) => {
          this.saving.set(false);
          this.issuer.set(issuer);
          this.toast.success('Emisor guardado.');
        },
        error: (err: unknown) => {
          this.saving.set(false);
          this.toast.error(errorMessage(err), { title: 'No se pudo guardar el emisor' });
        },
      });
  }
}
