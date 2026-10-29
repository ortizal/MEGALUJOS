import { Routes } from '@angular/router';
import { PublicLayoutComponent } from './core/layout/public-layout.component';

export const routes: Routes = [
  {
    path: '',
    component: PublicLayoutComponent,
    children: [
      {
        path: '',
        title: 'MEGALUJOS — Accesorios y servicios automotrices',
        loadComponent: () =>
          import('./pages/home/home-page.component').then((m) => m.HomePageComponent),
      },
      {
        path: 'catalogo',
        title: 'Catálogo — MEGALUJOS',
        loadComponent: () =>
          import('./pages/catalog/catalog-page.component').then((m) => m.CatalogPageComponent),
      },
      {
        path: 'producto/:id',
        loadComponent: () =>
          import('./pages/product/product-detail-page.component').then(
            (m) => m.ProductDetailPageComponent,
          ),
      },
      {
        path: 'vehiculos',
        title: 'Vehículos — MEGALUJOS',
        loadComponent: () =>
          import('./pages/vehicles/vehicles-page.component').then((m) => m.VehiclesPageComponent),
      },
      {
        path: 'servicios',
        title: 'Servicios — MEGALUJOS',
        loadComponent: () =>
          import('./pages/services/services-page.component').then((m) => m.ServicesPageComponent),
      },
      {
        path: 'promociones',
        title: 'Promociones vigentes — MEGALUJOS',
        loadComponent: () =>
          import('./pages/promotions/promotions-page.component').then(
            (m) => m.PromotionsPageComponent,
          ),
      },
      {
        path: 'contacto',
        title: 'Contacto — MEGALUJOS',
        loadComponent: () =>
          import('./pages/contact/contact-page.component').then((m) => m.ContactPageComponent),
      },
    ],
  },
  {
    path: 'tv',
    title: 'MEGALUJOS TV — Presentación',
    loadComponent: () =>
      import('./pages/tv/tv-mode-page.component').then((m) => m.TvModePageComponent),
  },
  { path: '**', redirectTo: '' },
];
