import {
  ApplicationRef,
  ComponentRef,
  EnvironmentInjector,
  Injectable,
  createComponent,
  inject,
} from '@angular/core';
import { MegaConfirmDialogComponent } from './mega-confirm-dialog.component';

/** Opciones del diálogo de confirmación. */
export interface MegaConfirmOptions {
  /** Título del diálogo. */
  title?: string;
  /** Mensaje explicativo (informativo y accionable). */
  message?: string;
  /** Texto del botón de aceptación. */
  confirmText?: string;
  /** Texto del botón de cancelación. */
  cancelText?: string;
  /** true = acción destructiva (estilo rojo + foco en cancelar). */
  danger?: boolean;
  /** Icono emoji decorativo. */
  icon?: string;
}

/**
 * Confirmación programática:
 * `const ok = await this.confirm.open({ title: '¿Eliminar?', danger: true });`
 *
 * Crea el `MegaConfirmDialogComponent` de forma dinámica, lo monta en `body`
 * y lo destruye al resolver. Nunca acepta por defecto: el foco inicial es "Cancelar".
 */
@Injectable({ providedIn: 'root' })
export class MegaConfirmService {
  private readonly appRef = inject(ApplicationRef);
  private readonly injector = inject(EnvironmentInjector);

  /** Abre el diálogo y resuelve `true` (confirmar) o `false` (cancelar/Escape/backdrop). */
  open(options: MegaConfirmOptions = {}): Promise<boolean> {
    return new Promise<boolean>((resolve) => {
      const componentRef = createComponent(MegaConfirmDialogComponent, {
        environmentInjector: this.injector,
      });

      componentRef.setInput('title', options.title ?? '¿Confirmar acción?');
      componentRef.setInput(
        'message',
        options.message ?? 'Esta acción no se puede deshacer.',
      );
      componentRef.setInput('confirmText', options.confirmText ?? 'Confirmar');
      componentRef.setInput('cancelText', options.cancelText ?? 'Cancelar');
      componentRef.setInput('danger', options.danger ?? false);
      if (options.icon) {
        componentRef.setInput('icon', options.icon);
      }

      this.initiate(componentRef, resolve);
    });
  }

  /** Alias de `open` (nombre del contrato: `confirm`). */
  confirm(options: MegaConfirmOptions = {}): Promise<boolean> {
    return this.open(options);
  }

  /** Confirmación simple con solo mensaje. */
  ask(message: string, options: MegaConfirmOptions = {}): Promise<boolean> {
    return this.open({ ...options, message });
  }

  private initiate(componentRef: ComponentRef<MegaConfirmDialogComponent>, resolve: (value: boolean) => void): void {
    const instance = componentRef.instance;
    const element = componentRef.location.nativeElement as HTMLElement;
    let settled = false;
    const subscriptions: Array<{ unsubscribe(): void }> = [];

    const destroy = (result: boolean): void => {
      if (settled) {
        return;
      }
      settled = true;
      subscriptions.forEach((subscription) => subscription.unsubscribe());
      this.appRef.detachView(componentRef.hostView);
      componentRef.destroy();
      element.remove();
      document.body.style.removeProperty('overflow');
      resolve(result);
    };

    subscriptions.push(instance.confirm.subscribe(() => destroy(true)));
    subscriptions.push(instance.cancel.subscribe(() => destroy(false)));

    this.appRef.attachView(componentRef.hostView);
    document.body.appendChild(element);
    document.body.style.overflow = 'hidden';
    instance.focusDefault();
  }
}
