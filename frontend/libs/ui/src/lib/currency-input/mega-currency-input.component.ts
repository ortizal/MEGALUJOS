import { Component, forwardRef, input, signal } from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';
import { megaUid, toDecimalString } from '../shared/mega-format';

/**
 * Campo monetario USD (ControlValueAccessor).
 * Muestra formato 0.00 mientras se edita y EMITE decimal string (contrato backend).
 * Al salir del campo normaliza a dos decimales.
 */
@Component({
  selector: 'mega-currency-input',
  standalone: true,
  styleUrl: './mega-currency-input.component.scss',
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => MegaCurrencyInputComponent),
      multi: true,
    },
  ],
  template: `
    @if (label()) {
      <label class="mega-field__label" [for]="id()">{{ label() }}</label>
    }
    <div class="mega-control mega-control--currency" [class.mega-control--invalid]="isInvalid()">
      <span class="mega-currency-input__prefix" aria-hidden="true">$</span>
      <input
        class="mega-control__field mega-currency-input__field"
        [attr.id]="id()"
        type="text"
        inputmode="decimal"
        autocomplete="off"
        [attr.placeholder]="placeholder() || '0.00'"
        [attr.aria-label]="ariaLabel() || label() || 'Importe en dólares'"
        [attr.aria-describedby]="describedBy()"
        [attr.aria-invalid]="isInvalid() ? 'true' : null"
        [disabled]="disabled()"
        [value]="display()"
        (input)="onInput($event)"
        (blur)="onBlur()"
        (focus)="onFocus()"
      />
      <span class="mega-currency-input__suffix" aria-hidden="true">{{ currencyLabel() }}</span>
    </div>
    @if (isInvalid() && error()) {
      <p class="mega-field__hint mega-field__hint--error" [attr.id]="errorId()">{{ error() }}</p>
    } @else if (hint()) {
      <p class="mega-field__hint" [attr.id]="hintId()">{{ hint() }}</p>
    }
  `,
})
export class MegaCurrencyInputComponent implements ControlValueAccessor {
  /** Etiqueta visible. */
  label = input<string | null>(null);

  /** Texto auxiliar. */
  hint = input<string | null>(null);

  /** Mensaje de error (estado inválido). */
  error = input<string | null>(null);

  /** Placeholder del campo. */
  placeholder = input('0.00');

  /** Sufijo de moneda visible (por defecto USD). */
  currencyLabel = input('USD');

  /** aria-label adicional. */
  ariaLabel = input<string | null>(null);

  /** Valor decimal string (lo que emite el control). */
  value = signal<string>('');

  /** Texto visible mientras el campo tiene foco. */
  display = signal<string>('');

  /** Estado deshabilitado. */
  disabled = signal(false);

  private focused = false;

  readonly id = signal(megaUid('currency'));

  onChange: (value: string) => void = () => {};
  onTouched: () => void = () => {};

  writeValue(value: string | number | null | undefined): void {
    const normalized = this.normalize(value);
    this.value.set(normalized);
    if (!this.focused) {
      this.display.set(normalized);
    }
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
    let next = (event.target as HTMLInputElement).value;
    // Solo dígitos, punto y coma decimal
    next = next.replace(/[^\d.,]/g, '');
    this.display.set(next);
    const decimal = toDecimalString(next);
    this.value.set(decimal ?? '');
    this.onChange(decimal ?? '');
  }

  onFocus(): void {
    this.focused = true;
    // Muestra el valor "crudo" para facilitar la edición
    this.display.set(this.value());
  }

  onBlur(): void {
    this.focused = false;
    const normalized = this.normalize(this.value());
    this.value.set(normalized);
    this.display.set(normalized);
    this.onChange(normalized);
    this.onTouched();
  }

  private normalize(value: string | number | null | undefined): string {
    return toDecimalString(value) ?? '';
  }
}
