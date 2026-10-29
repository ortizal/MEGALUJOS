import { Component, booleanAttribute, input } from '@angular/core';

export type MegaButtonVariant = 'primary' | 'secondary' | 'outline' | 'ghost' | 'danger';
export type MegaButtonSize = 'sm' | 'md';

/**
 * Botón MEGALUJOS: variantes primary (rojo), secondary, outline, ghost, danger;
 * tamaños sm/md; estado de carga con spinner y disabled accesible.
 */
@Component({
  selector: 'mega-button',
  standalone: true,
  styleUrl: './mega-button.component.scss',
  template: `
    <span class="mega-button__spinner" aria-hidden="true" [attr.hidden]="!loading()">
      <svg viewBox="0 0 24 24" fill="none" focusable="false">
        <circle cx="12" cy="12" r="9" stroke="currentColor" stroke-width="3" opacity="0.25" />
        <path
          d="M21 12a9 9 0 0 0-9-9"
          stroke="currentColor"
          stroke-width="3"
          stroke-linecap="round"
        />
      </svg>
    </span>
    <span class="mega-button__content">
      <ng-content />
    </span>
  `,
})
export class MegaButtonComponent {
  /** Variante visual. */
  variant = input<MegaButtonVariant>('primary');

  /** Tamaño del botón. */
  size = input<MegaButtonSize>('md');

  /** Estado de carga: deshabilita el botón y muestra spinner (aria-busy). */
  loading = input(false, { transform: booleanAttribute });

  /** Bloqueo explícito del botón. */
  disabled = input(false, { transform: booleanAttribute });

  /** Ancho completo del contenedor. */
  block = input(false, { transform: booleanAttribute });

  /** Tipo nativo del botón. */
  type = input<'button' | 'submit' | 'reset'>('button');

  /** true cuando el botón no admite interacción. */
  isDisabled(): boolean {
    return this.disabled() || this.loading();
  }
}
