import { Component, OnDestroy, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { PublicShowcaseService } from '@megalujos/api-client';
import { ProductPublicDto } from '@megalujos/models';
import { MegaCarouselComponent, MegaCarouselItem, MegaSkeletonComponent } from '@megalujos/ui';
import { Subject, takeUntil } from 'rxjs';
import { environment } from '../../../environments/environment';
import { generalWhatsappMessage, waLink } from '../../shared/whatsapp.util';
import { ProductCardWrapperComponent } from '../../shared/product-card/product-card-wrapper.component';

interface ServiceCard {
  icon: string;
  title: string;
  description: string;
  priceFrom: string;
}

@Component({
  selector: 'app-home-page',
  standalone: true,
  imports: [RouterLink, MegaCarouselComponent, MegaSkeletonComponent, ProductCardWrapperComponent],
  styleUrl: './home-page.component.scss',
  templateUrl: './home-page.component.html',
})
export class HomePageComponent implements OnDestroy {
  private readonly showcase = inject(PublicShowcaseService);
  private readonly destroy$ = new Subject<void>();

  readonly loading = signal(true);
  readonly error = signal(false);
  readonly banners = signal<MegaCarouselItem[]>([]);
  readonly featured = signal<ProductPublicDto[]>([]);
  readonly autoplaySeconds = signal(6);
  readonly showcaseTitle = signal<string | null>(null);

  readonly whatsappUrl = waLink(generalWhatsappMessage(), environment.whatsappNumber);

  readonly services: ServiceCard[] = [
    {
      icon: '🧽',
      title: 'Detailing premium',
      description:
        'Limpieza profunda, descontaminación, pulido y sellado para dejar tu vehículo como nuevo.',
      priceFrom: 'Desde $45',
    },
    {
      icon: '🔧',
      title: 'Instalación profesional',
      description:
        'Stickers, luces LED, rines, cámara de retroceso y más, instalados por especialistas.',
      priceFrom: 'Desde $15',
    },
    {
      icon: '🛡️',
      title: 'Protección y sellado',
      description:
        'Tratamientos cerámicos y películas de protección que cuidan la pintura por años.',
      priceFrom: 'Desde $120',
    },
  ];

  readonly trustItems = [
    { icon: '✅', label: 'Garantía en instalación' },
    { icon: '🚚', label: 'Envíos a todo el país' },
    { icon: '💳', label: 'Pagos seguros' },
    { icon: '💬', label: 'Asesoría por WhatsApp' },
  ];

  constructor() {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.error.set(false);

    this.showcase
      .home()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (home) => {
          this.banners.set(
            (home.banners ?? [])
              .filter((banner) => banner.active)
              .sort((a, b) => a.sortOrder - b.sortOrder)
              .map((banner) => ({
                image: banner.url
                  ? { url: banner.url, alt: banner.title ?? 'Banner MEGALUJOS' }
                  : null,
                title: banner.title,
                subtitle: banner.subtitle,
                cta: banner.linkUrl ? 'Ver más' : null,
                href: banner.linkUrl ?? null,
              })),
          );
          this.featured.set((home.featured ?? home.featuredProducts ?? []).slice(0, 8));
          this.autoplaySeconds.set(home.autoplaySeconds > 0 ? home.autoplaySeconds : 6);
          this.showcaseTitle.set(home.showcaseTitle ?? null);
          this.loading.set(false);
        },
        error: () => {
          this.error.set(true);
          this.loading.set(false);
        },
      });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }
}
