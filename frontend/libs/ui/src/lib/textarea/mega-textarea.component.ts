import { Component, forwardRef, input, signal } from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';
import { megaUid } from '../shared/mega-format';

/**
 * Área de texto MEGALUJOS (ControlValueAccessor) con label, hint y error.
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
      <label class="mega-field__label" [for]="id()">{{ label() }}</label>
    }
    <div class="mega-control mega-control--textarea" [class.mega-control--invalid]="isInvalid()">
      <textarea
        class="mega-control__field mega-textarea__control"
        [attr.id]="id()"
        [attr.placeholder]="placeholder() || null"
        [attr.rows]="rows()"
        [attr.aria-label]="ariaLabel() || null"
        [attr.aria-describedby]="describedBy()"
        [attr.aria-invalid]="isInvalid() ? 'true' : null"
        [attr.maxlength]="maxlength() || null"
        [readonly]="readonly()"
        [disabled]="disabled()"
        [value]="value()"
        (input)="onInput($event)"
        (blur)="onTouched()"
      ></textarea>
    </div>
    <div class="mega-textarea__footer">
      @if (isInvalid() && error()) {
        <p class="mega-field__hint mega-field__hint--error" [attr.id]="errorId()">{{ error() }}</p>
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
    const next = (event.target as HTMLTextAreaElement).value;
    this.value.set(next);
    this.onChange(next);
  }
}
