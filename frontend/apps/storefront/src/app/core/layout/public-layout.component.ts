import { Component, inject, signal } from '@angular/core';
import { NavigationEnd, Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { filter } from 'rxjs';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { environment } from '../../../environments/environment';
import { generalWhatsappMessage, waLink } from '../../shared/whatsapp.util';
import { WhatsAppButtonComponent } from '../whatsapp/whatsapp-button.component';

interface NavLink {
  path: string;
  label: string;
  exact: boolean;
}

@Component({
  selector: 'app-public-layout',
  standalone: true,
  imports: [RouterOutlet, RouterLink, RouterLinkActive, WhatsAppButtonComponent],
  styleUrl: './public-layout.component.scss',
  templateUrl: './public-layout.component.html',
})
export class PublicLayoutComponent {
  readonly menuOpen = signal(false);
  readonly year = new Date().getFullYear();

  readonly navLinks: NavLink[] = [
    { path: '/', label: 'Inicio', exact: true },
    { path: '/catalogo', label: 'Catálogo', exact: false },
    { path: '/servicios', label: 'Servicios', exact: false },
    { path: '/promociones', label: 'Promociones', exact: false },
    { path: '/tv', label: 'TV', exact: false },
    { path: '/contacto', label: 'Contacto', exact: false },
  ];

  readonly whatsappUrl = waLink(generalWhatsappMessage(), environment.whatsappNumber);
  readonly contactPhone = environment.contactPhone;
  readonly address = environment.address;
  readonly hours = environment.hours;
  readonly facebookUrl = environment.facebookUrl;
  readonly instagramUrl = environment.instagramUrl;
  readonly youtubeUrl = environment.youtubeUrl;

  constructor() {
    inject(Router)
      .events.pipe(
        filter((event): event is NavigationEnd => event instanceof NavigationEnd),
        takeUntilDestroyed(),
      )
      .subscribe(() => this.menuOpen.set(false));
  }

  toggleMenu(): void {
    this.menuOpen.update((open) => !open);
  }
}
