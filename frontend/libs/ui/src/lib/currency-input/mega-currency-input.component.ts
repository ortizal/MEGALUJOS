import { Component, computed, forwardRef, input, signal } from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';
import { megaUid, toDecimalString } from '../shared/mega-format';
import { MegaFieldState } from '../shared/mega-field-state';

/**
 * Campo monetario USD (ControlValueAccessor) sobre Bootstrap 5.
 * Emite `form-control form-control-sm` + la clase de estado de Bootstrap.
 * Muestra formato 0.00 mientras se edita y EMITE decimal string (contrato
 * backend). Al salir del campo normaliza a dos decimales.
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
      <label class="form-label mega-field__label" [for]="id()">{{ label() }}</label>
    }
    <div class="input-group input-group-sm" [class.has-validation]="showInvalid()">
      <span class="input-group-text mega-currency-input__prefix" aria-hidden="true">$</span>
      <input
        class="form-control form-control-sm mega-field-control mega-currency-input__field"
        [class.is-valid]="currentState() === 'valid'"
        [class.is-invalid]="showInvalid()"
        [class.is-warning]="currentState() === 'warning'"
        [attr.id]="id()"
        type="text"
        inputmode="decimal"
        autocomplete="off"
        [attr.placeholder]="placeholder() || '0.00'"
        [attr.aria-label]="ariaLabel() || label() || 'Importe en dólares'"
        [attr.aria-describedby]="describedBy()"
        [attr.aria-invalid]="showInvalid() ? 'true' : null"
        [disabled]="disabled()"
        [value]="display()"
        (input)="onInput($event)"
        (blur)="onBlur()"
        (focus)="onFocus()"
      />
      <span class="input-group-text mega-currency-input__suffix" aria-hidden="true">
        {{ currencyLabel() }}
      </span>
      @if (showInvalid() && error()) {
        <div class="invalid-feedback" [attr.id]="errorId()">{{ error() }}</div>
      } @else if (currentState() === 'valid' && validMessage()) {
        <div class="valid-feedback" [attr.id]="hintId()">{{ validMessage() }}</div>
      } @else if (currentState() === 'warning' && warningMessage()) {
        <div class="mega-warning-feedback" [attr.id]="hintId()">{{ warningMessage() }}</div>
      }
    </div>
    @if (!showInvalid() && currentState() !== 'valid' && currentState() !== 'warning' && hint()) {
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

  /** Estado explícito de validación (anula `error` si ambos se pasan). */
  state = input<MegaFieldState>(null);

  /** Mensaje mostrado cuando `state` es `valid`. */
  validMessage = input<string | null>(null);

  /** Mensaje mostrado cuando `state` es `warning`. */
  warningMessage = input<string | null>(null);

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

  /** Estado efectivo: `error` implícito se traduce a `invalid`. */
  readonly effectiveState = computed<MegaFieldState>(() => {
    const explicit = this.state();
    if (explicit) {
      return explicit;
    }
    return this.error() ? 'invalid' : null;
  });

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
