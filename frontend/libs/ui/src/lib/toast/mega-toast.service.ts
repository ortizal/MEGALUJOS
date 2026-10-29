import {
  ApplicationRef,
  ComponentRef,
  EnvironmentInjector,
  Injectable,
  PLATFORM_ID,
  createComponent,
  inject,
} from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { signal } from '@angular/core';
import { MegaToastHostComponent } from './mega-toast-host.component';

/** Tono visual de un toast. */
export type MegaToastTone = 'success' | 'error' | 'info' | 'warning';

/** Opciones de creación de un toast. */
export interface MegaToastOptions {
  /** Título en negrita (opcional). */
  title?: string;
  /** Tono visual. */
  tone?: MegaToastTone;
  /** Duración en ms (`0` o `null` = persistente hasta cerrar). */
  duration?: number | null;
}

/** Toast visible (view-model). */
export interface MegaToastViewModel {
  id: number;
  message: string;
  title?: string;
  tone: MegaToastTone;
}

/**
 * Servicio de toasts con signals.
 * `success('Guardado')`, `error('No se pudo guardar')`, etc.
 *
 * Monta automáticamente un `<mega-toast-host />` en `body` (solo browser)
 * la primera vez que se muestra un toast; también puede montarse a mano
 * en la plantilla de la app si se prefiere.
 */
@Injectable({ providedIn: 'root' })
export class MegaToastService {
  private readonly appRef = inject(ApplicationRef);
  private readonly injector = inject(EnvironmentInjector);
  private readonly platformId = inject(PLATFORM_ID);

  private readonly toastsSignal = signal<MegaToastViewModel[]>([]);
  private readonly timers = new Map<number, ReturnType<typeof setTimeout>>();
  private counter = 0;

  /** Toasts activos. */
  readonly toasts = this.toastsSignal.asReadonly();

  /** Muestra un toast y devuelve su id. */
  show(message: string, options: MegaToastOptions = {}): number {
    this.counter += 1;
    const id = this.counter;
    const toast: MegaToastViewModel = {
      id,
      message,
      title: options.title,
      tone: options.tone ?? 'info',
    };
    this.toastsSignal.update((current) => [...current, toast]);
    this.ensureHost();

    const duration = options.duration ?? this.defaultDuration(toast.tone);
    if (duration && duration > 0) {
      this.timers.set(
        id,
        setTimeout(() => this.dismiss(id), duration),
      );
    }
    return id;
  }

  /** Toast de éxito. */
  success(message: string, options: Omit<MegaToastOptions, 'tone'> = {}): number {
    return this.show(message, { ...options, tone: 'success' });
  }

  /** Toast de error (rol `alert`, persistente por defecto). */
  error(message: string, options: Omit<MegaToastOptions, 'tone'> = {}): number {
    return this.show(message, { ...options, tone: 'error' });
  }

  /** Toast informativo. */
  info(message: string, options: Omit<MegaToastOptions, 'tone'> = {}): number {
    return this.show(message, { ...options, tone: 'info' });
  }

  /** Toast de advertencia. */
  warning(message: string, options: Omit<MegaToastOptions, 'tone'> = {}): number {
    return this.show(message, { ...options, tone: 'warning' });
  }

  /** Cierra un toast por id. */
  dismiss(id: number): void {
    const timer = this.timers.get(id);
    if (timer !== undefined) {
      clearTimeout(timer);
      this.timers.delete(id);
    }
    this.toastsSignal.update((current) => current.filter((toast) => toast.id !== id));
  }

  /** Cierra todos los toasts. */
  clear(): void {
    this.timers.forEach((timer) => clearTimeout(timer));
    this.timers.clear();
    this.toastsSignal.set([]);
  }

  private defaultDuration(tone: MegaToastTone): number {
    if (tone === 'error') {
      return 0;
    }
    if (tone === 'warning') {
      return 6000;
    }
    return 4000;
  }

  private ensureHost(): void {
    if (!isPlatformBrowser(this.platformId)) {
      return;
    }
    if (document.querySelector('mega-toast-host')) {
      return;
    }
    const componentRef: ComponentRef<MegaToastHostComponent> = createComponent(
      MegaToastHostComponent,
      { environmentInjector: this.injector },
    );
    this.appRef.attachView(componentRef.hostView);
    document.body.appendChild(componentRef.location.nativeElement as HTMLElement);
  }
}
