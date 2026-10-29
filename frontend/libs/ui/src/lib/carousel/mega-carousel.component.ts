import {
  Component,
  ElementRef,
  OnDestroy,
  booleanAttribute,
  computed,
  effect,
  inject,
  input,
  output,
  signal,
} from '@angular/core';
import { MegaGalleryImage } from '../product-gallery/mega-product-gallery.component';

/** Elemento del carrusel (banner o producto destacado). */
export interface MegaCarouselItem {
  /** Imagen de fondo o lateral. */
  image?: MegaGalleryImage | null;
  /** Título del slide. */
  title?: string | null;
  /** Subtítulo o texto de apoyo. */
  subtitle?: string | null;
  /** Texto del enlace/acción. */
  cta?: string | null;
  /** URL de destino al pulsar el slide. */
  href?: string | null;
}

/**
 * Carrusel MEGALUJOS con autoplay (segundos configurables), pausa al
 * hover/foco, puntos de navegación y soporte de `prefers-reduced-motion`
 * (sin autoplay automático).
 */
@Component({
  selector: 'mega-carousel',
  standalone: true,
  styleUrl: './mega-carousel.component.scss',
  template: `
    <section
      class="mega-carousel"
      [attr.aria-label]="label()"
      (mouseenter)="pause()"
      (mouseleave)="resume()"
      (focusin)="pause()"
      (focusout)="resume()"
    >
      <div class="mega-carousel__viewport">
        @for (item of items(); track $index; let i = $index) {
          <article
            class="mega-carousel__slide"
            [class.mega-carousel__slide--active]="i === index()"
            [attr.aria-hidden]="i === index() ? null : 'true'"
          >
            @if (item.image?.url) {
              <img
                class="mega-carousel__image"
                [src]="item.image?.url"
                [attr.alt]="item.image?.alt ?? item.title ?? ''"
              />
            }
            <div class="mega-carousel__content">
              @if (item.title) {
                <h2 class="mega-carousel__title">{{ item.title }}</h2>
              }
              @if (item.subtitle) {
                <p class="mega-carousel__subtitle">{{ item.subtitle }}</p>
              }
              @if (item.cta) {
                <a
                  class="mega-carousel__cta"
                  [href]="item.href ?? '#'"
                  (click)="onCtaClick($event, item)"
                >
                  {{ item.cta }}
                </a>
              }
            </div>
          </article>
        }
      </div>

      @if (items().length > 1) {
        <div class="mega-carousel__dots">
          @for (item of items(); track $index; let i = $index) {
            <button
              type="button"
              class="mega-carousel__dot"
              [class.mega-carousel__dot--active]="i === index()"
              [attr.aria-label]="'Ir al slide ' + (i + 1)"
              [attr.aria-current]="i === index() ? 'true' : null"
              (click)="goTo(i)"
            ></button>
          }
        </div>
      }
    </section>
  `,
})
export class MegaCarouselComponent implements OnDestroy {
  /** Slides del carrusel. */
  items = input<MegaCarouselItem[]>([]);

  /** Segundos por slide en autoplay (0 = sin autoplay). */
  autoplaySeconds = input(6);

  /** true para pausar al pasar el mouse/foco. */
  pauseOnHover = input(true, { transform: booleanAttribute });

  /** aria-label del carrusel. */
  label = input('Carrusel');

  /** Emite al activar un slide (por autoplay o clic). */
  indexChange = output<number>();

  /** Índice del slide activo. */
  readonly index = signal(0);

  /** Slide activo. */
  readonly current = computed(() => this.items()[this.index()] ?? null);

  private readonly host = inject<ElementRef<HTMLElement>>(ElementRef);
  private timer: ReturnType<typeof setTimeout> | null = null;
  private paused = false;
  private destroyed = false;

  constructor() {
    effect(() => {
      // Reinicia el temporizador cuando cambian los slides o la duración.
      this.items();
      this.autoplaySeconds();
      this.restartTimer();
    });
  }

  ngOnDestroy(): void {
    this.destroyed = true;
    this.clearTimer();
  }

  /** Pasa al slide indicado. */
  goTo(index: number): void {
    const total = this.items().length;
    if (total === 0) {
      return;
    }
    const normalized = ((index % total) + total) % total;
    if (normalized !== this.index()) {
      this.index.set(normalized);
      this.indexChange.emit(normalized);
    }
    this.restartTimer();
  }

  next(): void {
    this.goTo(this.index() + 1);
  }

  pause(): void {
    if (!this.pauseOnHover()) {
      return;
    }
    this.paused = true;
    this.clearTimer();
  }

  resume(): void {
    this.paused = false;
    this.restartTimer();
  }

  onCtaClick(event: MouseEvent, item: MegaCarouselItem): void {
    if (!item.href || item.href === '#') {
      event.preventDefault();
    }
  }

  private restartTimer(): void {
    this.clearTimer();
    if (this.destroyed || this.paused || this.items().length < 2) {
      return;
    }
    const seconds = this.autoplaySeconds();
    if (seconds <= 0 || this.prefersReducedMotion()) {
      return;
    }
    this.timer = setTimeout(() => {
      this.next();
    }, seconds * 1000);
  }

  private clearTimer(): void {
    if (this.timer !== null) {
      clearTimeout(this.timer);
      this.timer = null;
    }
  }

  private prefersReducedMotion(): boolean {
    const windowRef = this.host.nativeElement.ownerDocument.defaultView;
    return !!windowRef?.matchMedia?.('(prefers-reduced-motion: reduce)').matches;
  }
}
