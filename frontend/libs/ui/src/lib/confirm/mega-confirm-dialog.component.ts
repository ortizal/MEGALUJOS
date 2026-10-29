import { booleanAttribute, Component, ElementRef, HostListener, input, output, signal } from '@angular/core';
import { megaUid } from '../shared/mega-format';

/**
 * Diálogo de confirmación (uso directo o vía MegaConfirmService).
 * Estilos propios de confirmación destructiva; nunca depende solo del color
 * (el botón destructivo siempre muestra texto explícito).
 */
@Component({
  selector: 'mega-confirm-dialog',
  standalone: true,
  host: { class: 'mega-confirm-host' },
  styleUrl: './mega-confirm-dialog.component.scss',
  template: `
    <div class="mega-confirm__backdrop" (click)="onBackdrop()"></div>
    <div class="mega-confirm__wrap">
      <div
        #dialogRef
        class="mega-confirm"
        role="alertdialog"
        aria-modal="true"
        [attr.aria-labelledby]="titleId()"
        [attr.aria-describedby]="messageId()"
      >
        <div class="mega-confirm__icon" aria-hidden="true">{{ icon() }}</div>
        <h2 class="mega-confirm__title" [attr.id]="titleId()">{{ title() }}</h2>
        <p class="mega-confirm__message" [attr.id]="messageId()">{{ message() }}</p>
        <div class="mega-confirm__actions">
          <button type="button" class="mega-confirm__cancel" #cancelRef (click)="cancel.emit()">
            {{ cancelText() }}
          </button>
          <button
            type="button"
            class="mega-confirm__accept"
            [class.mega-confirm__accept--danger]="danger()"
            #acceptRef
            (click)="confirm.emit()"
          >
            {{ confirmText() }}
          </button>
        </div>
      </div>
    </div>
  `,
})
export class MegaConfirmDialogComponent {
  /** Título del diálogo. */
  title = input('¿Confirmar acción?');

  /** Mensaje explicativo (informativo y accionable). */
  message = input('Esta acción no se puede deshacer.');

  /** Texto del botón de aceptación. */
  confirmText = input('Confirmar');

  /** Texto del botón de cancelación. */
  cancelText = input('Cancelar');

  /** Marca la acción como destructiva (estilo rojo + foco en cancelar). */
  danger = input(false, { transform: booleanAttribute });

  /** Icono emoji decorativo. */
  icon = input('⚠️');

  /** Emite cuando el usuario confirma. */
  confirm = output<void>();

  /** Emite cuando el usuario cancela (botón, Escape o backdrop). */
  cancel = output<void>();

  readonly titleId = signal(megaUid('confirm-title'));
  readonly messageId = signal(megaUid('confirm-message'));

  @HostListener('document:keydown.escape')
  onEscape(): void {
    this.cancel.emit();
  }

  onBackdrop(): void {
    this.cancel.emit();
  }

  /** Enfoca el botón seguro al montar (llamado por el servicio tras crearlo). */
  focusCancel(): void {
    const buttons = this.element.nativeElement.querySelectorAll<HTMLButtonElement>(
      '.mega-confirm__cancel',
    );
    buttons[0]?.focus();
  }

  constructor(private readonly element: ElementRef<HTMLElement>) {}

  /** Enfoca el botón cancel por defecto (evita aceptación accidental). */
  focusDefault(): void {
    queueMicrotask(() => this.focusCancel());
  }
}
