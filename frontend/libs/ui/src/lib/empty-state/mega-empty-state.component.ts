import { Component, input, output } from '@angular/core';

/**
 * Estado vacío ilustrado: icono emoji, título, mensaje y acción opcional.
 * Obligatorio en toda lista/tabla sin resultados.
 */
@Component({
  selector: 'mega-empty-state',
  standalone: true,
  styleUrl: './mega-empty-state.component.scss',
  template: `
    <div class="mega-empty-state">
      <span class="mega-empty-state__icon" aria-hidden="true">{{ icon() }}</span>
      <h3 class="mega-empty-state__title">{{ title() }}</h3>
      @if (message()) {
        <p class="mega-empty-state__message">{{ message() }}</p>
      }
      @if (actionLabel()) {
        <button type="button" class="mega-empty-state__action" (click)="action.emit()">
          {{ actionLabel() }}
        </button>
      }
      <ng-content />
    </div>
  `,
})
export class MegaEmptyStateComponent {
  /** Icono emoji (p. ej. '🔍', '📦', '🛠️'). */
  icon = input('📦');

  /** Título del estado. */
  title = input('Sin resultados');

  /** Mensaje explicativo accionable. */
  message = input('Intente con otros filtros o registre el primer elemento.');

  /** Etiqueta de la acción opcional (si se define se muestra el botón). */
  actionLabel = input<string | null>(null);

  /** Emite al pulsar la acción. */
  action = output<void>();
}
