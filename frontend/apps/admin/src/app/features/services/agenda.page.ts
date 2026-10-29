import { ChangeDetectionStrategy, Component, DestroyRef, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ServicesService } from '@megalujos/api-client';
import { AgendaDayDto } from '@megalujos/models';
import {
  MegaButtonComponent,
  MegaEmptyStateComponent,
  MegaStatusBadgeComponent,
  MegaToastService,
  formatUsd,
} from '@megalujos/ui';
import { errorMessage } from '../../core/util/ui';
import { SERVICE_ORDER_STATUS_LABELS, SERVICE_ORDER_STATUS_TONES } from './service-orders.page';

/** Agenda diaria de órdenes programadas. */
@Component({
  selector: 'app-agenda-page',
  imports: [
    RouterLink,
    MegaEmptyStateComponent,
    MegaStatusBadgeComponent,
    MegaButtonComponent,
  ],
  templateUrl: './agenda.page.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AgendaPage {
  private readonly services = inject(ServicesService);
  private readonly toast = inject(MegaToastService);
  private readonly destroyRef = inject(DestroyRef);

  readonly days = signal<AgendaDayDto[]>([]);
  readonly loading = signal(true);
  readonly error = signal<string | null>(null);

  readonly statusLabels = SERVICE_ORDER_STATUS_LABELS;
  readonly statusTones = SERVICE_ORDER_STATUS_TONES;

  constructor() {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.error.set(null);
    this.services
      .agenda()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (days) => {
          this.days.set(days ?? []);
          this.loading.set(false);
        },
        error: (err: unknown) => {
          const message = errorMessage(err);
          this.error.set(message);
          this.loading.set(false);
          this.toast.error(message, { title: 'No se pudo cargar la agenda' });
        },
      });
  }

  money(value: string | number | null | undefined): string {
    return formatUsd(value ?? null);
  }

  formatDate(iso: string): string {
    const date = new Date(`${iso}T12:00:00`);
    if (Number.isNaN(date.getTime())) {
      return iso;
    }
    return new Intl.DateTimeFormat('es-EC', {
      timeZone: 'America/Guayaquil',
      weekday: 'long',
      day: 'numeric',
      month: 'long',
    }).format(date);
  }
}
