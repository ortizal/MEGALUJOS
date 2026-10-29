import {
  Component,
  ElementRef,
  HostListener,
  OnDestroy,
  ViewChild,
  inject,
  signal,
} from '@angular/core';
import { RouterLink } from '@angular/router';
import { PublicShowcaseService } from '@megalujos/api-client';
import { PublicPromotionDto } from '@megalujos/models';
import {
  MegaCarouselComponent,
  MegaCarouselItem,
  MegaEmptyStateComponent,
  MegaSkeletonComponent,
} from '@megalujos/ui';
import { Subject, catchError, of, switchMap, takeUntil, timer } from 'rxjs';
import { formatDiscount } from '../../shared/format.util';

const REFRESH_MS = 60_000;
const MIN_AUTPLAY_SECONDS = 10;

@Component({
  selector: 'app-tv-mode-page',
  standalone: true,
  imports: [RouterLink, MegaCarouselComponent, MegaEmptyStateComponent, MegaSkeletonComponent],
  styleUrl: './tv-mode-page.component.scss',
  templateUrl: './tv-mode-page.component.html',
})
export class TvModePageComponent implements OnDestroy {
  private readonly showcase = inject(PublicShowcaseService);
  private readonly destroy$ = new Subject<void>();
  private readonly host = inject<ElementRef<HTMLElement>>(ElementRef);

  @ViewChild('stage') private stage?: ElementRef<HTMLElement>;

  readonly items = signal<MegaCarouselItem[]>([]);
  readonly promos = signal<PublicPromotionDto[]>([]);
  readonly loading = signal(true);
  readonly error = signal(false);
  readonly fullscreen = signal(false);
  readonly autoplaySeconds = signal(10);
  readonly clock = signal(this.now());

  constructor() {
    timer(0, REFRESH_MS)
      .pipe(
        switchMap(() =>
          this.showcase.tv().pipe(
            catchError(() => {
              const keepContent = this.items().length > 0;
              if (!keepContent) {
                this.error.set(true);
                this.loading.set(false);
              }
              return of(null);
            }),
          ),
        ),
        takeUntil(this.destroy$),
      )
      .subscribe((tv) => {
        if (!tv) {
          return;
        }
        this.items.set(
          (tv.items ?? []).map((item) => ({
            image: item.imageUrl ? { url: item.imageUrl, alt: item.name } : null,
            title: item.name,
            subtitle: [item.sku, `$${item.price}`].filter(Boolean).join(' · '),
          })),
        );
        this.promos.set((tv.promos ?? []).slice(0, 4));
        this.autoplaySeconds.set(Math.max(MIN_AUTPLAY_SECONDS, Number(tv.autoplaySeconds) || 10));
        this.loading.set(false);
        this.error.set(false);
      });

    timer(30_000, 30_000)
      .pipe(takeUntil(this.destroy$))
      .subscribe(() => this.clock.set(this.now()));
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  @HostListener('document:fullscreenchange')
  onFullscreenChange(): void {
    this.fullscreen.set(typeof document !== 'undefined' && !!document.fullscreenElement);
  }

  @HostListener('window:keydown', ['$event'])
  onKeydown(event: KeyboardEvent): void {
    const target = event.target as HTMLElement | null;
    const tag = target?.tagName?.toLowerCase();
    if (tag === 'input' || tag === 'textarea' || tag === 'select' || target?.isContentEditable) {
      return;
    }
    if (event.key === 'f' || event.key === 'F') {
      event.preventDefault();
      this.toggleFullscreen();
    }
  }

  toggleFullscreen(): void {
    const element = this.stage?.nativeElement ?? this.host.nativeElement;
    if (!document.fullscreenElement) {
      void element.requestFullscreen?.();
    } else {
      void document.exitFullscreen?.();
    }
  }

  promoLabel(promo: PublicPromotionDto): string {
    return `${promo.name} · ${formatDiscount(promo.promoType, promo.value)}`;
  }

  private now(): string {
    return new Intl.DateTimeFormat('es-EC', {
      hour: '2-digit',
      minute: '2-digit',
      hour12: false,
    }).format(new Date());
  }
}
