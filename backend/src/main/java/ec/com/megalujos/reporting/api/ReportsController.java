package ec.com.megalujos.reporting.api;

import ec.com.megalujos.reporting.application.ReportsService;
import ec.com.megalujos.reporting.dto.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/reports")
@Tag(name = "Reportes y Estadísticas", description = "Endpoints para métricas financieras, resumen de ventas y estado operativo")
@PreAuthorize("hasAuthority('reports:view') or hasRole('ADMIN')")
public class ReportsController {

    private final ReportsService reportsService;

    public ReportsController(ReportsService reportsService) {
        this.reportsService = reportsService;
    }

    @GetMapping("/dashboard")
    @Operation(summary = "Métricas principales del Dashboard", description = "Retorna ventas del día, balance de caja y alertas operativas")
    public ResponseEntity<DashboardStatsDto> getDashboardStats() {
        return ResponseEntity.ok(reportsService.getDashboardStats());
    }

    @GetMapping("/daily-sales")
    @Operation(summary = "Ventas diarias", description = "Por día (America/Guayaquil): count, subtotal, impuestos y total")
    public ResponseEntity<List<DailySalesDto>> getDailySales(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo) {
        return ResponseEntity.ok(reportsService.getDailySales(dateFrom, dateTo));
    }

    @GetMapping("/margin")
    @Operation(summary = "Margen bruto estimado", description = "Por producto vendido: cantidad, ingreso, costo (costPrice actual de la variante) y margen estimado; NO es cálculo contable")
    public ResponseEntity<List<MarginDto>> getMargin(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo) {
        return ResponseEntity.ok(reportsService.getMargin(dateFrom, dateTo));
    }

    @GetMapping("/stock-alerts")
    @Operation(summary = "Alertas de stock", description = "Variantes cuya cantidad total en almacenes es menor o igual al stock mínimo")
    public ResponseEntity<List<StockAlertDto>> getStockAlerts() {
        return ResponseEntity.ok(reportsService.getStockAlerts());
    }

    @GetMapping("/sales-by-category")
    @Operation(summary = "Ventas por categoría", description = "Agrega cantidad de ítems y total vendido por categoría de producto")
    public ResponseEntity<List<SalesByCategoryDto>> getSalesByCategory(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo) {
        return ResponseEntity.ok(reportsService.getSalesByCategory(dateFrom, dateTo));
    }

    @GetMapping("/tax-summary")
    @Operation(summary = "Resumen de impuestos", description = "Por tax_code: base imponible y valor de impuesto desde sale_items")
    public ResponseEntity<List<TaxSummaryDto>> getTaxSummary(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo) {
        return ResponseEntity.ok(reportsService.getTaxSummary(dateFrom, dateTo));
    }

    @GetMapping("/pending-documents")
    @Operation(summary = "Comprobantes pendientes", description = "electronic_documents con status distinto de AUTHORIZED, agrupados por status")
    public ResponseEntity<List<PendingDocumentDto>> getPendingDocuments() {
        return ResponseEntity.ok(reportsService.getPendingDocuments());
    }

    @GetMapping("/services-by-status")
    @Operation(summary = "Servicios por estado", description = "Órdenes de servicio: count y total por status")
    public ResponseEntity<List<ServiceStatusDto>> getServicesByStatus() {
        return ResponseEntity.ok(reportsService.getServicesByStatus());
    }

    @GetMapping("/popular-products")
    @Operation(summary = "Productos populares", description = "Top de productos por cantidad vendida en el rango de fechas")
    public ResponseEntity<List<PopularProductDto>> getPopularProducts(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(reportsService.getPopularProducts(dateFrom, dateTo, Math.max(1, limit)));
    }

    @GetMapping("/cash-closings")
    @Operation(summary = "Cierres de caja", description = "Sesiones cerradas en el rango con montos esperado, contado y diferencia")
    public ResponseEntity<List<CashClosingDto>> getCashClosings(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo) {
        return ResponseEntity.ok(reportsService.getCashClosings(dateFrom, dateTo));
    }
}
