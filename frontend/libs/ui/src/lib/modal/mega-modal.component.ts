import {
  booleanAttribute,
  Component,
  ElementRef,
  HostListener,
  effect,
  inject,
  input,
  output,
  signal,
  viewChild,
} from '@angular/core';
import { megaUid } from '../shared/mega-format';

const FOCUSABLE_SELECTOR = [
  'a[href]',
  'button:not([disabled])',
  'input:not([disabled])',
  'select:not([disabled])',
  'textarea:not([disabled])',
  '[tabindex]:not([tabindex="-1"])',
].join(', ');

/**
 * Modal MEGALUJOS: overlay con backdrop, cierre con Escape,
 * trap de foco básico y restauración del foco al cerrar.
 * Tamaños: sm / md / lg.
 */
@Component({
  selector: 'mega-modal',
  standalone: true,
  styleUrl: './mega-modal.component.scss',
  template: `
    @if (open()) {
      <div class="mega-modal__backdrop" (click)="onBackdropClick()"></div>
      <div class="mega-modal__wrap">
        <div
          #dialogRef
          [class]="'mega-modal mega-modal--' + size()"
          role="dialog"
          aria-modal="true"
          tabindex="-1"
          [attr.aria-labelledby]="hasTitle() ? titleId() : null"
          [attr.aria-label]="hasTitle() ? null : ariaLabel()"
        >
          @if (hasTitle() || closable()) {
            <header class="mega-modal__header">
              @if (hasTitle()) {
                <h2 class="mega-modal__title" [attr.id]="titleId()">{{ title() }}</h2>
              }
              @if (closable()) {
                <button
                  type="button"
                  class="mega-modal__close"
                  [attr.aria-label]="closeLabel()"
                  (click)="requestClose()"
                >
                  <svg viewBox="0 0 16 16" aria-hidden="true" focusable="false">
                    <path
                      d="M3 3l10 10M13 3L3 13"
                      stroke="currentColor"
                      stroke-width="1.8"
                      stroke-linecap="round"
                    />
                  </svg>
                </button>
              }
            </header>
          }
          <div class="mega-modal__body">
            <ng-content />
          </div>
          <footer class="mega-modal__footer">
            <ng-content select="[megaModalFooter]" />
          </footer>
        </div>
      </div>
    }
  `,
})
export class MegaModalComponent {
  /** Abre/cierra el modal. */
  open = input(false, { transform: booleanAttribute });

  /** Título del diálogo (usado como aria-labelledby). */
  title = input<string | null>(null);

  /** Tamaño del modal. */
  size = input<'sm' | 'md' | 'lg'>('md');

  /** Muestra el botón de cierre y permite backdrop/Escape. */
  closable = input(true, { transform: booleanAttribute });

  /** Cierra al pulsar el backdrop. */
  backdropClose = input(true, { transform: booleanAttribute });

  /** aria-label cuando no hay título. */
  ariaLabel = input('Diálogo');

  /** Texto del botón de cierre (aria-label). */
  closeLabel = input('Cerrar diálogo');

  /** Emite cuando el usuario pide cerrar (Escape, backdrop o botón). */
  close = output<void>();

  readonly titleId = signal(megaUid('modal-title'));

  private readonly element = inject(ElementRef<HTMLElement>);
  private previousFocus: HTMLElement | null = null;
  private scrollLocked = false;

  private readonly dialog = viewChild.required<ElementRef<HTMLElement>>('dialogRef');

  constructor() {
    effect(() => {
      if (this.open()) {
        this.onOpen();
      } else {
        this.onClose();
      }
    });
  }

  hasTitle(): boolean {
    return !!this.title();
  }

  /** Solicita el cierre del modal (emite `close`). */
  requestClose(): void {
    if (!this.closable()) {
      return;
    }
    this.close.emit();
  }

  onBackdropClick(): void {
    if (this.backdropClose()) {
      this.requestClose();
    }
  }

  @HostListener('document:keydown', ['$event'])
  onDocumentKeydown(event: KeyboardEvent): void {
    if (!this.open()) {
      return;
    }
    if (event.key === 'Escape') {
      event.preventDefault();
      this.requestClose();
      return;
    }
    if (event.key === 'Tab') {
      this.trapFocus(event);
    }
  }

  private onOpen(): void {
    this.previousFocus = (this.element.nativeElement.ownerDocument.activeElement as HTMLElement) ?? null;
    if (!this.scrollLocked) {
      this.scrollLocked = true;
      this.element.nativeElement.ownerDocument.body.style.overflow = 'hidden';
    }
    // Da un tick al render para enfocar el primer elemento interactivo
    setTimeout(() => this.focusFirst(), 0);
  }

  private onClose(): void {
    if (this.scrollLocked) {
      this.scrollLocked = false;
      this.element.nativeElement.ownerDocument.body.style.overflow = '';
    }
    if (this.previousFocus && typeof this.previousFocus.focus === 'function') {
      this.previousFocus.focus();
    }
    this.previousFocus = null;
  }

  private focusFirst(): void {
    const dialog = this.dialogOrNull();
    if (!dialog) {
      return;
    }
    const focusable = dialog.querySelectorAll<HTMLElement>(FOCUSABLE_SELECTOR);
    (focusable[0] ?? dialog).focus();
  }

  private trapFocus(event: KeyboardEvent): void {
    const dialog = this.dialogOrNull();
    if (!dialog) {
      return;
    }
    const focusable = Array.from(dialog.querySelectorAll<HTMLElement>(FOCUSABLE_SELECTOR)).filter(
      (element) => element.offsetParent !== null || element === document.activeElement,
    );
    if (focusable.length === 0) {
      event.preventDefault();
      dialog.focus();
      return;
    }
    const first = focusable[0];
    const last = focusable[focusable.length - 1];
    const active = dialog.contains(document.activeElement) ? document.activeElement : null;
    if (event.shiftKey && (active === first || !active)) {
      event.preventDefault();
      last.focus();
    } else if (!event.shiftKey && active === last) {
      event.preventDefault();
      first.focus();
    }
  }

  private dialogOrNull(): HTMLElement | null {
    try {
      return this.dialog().nativeElement;
    } catch {
      return null;
    }
  }
}
