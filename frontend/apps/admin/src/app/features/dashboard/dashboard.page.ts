import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ReportingService } from '@megalujos/api-client';
import { AuthService } from '@megalujos/auth';
import { DashboardStatsDto } from '@megalujos/models';
import {
  MegaEmptyStateComponent,
  MegaSkeletonComponent,
  MegaToastService,
} from '@megalujos/ui';
import { errorMessage, formatNumber } from '../../core/util/ui';

interface QuickLink {
  label: string;
  icon: string;
  path: string;
  permissions: string[];
}

/** Resumen operativo del día: ventas, caja, stock y accesos rápidos. */
@Component({
  selector: 'app-dashboard-page',
  imports: [RouterLink, MegaSkeletonComponent, MegaEmptyStateComponent],
  templateUrl: './dashboard.page.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DashboardPage {
  private readonly reporting = inject(ReportingService);
  private readonly toast = inject(MegaToastService);
  readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  readonly loading = signal(true);
  readonly error = signal<string | null>(null);
  readonly stats = signal<DashboardStatsDto | null>(null);

  readonly quickLinks: QuickLink[] = [
    { label: 'Punto de venta', icon: '🛒', path: '/pos', permissions: ['sales:create'] },
    { label: 'Productos', icon: '📦', path: '/catalogo/productos', permissions: ['catalog:read'] },
    { label: 'Facturación', icon: '📄', path: '/facturacion/documentos', permissions: ['invoicing:read'] },
    { label: 'Reportes', icon: '📈', path: '/reportes', permissions: ['reports:view'] },
  ];

  constructor() {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.error.set(null);
    this.reporting
      .dashboard()
      .pipe(takeUntilDestroyed())
      .subscribe({
        next: (stats) => {
          this.stats.set(stats);
          this.loading.set(false);
        },
        error: (err: unknown) => {
          const message = errorMessage(err);
          this.error.set(message);
          this.loading.set(false);
          this.toast.error(message, { title: 'No se pudo cargar el dashboard' });
        },
      });
  }

  visibleQuickLinks(): QuickLink[] {
    if (this.auth.hasRole('ADMIN')) {
      return this.quickLinks;
    }
    return this.quickLinks.filter((link) => this.auth.hasAnyPermission(link.permissions));
  }

  go(path: string): void {
    void this.router.navigate([path]);
  }

  money(value: string | number | null | undefined): string {
    if (value === null || value === undefined || value === '') {
      return '$0.00';
    }
    const numeric = Number(value);
    return Number.isNaN(numeric) ? '$0.00' : `$${formatNumber(numeric)}`;
  }

  count(value: number | null | undefined): string {
    return formatNumber(value ?? 0, 0);
  }
}
