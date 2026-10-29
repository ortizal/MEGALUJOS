import { Component, forwardRef, input, signal } from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';
import { megaUid } from '../shared/mega-format';

/**
 * Campo de texto MEGALUJOS (ControlValueAccessor).
 * label + hint + error + prefix/suffix, aria-describedby y aria-invalid correctos.
 */
@Component({
  selector: 'mega-input',
  standalone: true,
  styleUrl: './mega-input.component.scss',
  providers: [
    { provide: NG_VALUE_ACCESSOR, useExisting: forwardRef(() => MegaInputComponent), multi: true },
  ],
  template: `
    @if (label()) {
      <label class="mega-field__label" [for]="id()">{{ label() }}</label>
    }
    <div class="mega-input" [class.mega-input--invalid]="isInvalid()">
      @if (prefix()) {
        <span class="mega-input__affix mega-input__affix--prefix" aria-hidden="true">{{ prefix() }}</span>
      }
      <input
        class="mega-input__control"
        [attr.id]="id()"
        [attr.type]="type()"
        [attr.placeholder]="placeholder() || null"
        [attr.aria-label]="ariaLabel() || null"
        [attr.aria-describedby]="describedBy()"
        [attr.aria-invalid]="isInvalid() ? 'true' : null"
        [attr.autocomplete]="autocomplete() || null"
        [attr.inputmode]="inputmode() || null"
        [attr.maxlength]="maxlength() || null"
        [readonly]="readonly()"
        [disabled]="disabled()"
        [value]="value()"
        (input)="onInput($event)"
        (blur)="onTouched()"
      />
      @if (suffix()) {
        <span class="mega-input__affix mega-input__affix--suffix" aria-hidden="true">{{ suffix() }}</span>
      }
    </div>
    @if (isInvalid() && error()) {
      <p class="mega-field__hint mega-field__hint--error" [attr.id]="errorId()">{{ error() }}</p>
    } @else if (hint()) {
      <p class="mega-field__hint" [attr.id]="hintId()">{{ hint() }}</p>
    }
  `,
})
export class MegaInputComponent implements ControlValueAccessor {
  /** Etiqueta visible del campo. */
  label = input<string | null>(null);

  /** Texto auxiliar bajo el campo. */
  hint = input<string | null>(null);

  /** Mensaje de validación; muestra el campo en estado inválido. */
  error = input<string | null>(null);

  /** Prefijo interior (p. ej. '$'). */
  prefix = input<string | null>(null);

  /** Sufijo interior (p. ej. 'USD'). */
  suffix = input<string | null>(null);

  /** Tipo nativo del input (text, email, password, search, tel, number). */
  type = input('text');

  /** Placeholder. */
  placeholder = input<string | null>(null);

  /** aria-label adicional (cuando no hay label visible). */
  ariaLabel = input<string | null>(null);

  /** autocomplete nativo. */
  autocomplete = input<string | null>(null);

  /** inputmode nativo (numeric, decimal…). */
  inputmode = input<string | null>(null);

  /** Longitud máxima. */
  maxlength = input<number | null>(null);

  /** Solo lectura. */
  readonly = input(false);

  /** Valor del campo. */
  value = signal('');

  /** Estado deshabilitado (ControlValueAccessor). */
  disabled = signal(false);

  readonly id = signal(megaUid('input'));

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
