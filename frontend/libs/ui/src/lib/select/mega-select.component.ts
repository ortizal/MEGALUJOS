import { Component, computed, forwardRef, input, signal } from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';
import { megaUid } from '../shared/mega-format';
import { MegaFieldState } from '../shared/mega-field-state';

/** Opción simple del select. */
export interface MegaSelectOption {
  value: string;
  label: string;
  disabled?: boolean;
}

/**
 * Selector MEGALUJOS (ControlValueAccessor) sobre Bootstrap 5.
 * Emite `form-select form-select-sm` + la clase de estado de Bootstrap
 * (`is-valid` / `is-invalid` / `is-warning`). Acepta `options` como arreglo
 * o `<option>` proyectados dentro del `<select>`.
 */
@Component({
  selector: 'mega-select',
  standalone: true,
  styleUrl: './mega-select.component.scss',
  providers: [
    { provide: NG_VALUE_ACCESSOR, useExisting: forwardRef(() => MegaSelectComponent), multi: true },
  ],
  template: `
    @if (label()) {
      <label class="form-label mega-field__label" [for]="id()">{{ label() }}</label>
    }
    <select
      class="form-select form-select-sm mega-field-control"
      [class.is-valid]="currentState() === 'valid'"
      [class.is-invalid]="showInvalid()"
      [class.is-warning]="currentState() === 'warning'"
      [attr.id]="id()"
      [attr.aria-label]="ariaLabel() || null"
      [attr.aria-describedby]="describedBy()"
      [attr.aria-invalid]="showInvalid() ? 'true' : null"
      [disabled]="disabled()"
      [value]="value()"
      (change)="onChangeValue($event)"
      (blur)="onTouched()"
    >
      @if (placeholder()) {
        <option value="" [disabled]="required()">{{ placeholder() }}</option>
      }
      @for (option of options(); track option.value) {
        <option [value]="option.value" [disabled]="option.disabled ?? false">
          {{ option.label }}
        </option>
      }
      <ng-content />
    </select>
    @if (showInvalid() && error()) {
      <div class="invalid-feedback d-block" [attr.id]="errorId()">{{ error() }}</div>
    } @else if (currentState() === 'valid' && validMessage()) {
      <div class="valid-feedback d-block" [attr.id]="hintId()">{{ validMessage() }}</div>
    } @else if (currentState() === 'warning' && warningMessage()) {
      <div class="mega-warning-feedback" [attr.id]="hintId()">{{ warningMessage() }}</div>
    } @else if (hint()) {
      <p class="mega-field__hint" [attr.id]="hintId()">{{ hint() }}</p>
    }
  `,
})
export class MegaSelectComponent implements ControlValueAccessor {
  /** Etiqueta visible. */
  label = input<string | null>(null);

  /** Texto auxiliar. */
  hint = input<string | null>(null);

  /** Mensaje de error (estado inválido). */
  error = input<string | null>(null);

  /** Estado explícito de validación (anula `error` si ambos se pasan). */
  state = input<MegaFieldState>(null);

  /** Mensaje mostrado cuando `state` es `valid`. */
  validMessage = input<string | null>(null);

  /** Mensaje mostrado cuando `state` es `warning`. */
  warningMessage = input<string | null>(null);

  /** Opciones del selector. */
  options = input<MegaSelectOption[]>([]);

  /** Placeholder (opción vacía). */
  placeholder = input<string | null>('Seleccione…');

  /** Campo obligatorio (el placeholder no puede quedar seleccionado). */
  required = input(false);

  /** aria-label adicional. */
  ariaLabel = input<string | null>(null);

  /** Valor seleccionado (string). */
  value = signal('');

  /** Estado deshabilitado. */
  disabled = signal(false);

  readonly id = signal(megaUid('select'));

  onChange: (value: string) => void = () => {};
  onTouched: () => void = () => {};

  /** Estado efectivo: `error` implícito se traduce a `invalid`. */
  readonly effectiveState = computed<MegaFieldState>(() => {
    const explicit = this.state();
    if (explicit) {
      return explicit;
    }
    return this.error() ? 'invalid' : null;
  });

  writeValue(value: string | null | undefined): void {
    this.value.set(value ?? '');
  }

  registerOnChange(fn: (value: string) => void): void {
    this.onChange = fn;
  }

  registerOnTouched(fn: () => void): void {
    this.onTouched = fn;
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled.set(isDisabled);
  }

  /** Estado efectivo del campo (resuelve `state` o `error`). */
  currentState(): MegaFieldState {
    return this.effectiveState();
  }

  showInvalid(): boolean {
    return this.effectiveState() === 'invalid';
  }

  hintId(): string {
    return `${this.id()}-hint`;
  }

  errorId(): string {
    return `${this.id()}-error`;
  }

  describedBy(): string | null {
    const st = this.effectiveState();
    if (st === 'invalid' && this.error()) {
      return this.errorId();
    }
    if ((st === 'valid' && this.validMessage()) || (st === 'warning' && this.warningMessage())) {
      return this.hintId();
    }
    return this.hint() ? this.hintId() : null;
  }

  onChangeValue(event: Event): void {
    const next = (event.target as HTMLSelectElement).value;
    this.value.set(next);
    this.onChange(next);
  }
}
