import { Component, computed, forwardRef, input, signal } from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';
import { megaUid } from '../shared/mega-format';
import { MegaFieldState } from '../shared/mega-field-state';

/**
 * Selector de fecha MEGALUJOS (input date nativo sobre Bootstrap 5).
 * Emite `form-control form-control-sm` + la clase de estado de Bootstrap.
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
      <label class="form-label mega-field__label" [for]="id()">{{ label() }}</label>
    }
    <div class="input-group input-group-sm" [class.has-validation]="showInvalid()">
      <input
        class="form-control form-control-sm mega-field-control mega-date-picker__field"
        [class.is-valid]="currentState() === 'valid'"
        [class.is-invalid]="showInvalid()"
        [class.is-warning]="currentState() === 'warning'"
        [attr.id]="id()"
        type="date"
        [attr.min]="min() || null"
        [attr.max]="max() || null"
        [attr.aria-label]="ariaLabel() || label() || 'Fecha'"
        [attr.aria-describedby]="describedBy()"
        [attr.aria-invalid]="showInvalid() ? 'true' : null"
        [disabled]="disabled()"
        [value]="value()"
        (input)="onInput($event)"
        (blur)="onTouched()"
      />
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
export class MegaDatePickerComponent implements ControlValueAccessor {
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
