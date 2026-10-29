import { Component, forwardRef, input, signal } from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';
import { megaUid } from '../shared/mega-format';

/**
 * Selector de fecha MEGALUJOS (input date nativo estilizado).
 * ControlValueAccessor: emite/recibe ISO date string `yyyy-MM-dd`.
 */
@Component({
  selector: 'mega-date-picker',
  standalone: true,
  styleUrl: './mega-date-picker.component.scss',
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => MegaDatePickerComponent),
      multi: true,
    },
  ],
  template: `
    @if (label()) {
      <label class="mega-field__label" [for]="id()">{{ label() }}</label>
    }
    <div class="mega-control mega-control--date" [class.mega-control--invalid]="isInvalid()">
      <input
        class="mega-control__field mega-date-picker__field"
        [attr.id]="id()"
        type="date"
        [attr.min]="min() || null"
        [attr.max]="max() || null"
        [attr.aria-label]="ariaLabel() || label() || 'Fecha'"
        [attr.aria-describedby]="describedBy()"
        [attr.aria-invalid]="isInvalid() ? 'true' : null"
        [disabled]="disabled()"
        [value]="value()"
        (input)="onInput($event)"
        (blur)="onTouched()"
      />
      <svg class="mega-date-picker__icon" viewBox="0 0 16 16" aria-hidden="true" focusable="false">
        <rect x="1.5" y="3" width="13" height="11.5" rx="2" fill="none" stroke="currentColor" stroke-width="1.4" />
        <path d="M1.5 6.5h13M5 1.5v3M11 1.5v3" fill="none" stroke="currentColor" stroke-width="1.4" stroke-linecap="round" />
      </svg>
    </div>
    @if (isInvalid() && error()) {
      <p class="mega-field__hint mega-field__hint--error" [attr.id]="errorId()">{{ error() }}</p>
    } @else if (hint()) {
      <p class="mega-field__hint" [attr.id]="hintId()">{{ hint() }}</p>
    }
  `,
})
export class MegaDatePickerComponent implements ControlValueAccessor {
  /** Etiqueta visible. */
  label = input<string | null>(null);

  /** Texto auxiliar. */
  hint = input<string | null>(null);

  /** Mensaje de error (estado inválido). */
  error = input<string | null>(null);

  /** Fecha mínima admitida (ISO date). */
  min = input<string | null>(null);

  /** Fecha máxima admitida (ISO date). */
  max = input<string | null>(null);

  /** aria-label adicional. */
  ariaLabel = input<string | null>(null);

  /** Valor ISO date string `yyyy-MM-dd`. */
  value = signal('');

  /** Estado deshabilitado. */
  disabled = signal(false);

  readonly id = signal(megaUid('date'));

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

  onInput(event: Event): void {
    const next = (event.target as HTMLInputElement).value;
    this.value.set(next);
    this.onChange(next);
  }
}
