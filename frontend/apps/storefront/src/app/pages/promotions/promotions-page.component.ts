import { Component, OnDestroy, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { PublicShowcaseService } from '@megalujos/api-client';
import { PublicPromotionDto } from '@megalujos/models';
import { MegaEmptyStateComponent, MegaSkeletonComponent } from '@megalujos/ui';
import { Subject, takeUntil } from 'rxjs';
import { environment } from '../../../environments/environment';
import { formatDiscount, formatRange } from '../../shared/format.util';
import { waLink } from '../../shared/whatsapp.util';

@Component({
  selector: 'app-promotions-page',
  standalone: true,
  imports: [RouterLink, MegaEmptyStateComponent, MegaSkeletonComponent],
  styleUrl: './promotions-page.component.scss',
  templateUrl: './promotions-page.component.html',
})
export class PromotionsPageComponent implements OnDestroy {
  private readonly showcase = inject(PublicShowcaseService);
  private readonly destroy$ = new Subject<void>();

  readonly loading = signal(true);
  readonly error = signal(false);
  readonly promotions = signal<PublicPromotionDto[]>([]);

  constructor() {
    this.load();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  load(): void {
    this.loading.set(true);
    this.error.set(false);
    this.showcase
      .promotions()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (promotions) => {
          this.promotions.set(promotions ?? []);
          this.loading.set(false);
        },
        error: () => {
          this.error.set(true);
          this.loading.set(false);
        },
      });
  }

  discountLabel(promo: PublicPromotionDto): string {
    return formatDiscount(promo.promoType, promo.value);
  }

  validityLabel(promo: PublicPromotionDto): string {
    return formatRange(promo.startsAt, promo.endsAt);
  }

  whatsappUrl(): string {
    return waLink(
      'Hola MEGALUJOS 👋 Quiero aprovechar las promociones vigentes. ¿Me ayudan con details?',
      environment.whatsappNumber,
    );
  }
}
