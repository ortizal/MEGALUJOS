import { Component, booleanAttribute, computed, input } from '@angular/core';

export type MegaButtonVariant =
  | 'primary'
  | 'secondary'
  | 'outline'
  | 'ghost'
  | 'danger'
  | 'success'
  | 'warning'
  | 'info';
export type MegaButtonSize = 'sm' | 'md';

/** Clases Bootstrap 5 por variante (paleta MEGALUJOS en primary/danger). */
const VARIANT_CLASS: Record<MegaButtonVariant, string> = {
  primary: 'btn-primary',
  secondary: 'btn-secondary',
  outline: 'btn-outline-secondary',
  ghost: 'mega-btn-ghost',
  danger: 'btn-danger',
  success: 'btn-success',
  warning: 'btn-warning',
  info: 'btn-info',
};

/**
 * Botón MEGALUJOS sobre Bootstrap 5.
 * Emite `btn btn-sm|btn-md` + la clase de variante de Bootstrap, de modo que
 * en todo el sistema los botones son sm y muestran los estados de Bootstrap
 * (primary/danger/success/warning/info/outline). La variante `primary` usa el
 * rojo de marca; `ghost` es un helper propio.
 * Incluye estado de carga con spinner y disabled accesible.
 */
@Component({
  selector: 'mega-button',
  standalone: true,
  styleUrl: './mega-button.component.scss',
  host: {
    '[class]': 'hostClasses()',
    '[attr.aria-busy]': `loading() ? 'true' : null`,
    '[attr.disabled]': 'isDisabled() ? "" : null',
    '[attr.type]': 'type()',
  },
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
  /** Variante visual (mapea a una clase de botón de Bootstrap 5). */
  variant = input<MegaButtonVariant>('primary');

  /** Tamaño del botón. Por defecto `sm` (estándar de todo el sistema). */
  size = input<MegaButtonSize>('sm');

  /** Estado de carga: deshabilita el botón y muestra spinner (aria-busy). */
  loading = input(false, { transform: booleanAttribute });

  /** Bloqueo explícito del botón. */
  disabled = input(false, { transform: booleanAttribute });

  /** Ancho completo del contenedor. */
  block = input(false, { transform: booleanAttribute });

  /** Tipo nativo del botón (submit por defecto dentro de formularios). */
  type = input<'button' | 'submit' | 'reset'>('button');

  /** Clases completas del host: base + Bootstrap + variante + modificadores. */
  readonly hostClasses = computed(() => {
    const sizeCls = this.size() === 'md' ? 'btn-md' : 'btn-sm';
    const blockCls = this.block() ? 'mega-button--block' : '';
    const variantCls = VARIANT_CLASS[this.variant()] ?? 'btn-primary';
    return ['mega-button', 'btn', sizeCls, variantCls, blockCls].filter(Boolean).join(' ');
  });

  /** true cuando el botón no admite interacción. */
  isDisabled(): boolean {
    return this.disabled() || this.loading();
  }
}
