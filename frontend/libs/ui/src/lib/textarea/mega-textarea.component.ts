import { Component, computed, forwardRef, input, signal } from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';
import { megaUid } from '../shared/mega-format';
import { MegaFieldState } from '../shared/mega-field-state';

/**
 * Área de texto MEGALUJOS (ControlValueAccessor) sobre Bootstrap 5.
 * Emite `form-control form-control-sm` + la clase de estado de Bootstrap
 * (`is-valid` / `is-invalid` / `is-warning`), con label, hint y error.
 */
@Component({
  selector: 'mega-textarea',
  standalone: true,
  styleUrl: './mega-textarea.component.scss',
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => MegaTextareaComponent),
      multi: true,
    },
  ],
  template: `
    @if (label()) {
      <label class="form-label mega-field__label" [for]="id()">{{ label() }}</label>
    }
    <textarea
      class="form-control form-control-sm mega-field-control"
      [class.is-valid]="currentState() === 'valid'"
      [class.is-invalid]="showInvalid()"
      [class.is-warning]="currentState() === 'warning'"
      [attr.id]="id()"
      [attr.placeholder]="placeholder() || null"
      [attr.rows]="rows()"
      [attr.aria-label]="ariaLabel() || null"
      [attr.aria-describedby]="describedBy()"
      [attr.aria-invalid]="showInvalid() ? 'true' : null"
      [attr.maxlength]="maxlength() || null"
      [readonly]="readonly()"
      [disabled]="disabled()"
      [value]="value()"
      (input)="onInput($event)"
      (blur)="onTouched()"
    ></textarea>
    <div class="mega-textarea__footer">
      @if (showInvalid() && error()) {
        <div class="invalid-feedback d-block" [attr.id]="errorId()">{{ error() }}</div>
      } @else if (currentState() === 'valid' && validMessage()) {
        <div class="valid-feedback d-block" [attr.id]="hintId()">{{ validMessage() }}</div>
      } @else if (currentState() === 'warning' && warningMessage()) {
        <div class="mega-warning-feedback" [attr.id]="hintId()">{{ warningMessage() }}</div>
      } @else if (hint()) {
        <p class="mega-field__hint" [attr.id]="hintId()">{{ hint() }}</p>
      }
      @if (maxlength()) {
        <span class="mega-textarea__count" aria-hidden="true">
          {{ value().length }}/{{ maxlength() }}
        </span>
      }
    </div>
  `,
})
export class MegaTextareaComponent implements ControlValueAccessor {
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

  /** Filas visibles. */
  rows = input(4);

  /** Placeholder. */
  placeholder = input<string | null>(null);

  /** aria-label adicional. */
  ariaLabel = input<string | null>(null);

  /** Longitud máxima (muestra contador). */
  maxlength = input<number | null>(null);

  /** Solo lectura. */
  readonly = input(false);

  /** Valor del campo. */
  value = signal('');

  /** Estado deshabilitado. */
  disabled = signal(false);

  readonly id = signal(megaUid('textarea'));

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
    const next = (event.target as HTMLTextAreaElement).value;
    this.value.set(next);
    this.onChange(next);
  }
}
