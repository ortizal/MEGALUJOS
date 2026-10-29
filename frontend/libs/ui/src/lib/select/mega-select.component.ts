import { Component, forwardRef, input, signal } from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';
import { megaUid } from '../shared/mega-format';

/** Opción simple del select. */
export interface MegaSelectOption {
  value: string;
  label: string;
  disabled?: boolean;
}

/**
 * Selector MEGALUJOS (ControlValueAccessor).
 * Acepta `options` como arreglo o `<option>` proyectados dentro del `<select>`.
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
      <label class="mega-field__label" [for]="id()">{{ label() }}</label>
    }
    <div class="mega-control" [class.mega-control--invalid]="isInvalid()">
      <select
        class="mega-select__control"
        [attr.id]="id()"
        [attr.aria-label]="ariaLabel() || null"
        [attr.aria-describedby]="describedBy()"
        [attr.aria-invalid]="isInvalid() ? 'true' : null"
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
      <svg class="mega-select__chevron" viewBox="0 0 16 16" aria-hidden="true" focusable="false">
        <path
          d="M3 6l5 5 5-5"
          fill="none"
          stroke="currentColor"
          stroke-width="1.8"
          stroke-linecap="round"
          stroke-linejoin="round"
        />
      </svg>
    </div>
    @if (isInvalid() && error()) {
      <p class="mega-field__hint mega-field__hint--error" [attr.id]="errorId()">{{ error() }}</p>
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

  isInvalid(): boolean {
    return !!this.error();
  }

  hintId(): string {
    return `${this.id()}-hint`;
  }

  errorId(): string {
    return `${this.id()}-error`;
  }

  describedBy(): string | null {
    if (this.isInvalid() && this.error()) {
      return this.errorId();
    }
    return this.hint() ? this.hintId() : null;
  }

  onChangeValue(event: Event): void {
    const next = (event.target as HTMLSelectElement).value;
    this.value.set(next);
    this.onChange(next);
  }
}
