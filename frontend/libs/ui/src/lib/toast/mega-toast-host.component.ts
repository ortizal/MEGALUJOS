import { Component, computed, inject } from '@angular/core';
import { MegaToastService, MegaToastViewModel } from './mega-toast.service';

/**
 * Host de toasts: lista fija en la esquina superior derecha, con autocierre
 * por tipo y cierre manual accesible. Se monta una sola vez con
 * `<mega-toast-host />` (lo gestiona `MegaToastService`).
 */
@Component({
  selector: 'mega-toast-host',
  standalone: true,
  styleUrl: './mega-toast.component.scss',
  template: `
    <div class="mega-toast__list" role="region" aria-label="Notificaciones" aria-live="polite">
      @for (toast of toasts(); track toast.id) {
        <div
          class="mega-toast"
          [class.mega-toast--success]="toast.tone === 'success'"
          [class.mega-toast--error]="toast.tone === 'error'"
          [class.mega-toast--info]="toast.tone === 'info'"
          [class.mega-toast--warning]="toast.tone === 'warning'"
          [attr.role]="toast.tone === 'error' ? 'alert' : 'status'"
        >
          <span class="mega-toast__icon" aria-hidden="true">{{ iconFor(toast) }}</span>
          <div class="mega-toast__body">
            @if (toast.title) {
              <p class="mega-toast__title">{{ toast.title }}</p>
            }
            <p class="mega-toast__message">{{ toast.message }}</p>
          </div>
          <button
            type="button"
            class="mega-toast__close"
            [attr.aria-label]="'Cerrar notificación'"
            (click)="service.dismiss(toast.id)"
          >
            ✕
          </button>
        </div>
      }
    </div>
  `,
})
export class MegaToastHostComponent {
  /** Servicio de toasts (señales de los toasts activos). */
  readonly service = inject(MegaToastService);

  /** Toasts visibles. */
  readonly toasts = computed(() => this.service.toasts());

  iconFor(toast: MegaToastViewModel): string {
    switch (toast.tone) {
      case 'success':
        return '✓';
      case 'error':
        return '✕';
      case 'warning':
        return '⚠';
      default:
        return 'ℹ';
    }
  }
}
