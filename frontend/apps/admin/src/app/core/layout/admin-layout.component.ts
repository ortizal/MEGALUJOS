import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import {
  ActivatedRouteSnapshot,
  NavigationEnd,
  Router,
  RouterLink,
  RouterLinkActive,
  RouterOutlet,
} from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { filter } from 'rxjs';
import { AuthService } from '@megalujos/auth';
import { ThemeService } from '@megalujos/theme';

/** Enlace del menú lateral. */
interface NavItem {
  label: string;
  icon: string;
  path: string;
  permissions: string[];
  queryParams?: Record<string, string>;
}

/** Sección agrupada del menú lateral. */
interface NavSection {
  title: string;
  items: NavItem[];
}

const NAV: NavSection[] = [
  {
    title: 'Principal',
    items: [{ label: 'Dashboard', icon: '📊', path: '/dashboard', permissions: [] }],
  },
  {
    title: 'POS',
    items: [
      { label: 'Punto de venta', icon: '🛒', path: '/pos', permissions: ['sales:create'] },
      { label: 'Ventas', icon: '🧾', path: '/ventas', permissions: ['sales:read'] },
      { label: 'Devoluciones', icon: '↩️', path: '/devoluciones', permissions: ['sales:return'] },
    ],
  },
  {
    title: 'Caja',
    items: [{ label: 'Sesiones', icon: '💰', path: '/caja', permissions: ['cash:manage'] }],
  },
  {
    title: 'Catálogo',
    items: [
      { label: 'Productos', icon: '📦', path: '/catalogo/productos', permissions: ['catalog:read'] },
      { label: 'Categorías', icon: '🗂️', path: '/catalogo/categorias', permissions: ['catalog:read'] },
      { label: 'Marcas', icon: '🏷️', path: '/catalogo/marcas', permissions: ['catalog:read'] },
      { label: 'Vehículos', icon: '🚗', path: '/catalogo/vehiculos', permissions: ['catalog:read'] },
    ],
  },
  {
    title: 'Inventario',
    items: [
      { label: 'Stock', icon: '📉', path: '/inventario/stock', permissions: ['inventory:read'] },
      { label: 'Movimientos', icon: '📜', path: '/inventario/movimientos', permissions: ['inventory:read'] },
      { label: 'Ajustes', icon: '⚖️', path: '/inventario/ajustes', permissions: ['inventory:adjust'] },
      { label: 'Conteos', icon: '🔢', path: '/inventario/conteos', permissions: ['inventory:count'] },
      { label: 'Alertas', icon: '⚠️', path: '/inventario/alertas', permissions: ['inventory:read'] },
    ],
  },
  {
    title: 'Servicios',
    items: [
      { label: 'Órdenes', icon: '🔧', path: '/servicios/ordenes', permissions: ['services:read'] },
      { label: 'Agenda', icon: '📅', path: '/servicios/agenda', permissions: ['services:read'] },
    ],
  },
  {
    title: 'Facturación',
    items: [
      { label: 'Emisores', icon: '🏢', path: '/facturacion/emisores', permissions: ['invoicing:read'] },
      { label: 'Documentos', icon: '📄', path: '/facturacion/documentos', permissions: ['invoicing:read'] },
      {
        label: 'Notas de crédito',
        icon: '🧮',
        path: '/facturacion/documentos',
        permissions: ['invoicing:read'],
        queryParams: { tipo: 'CREDIT_NOTE' },
      },
    ],
  },
  {
    title: 'Comercial',
    items: [
      { label: 'Clientes', icon: '👥', path: '/clientes', permissions: ['customers:manage'] },
      { label: 'Proveedores', icon: '🏭', path: '/compras/proveedores', permissions: ['purchasing:manage'] },
      { label: 'Compras', icon: '📑', path: '/compras/ordenes', permissions: ['purchasing:manage'] },
    ],
  },
  {
    title: 'Vitrina',
    items: [
      { label: 'Banners', icon: '🖼️', path: '/vitrina/banners', permissions: ['showcase:manage'] },
      { label: 'Showcases', icon: '🧩', path: '/vitrina/showcases', permissions: ['showcase:manage'] },
      { label: 'Promociones', icon: '🎁', path: '/vitrina/promociones', permissions: ['showcase:manage'] },
    ],
  },
  {
    title: 'Reportes',
    items: [{ label: 'Reportes', icon: '📈', path: '/reportes', permissions: ['reports:view'] }],
  },
  {
    title: 'Configuración',
    items: [
      { label: 'Usuarios', icon: '🧑‍💼', path: '/configuracion/usuarios', permissions: ['users:manage'] },
      { label: 'Roles', icon: '🔐', path: '/configuracion/roles', permissions: ['users:manage'] },
      { label: 'Auditoría', icon: '📋', path: '/configuracion/auditoria', permissions: ['audit:read'] },
      { label: 'Impresión', icon: '🖨️', path: '/configuracion/impresion', permissions: ['print:manage'] },
    ],
  },
];

/**
 * Shell del panel: sidebar oscuro permanente (secciones agrupadas + permisos),
 * topbar con breadcrumb, tema, usuario y cierre de sesión. Responsive con
 * hamburger y overlay en móvil.
 */
@Component({
  selector: 'app-admin-layout',
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  templateUrl: './admin-layout.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AdminLayoutComponent {
  readonly auth = inject(AuthService);
  readonly theme = inject(ThemeService);
  private readonly router = inject(Router);

  readonly sidebarOpen = signal(false);
  readonly breadcrumb = signal<string[]>([]);
  readonly sections = signal<NavSection[]>(NAV);

  constructor() {
    this.refreshBreadcrumb();
    this.router.events
      .pipe(
        filter((event): event is NavigationEnd => event instanceof NavigationEnd),
        takeUntilDestroyed(),
      )
      .subscribe(() => {
        this.sidebarOpen.set(false);
        this.refreshBreadcrumb();
      });
  }

  visibleItems(section: NavSection): NavItem[] {
    if (this.auth.hasRole('ADMIN')) {
      return section.items;
    }
    return section.items.filter((item) => this.auth.hasAnyPermission(item.permissions));
  }

  toggleSidebar(): void {
    this.sidebarOpen.update((open) => !open);
  }

  closeSidebar(): void {
    this.sidebarOpen.set(false);
  }

  toggleTheme(): void {
    this.theme.toggle();
  }

  logout(): void {
    this.auth.logout();
    void this.router.navigate(['/login']);
  }

  private refreshBreadcrumb(): void {
    const crumbs: string[] = [];
    let route: ActivatedRouteSnapshot | null = this.router.routerState.snapshot.root;
    while (route) {
      const data = route.data as { section?: string; title?: string };
      if (data['section'] && !crumbs.includes(data['section'])) {
        crumbs.push(data['section']);
      }
      if (data['title'] && crumbs[crumbs.length - 1] !== data['title']) {
        crumbs.push(data['title']);
      }
      route = route.firstChild;
    }
    this.breadcrumb.set(crumbs.length ? crumbs : ['Panel']);
  }
}
