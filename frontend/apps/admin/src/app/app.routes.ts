import { Routes } from '@angular/router';
import { authGuard, anonymousGuard, permissionGuard } from '@megalujos/auth';

export const routes: Routes = [
  {
    path: 'login',
    title: 'Iniciar sesión | MEGALUJOS',
    canActivate: [anonymousGuard],
    loadComponent: () => import('./core/auth/login.component').then((m) => m.LoginComponent),
  },
  {
    path: 'acceso-denegado',
    title: 'Acceso denegado | MEGALUJOS',
    loadComponent: () =>
      import('./features/errors/forbidden.component').then((m) => m.ForbiddenComponent),
  },
  {
    path: '',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./core/layout/admin-layout.component').then((m) => m.AdminLayoutComponent),
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'dashboard' },
      {
        path: 'dashboard',
        title: 'Dashboard | MEGALUJOS',
        data: { section: 'Principal', title: 'Dashboard' },
        loadComponent: () =>
          import('./features/dashboard/dashboard.page').then((m) => m.DashboardPage),
      },

      // ------------------------------------------------------------------ POS
      {
        path: 'pos',
        title: 'Punto de venta | MEGALUJOS',
        canActivate: [permissionGuard(['sales:create'])],
        data: { section: 'POS', title: 'Punto de venta' },
        loadComponent: () => import('./features/pos/pos.page').then((m) => m.PosPage),
      },
      {
        path: 'ventas',
        title: 'Ventas | MEGALUJOS',
        canActivate: [permissionGuard(['sales:read'])],
        data: { section: 'POS', title: 'Ventas' },
        loadComponent: () =>
          import('./features/sales/sales-list.page').then((m) => m.SalesListPage),
      },
      {
        path: 'devoluciones',
        title: 'Devoluciones | MEGALUJOS',
        canActivate: [permissionGuard(['sales:return'])],
        data: { section: 'POS', title: 'Devoluciones' },
        loadComponent: () =>
          import('./features/returns/returns.page').then((m) => m.ReturnsPage),
      },

      // ----------------------------------------------------------------- Caja
      {
        path: 'caja',
        title: 'Caja | MEGALUJOS',
        canActivate: [permissionGuard(['cash:manage'])],
        data: { section: 'Caja', title: 'Sesiones de caja' },
        loadComponent: () =>
          import('./features/cash/cash-sessions.page').then((m) => m.CashSessionsPage),
      },
      {
        path: 'caja/:id',
        title: 'Detalle de caja | MEGALUJOS',
        canActivate: [permissionGuard(['cash:manage'])],
        data: { section: 'Caja', title: 'Detalle de sesión' },
        loadComponent: () =>
          import('./features/cash/cash-session-detail.page').then(
            (m) => m.CashSessionDetailPage,
          ),
      },

      // -------------------------------------------------------------- Catálogo
      {
        path: 'catalogo/productos',
        title: 'Productos | MEGALUJOS',
        canActivate: [permissionGuard(['catalog:read'])],
        data: { section: 'Catálogo', title: 'Productos' },
        loadComponent: () =>
          import('./features/catalog/products-admin.page').then((m) => m.ProductsAdminPage),
      },
      {
        path: 'catalogo/productos/nuevo',
        title: 'Nuevo producto | MEGALUJOS',
        canActivate: [permissionGuard(['catalog:read'])],
        data: { section: 'Catálogo', title: 'Nuevo producto' },
        loadComponent: () =>
          import('./features/catalog/product-form.page').then((m) => m.ProductFormPage),
      },
      {
        path: 'catalogo/productos/:id',
        title: 'Editar producto | MEGALUJOS',
        canActivate: [permissionGuard(['catalog:read'])],
        data: { section: 'Catálogo', title: 'Editar producto' },
        loadComponent: () =>
          import('./features/catalog/product-form.page').then((m) => m.ProductFormPage),
      },
      {
        path: 'catalogo/categorias',
        title: 'Categorías | MEGALUJOS',
        canActivate: [permissionGuard(['catalog:read'])],
        data: { section: 'Catálogo', title: 'Categorías' },
        loadComponent: () =>
          import('./features/catalog/categories-admin.page').then((m) => m.CategoriesAdminPage),
      },
      {
        path: 'catalogo/marcas',
        title: 'Marcas | MEGALUJOS',
        canActivate: [permissionGuard(['catalog:read'])],
        data: { section: 'Catálogo', title: 'Marcas' },
        loadComponent: () =>
          import('./features/catalog/brands-admin.page').then((m) => m.BrandsAdminPage),
      },
      {
        path: 'catalogo/vehiculos',
        title: 'Vehículos | MEGALUJOS',
        canActivate: [permissionGuard(['catalog:read'])],
        data: { section: 'Catálogo', title: 'Vehículos' },
        loadComponent: () =>
          import('./features/catalog/vehicles-admin.page').then((m) => m.VehiclesAdminPage),
      },

      // ------------------------------------------------------------ Inventario
      {
        path: 'inventario/stock',
        title: 'Stock | MEGALUJOS',
        canActivate: [permissionGuard(['inventory:read'])],
        data: { section: 'Inventario', title: 'Existencias' },
        loadComponent: () => import('./features/inventory/stock.page').then((m) => m.StockPage),
      },
      {
        path: 'inventario/movimientos',
        title: 'Movimientos | MEGALUJOS',
        canActivate: [permissionGuard(['inventory:read'])],
        data: { section: 'Inventario', title: 'Movimientos (kardex)' },
        loadComponent: () =>
          import('./features/inventory/movements.page').then((m) => m.MovementsPage),
      },
      {
        path: 'inventario/ajustes',
        title: 'Ajustes | MEGALUJOS',
        canActivate: [permissionGuard(['inventory:adjust'])],
        data: { section: 'Inventario', title: 'Ajustes' },
        loadComponent: () =>
          import('./features/inventory/adjustment.page').then((m) => m.AdjustmentPage),
      },
      {
        path: 'inventario/conteos',
        title: 'Conteos | MEGALUJOS',
        canActivate: [permissionGuard(['inventory:count'])],
        data: { section: 'Inventario', title: 'Conteos físicos' },
        loadComponent: () =>
          import('./features/inventory/counts.page').then((m) => m.CountsPage),
      },
      {
        path: 'inventario/alertas',
        title: 'Alertas de stock | MEGALUJOS',
        canActivate: [permissionGuard(['inventory:read'])],
        data: { section: 'Inventario', title: 'Alertas' },
        loadComponent: () =>
          import('./features/inventory/alerts.page').then((m) => m.AlertsPage),
      },

      // ------------------------------------------------------------- Servicios
      {
        path: 'servicios/ordenes',
        title: 'Órdenes de servicio | MEGALUJOS',
        canActivate: [permissionGuard(['services:read'])],
        data: { section: 'Servicios', title: 'Órdenes' },
        loadComponent: () =>
          import('./features/services/service-orders.page').then((m) => m.ServiceOrdersPage),
      },
      {
        path: 'servicios/ordenes/:id',
        title: 'Orden de servicio | MEGALUJOS',
        canActivate: [permissionGuard(['services:read'])],
        data: { section: 'Servicios', title: 'Detalle de orden' },
        loadComponent: () =>
          import('./features/services/service-order-detail.page').then(
            (m) => m.ServiceOrderDetailPage,
          ),
      },
      {
        path: 'servicios/agenda',
        title: 'Agenda | MEGALUJOS',
        canActivate: [permissionGuard(['services:read'])],
        data: { section: 'Servicios', title: 'Agenda' },
        loadComponent: () =>
          import('./features/services/agenda.page').then((m) => m.AgendaPage),
      },

      // ----------------------------------------------------------- Facturación
      {
        path: 'facturacion/emisores',
        title: 'Emisores | MEGALUJOS',
        canActivate: [permissionGuard(['invoicing:read'])],
        data: { section: 'Facturación', title: 'Emisores' },
        loadComponent: () =>
          import('./features/invoicing/issuers.page').then((m) => m.IssuersPage),
      },
      {
        path: 'facturacion/documentos',
        title: 'Documentos electrónicos | MEGALUJOS',
        canActivate: [permissionGuard(['invoicing:read'])],
        data: { section: 'Facturación', title: 'Documentos' },
        loadComponent: () =>
          import('./features/invoicing/documents.page').then((m) => m.DocumentsPage),
      },
      {
        path: 'facturacion/configuracion',
        title: 'Configuración de facturación | MEGALUJOS',
        canActivate: [permissionGuard(['invoicing:read'])],
        data: { section: 'Facturación', title: 'Configuración SRI' },
        loadComponent: () =>
          import('./features/invoicing/invoicing-config.page').then(
            (m) => m.InvoicingConfigPage,
          ),
      },

      // -------------------------------------------------------------- Clientes
      {
        path: 'clientes',
        title: 'Clientes | MEGALUJOS',
        canActivate: [permissionGuard(['customers:manage'])],
        data: { section: 'Clientes', title: 'Clientes' },
        loadComponent: () =>
          import('./features/customers/customers.page').then((m) => m.CustomersPage),
      },

      // --------------------------------------------------------------- Compras
      {
        path: 'compras/proveedores',
        title: 'Proveedores | MEGALUJOS',
        canActivate: [permissionGuard(['purchasing:manage'])],
        data: { section: 'Compras', title: 'Proveedores' },
        loadComponent: () =>
          import('./features/purchasing/suppliers.page').then((m) => m.SuppliersPage),
      },
      {
        path: 'compras/ordenes',
        title: 'Órdenes de compra | MEGALUJOS',
        canActivate: [permissionGuard(['purchasing:manage'])],
        data: { section: 'Compras', title: 'Órdenes de compra' },
        loadComponent: () =>
          import('./features/purchasing/purchase-orders.page').then((m) => m.PurchaseOrdersPage),
      },

      // --------------------------------------------------------------- Vitrina
      {
        path: 'vitrina/banners',
        title: 'Banners | MEGALUJOS',
        canActivate: [permissionGuard(['showcase:manage'])],
        data: { section: 'Vitrina', title: 'Banners' },
        loadComponent: () =>
          import('./features/showcase/banners.page').then((m) => m.BannersPage),
      },
      {
        path: 'vitrina/showcases',
        title: 'Showcases | MEGALUJOS',
        canActivate: [permissionGuard(['showcase:manage'])],
        data: { section: 'Vitrina', title: 'Showcases' },
        loadComponent: () =>
          import('./features/showcase/showcases.page').then((m) => m.ShowcasesPage),
      },
      {
        path: 'vitrina/promociones',
        title: 'Promociones | MEGALUJOS',
        canActivate: [permissionGuard(['showcase:manage'])],
        data: { section: 'Vitrina', title: 'Promociones' },
        loadComponent: () =>
          import('./features/showcase/promotions.page').then((m) => m.PromotionsPage),
      },

      // -------------------------------------------------------------- Reportes
      {
        path: 'reportes',
        title: 'Reportes | MEGALUJOS',
        canActivate: [permissionGuard(['reports:view'])],
        data: { section: 'Reportes', title: 'Reportes' },
        loadComponent: () =>
          import('./features/reporting/reports.page').then((m) => m.ReportsPage),
      },

      // ---------------------------------------------------------- Configuración
      {
        path: 'configuracion/usuarios',
        title: 'Usuarios | MEGALUJOS',
        canActivate: [permissionGuard(['users:manage'])],
        data: { section: 'Configuración', title: 'Usuarios' },
        loadComponent: () =>
          import('./features/settings/users.page').then((m) => m.UsersPage),
      },
      {
        path: 'configuracion/roles',
        title: 'Roles | MEGALUJOS',
        canActivate: [permissionGuard(['users:manage'])],
        data: { section: 'Configuración', title: 'Roles' },
        loadComponent: () => import('./features/settings/roles.page').then((m) => m.RolesPage),
      },
      {
        path: 'configuracion/auditoria',
        title: 'Auditoría | MEGALUJOS',
        canActivate: [permissionGuard(['audit:read'])],
        data: { section: 'Configuración', title: 'Auditoría' },
        loadComponent: () =>
          import('./features/settings/audit.page').then((m) => m.AuditPage),
      },
      {
        path: 'configuracion/impresion',
        title: 'Impresión | MEGALUJOS',
        canActivate: [permissionGuard(['print:manage'])],
        data: { section: 'Configuración', title: 'Impresión' },
        loadComponent: () =>
          import('./features/settings/print-jobs.page').then((m) => m.PrintJobsPage),
      },
      { path: '**', redirectTo: 'dashboard' },
    ],
  },
  {
    path: '**',
    title: 'Página no encontrada | MEGALUJOS',
    loadComponent: () =>
      import('./features/errors/not-found.component').then((m) => m.NotFoundComponent),
  },
];
