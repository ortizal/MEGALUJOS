import { Component, computed, forwardRef, input, signal } from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';
import { megaUid } from '../shared/mega-format';
import { MegaFieldState } from '../shared/mega-field-state';

/**
 * Campo de texto MEGALUJOS (ControlValueAccessor) sobre Bootstrap 5.
 * Emite `form-control form-control-sm` + la clase de estado de Bootstrap
 * (`is-valid` / `is-invalid` / `is-warning`) para que el sistema muestre
 * siempre los estados de validación. Conserva label, hint, error y
 * prefix/suffix, con aria-describedby y aria-invalid correctos.
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
      <label class="form-label mega-field__label" [for]="id()">{{ label() }}</label>
    }
    <div class="input-group input-group-sm" [class.has-validation]="showInvalid()">
      @if (prefix()) {
        <span class="input-group-text" aria-hidden="true">{{ prefix() }}</span>
      }
      <input
        class="form-control form-control-sm mega-field-control"
        [class.is-valid]="currentState() === 'valid'"
        [class.is-invalid]="showInvalid()"
        [class.is-warning]="currentState() === 'warning'"
        [attr.id]="id()"
        [attr.type]="type()"
        [attr.placeholder]="placeholder() || null"
        [attr.aria-label]="ariaLabel() || null"
        [attr.aria-describedby]="describedBy()"
        [attr.aria-invalid]="showInvalid() ? 'true' : null"
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
        <span class="input-group-text" aria-hidden="true">{{ suffix() }}</span>
      }
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
export class MegaInputComponent implements ControlValueAccessor {
  /** Etiqueta visible del campo. */
  label = input<string | null>(null);

  /** Texto auxiliar bajo el campo. */
  hint = input<string | null>(null);

  /** Mensaje de validación; muestra el campo en estado inválido. */
  error = input<string | null>(null);

  /** Estado explícito de validación (anula `error` si ambos se pasan). */
  state = input<MegaFieldState>(null);

  /** Mensaje mostrado cuando `state` es `valid`. */
  validMessage = input<string | null>(null);

  /** Mensaje mostrado cuando `state` es `warning`. */
  warningMessage = input<string | null>(null);

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

  onInput(event: Event): void {
    const next = (event.target as HTMLInputElement).value;
    this.value.set(next);
    this.onChange(next);
  }
}
