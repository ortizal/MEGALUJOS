package ec.com.megalujos.reporting.application;

import ec.com.megalujos.cash.domain.CashMovementRepository;
import ec.com.megalujos.cash.domain.CashSession;
import ec.com.megalujos.cash.domain.CashSessionRepository;
import ec.com.megalujos.catalog.domain.ProductVariant;
import ec.com.megalujos.catalog.domain.ProductVariantRepository;
import ec.com.megalujos.pos.domain.Sale;
import ec.com.megalujos.pos.domain.SaleRepository;
import ec.com.megalujos.reporting.dto.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.*;

@Service
public class ReportsService {

    private static final ZoneId ECUADOR = ZoneId.of("America/Guayaquil");

    private final SaleRepository saleRepository;
    private final CashSessionRepository cashSessionRepository;
    private final CashMovementRepository cashMovementRepository;
    private final ProductVariantRepository variantRepository;

    @PersistenceContext
    private EntityManager entityManager;

    public ReportsService(SaleRepository saleRepository,
                          CashSessionRepository cashSessionRepository,
                          CashMovementRepository cashMovementRepository,
                          ProductVariantRepository variantRepository) {
        this.saleRepository = saleRepository;
        this.cashSessionRepository = cashSessionRepository;
        this.cashMovementRepository = cashMovementRepository;
        this.variantRepository = variantRepository;
    }

    @Transactional(readOnly = true)
    public DashboardStatsDto getDashboardStats() {
        DashboardStatsDto dto = new DashboardStatsDto();

        OffsetDateTime startOfDay = LocalDate.now().atStartOfDay(ECUADOR).toOffsetDateTime();

        dto.setTotalSalesTodayCount(saleRepository.countSalesSince(startOfDay));
        dto.setTotalSalesTodayAmount(saleRepository.sumTotalSalesSince(startOfDay));

        List<CashSession> openSessions = cashSessionRepository.findByStatusOrderByOpenedAtDesc("OPEN");
        dto.setOpenCashSessionsCount(openSessions.size());

        if (!openSessions.isEmpty()) {
            CashSession latestSession = openSessions.get(0);
            BigDecimal income = cashMovementRepository.sumIncomeBySessionId(latestSession.getId());
            BigDecimal expense = cashMovementRepository.sumExpenseBySessionId(latestSession.getId());
            BigDecimal balance = latestSession.getOpeningAmount().add(income).subtract(expense);
            dto.setCurrentCashSessionBalance(balance);
        } else {
            dto.setCurrentCashSessionBalance(BigDecimal.ZERO);
        }

        dto.setLowStockProductsCount(countLowStockVariants());

        return dto;
    }

    /** Ventas por día (America/Guayaquil): count, subtotal, tax y total. */
    @Transactional(readOnly = true)
    public List<DailySalesDto> getDailySales(LocalDate dateFrom, LocalDate dateTo) {
        OffsetDateTime from = dateFrom.atStartOfDay(ECUADOR).toOffsetDateTime();
        OffsetDateTime to = dateTo.plusDays(1).atStartOfDay(ECUADOR).toOffsetDateTime();

        List<Sale> sales = entityManager.createQuery(
                        "SELECT s FROM Sale s WHERE s.createdAt >= :from AND s.createdAt < :to "
                                + "AND s.status IN ('CONFIRMED', 'PARTIALLY_RETURNED')", Sale.class)
                .setParameter("from", from)
                .setParameter("to", to)
                .getResultList();

        Map<LocalDate, DailySalesDto> byDay = new TreeMap<>();
        for (Sale sale : sales) {
            LocalDate day = sale.getCreatedAt().atZoneSameInstant(ECUADOR).toLocalDate();
            DailySalesDto dto = byDay.computeIfAbsent(day, d -> {
                DailySalesDto created = new DailySalesDto();
                created.setDate(d.toString());
                return created;
            });
            dto.setCount(dto.getCount() + 1);
            dto.setSubtotal(dto.getSubtotal().add(nz(sale.getSubtotal())));
            dto.setTax(dto.getTax().add(nz(sale.getTaxTotal())));
            dto.setTotal(dto.getTotal().add(nz(sale.getGrandTotal())));
        }
        return new ArrayList<>(byDay.values());
    }

    /**
     * Margen bruto ESTIMADO (no contable): el costo unitario es el costPrice
     * actual de la variante, no el costo histórico del movimiento de inventario.
     */
    @Transactional(readOnly = true)
    public List<MarginDto> getMargin(LocalDate dateFrom, LocalDate dateTo) {
        OffsetDateTime from = dateFrom.atStartOfDay(ECUADOR).toOffsetDateTime();
        OffsetDateTime to = dateTo.plusDays(1).atStartOfDay(ECUADOR).toOffsetDateTime();

        List<Object[]> rows = entityManager.createQuery(
                        "SELECT si.variantId, COALESCE(SUM(si.quantity), 0), COALESCE(SUM(si.lineTotal), 0) "
                                + "FROM SaleItem si JOIN si.sale s "
                                + "WHERE si.variantId IS NOT NULL AND s.createdAt >= :from AND s.createdAt < :to "
                                + "AND s.status IN ('CONFIRMED', 'PARTIALLY_RETURNED') "
                                + "GROUP BY si.variantId", Object[].class)
                .setParameter("from", from)
                .setParameter("to", to)
                .getResultList();

        Set<UUID> variantIds = new HashSet<>();
        for (Object[] row : rows) {
            variantIds.add((UUID) row[0]);
        }
        Map<UUID, ProductVariant> variants = new HashMap<>();
        variantRepository.findAllById(variantIds).forEach(v -> variants.put(v.getId(), v));

        List<MarginDto> result = new ArrayList<>();
        for (Object[] row : rows) {
            UUID variantId = (UUID) row[0];
            BigDecimal quantity = (BigDecimal) row[1];
            BigDecimal revenue = (BigDecimal) row[2];
            ProductVariant variant = variants.get(variantId);
            BigDecimal unitCost = variant != null && variant.getCostPrice() != null
                    ? variant.getCostPrice() : BigDecimal.ZERO;
            BigDecimal cost = quantity.multiply(unitCost).setScale(2, RoundingMode.HALF_UP);
            MarginDto dto = new MarginDto();
            dto.setVariantId(variantId);
            if (variant != null) {
                dto.setSku(variant.getSku());
                dto.setProductName(variant.getProduct() != null ? variant.getProduct().getName() : null);
            }
            dto.setQuantity(quantity);
            dto.setRevenue(revenue.setScale(2, RoundingMode.HALF_UP));
            dto.setCost(cost);
            dto.setEstimatedMargin(revenue.setScale(2, RoundingMode.HALF_UP).subtract(cost));
            result.add(dto);
        }
        return result;
    }

    /** Variantes con cantidad total en almacenes <= min_stock (lectura de stock_balances). */
    @Transactional(readOnly = true)
    public List<StockAlertDto> getStockAlerts() {
        Query query = entityManager.createNativeQuery(
                "SELECT v.id, v.sku, p.name, v.min_stock, COALESCE(SUM(b.quantity), 0) AS qty "
                        + "FROM product_variants v "
                        + "JOIN products p ON p.id = v.product_id "
                        + "LEFT JOIN stock_balances b ON b.variant_id = v.id "
                        + "WHERE v.active = true "
                        + "GROUP BY v.id, v.sku, p.name, v.min_stock "
                        + "HAVING COALESCE(SUM(b.quantity), 0) <= v.min_stock "
                        + "ORDER BY p.name, v.sku");
        List<StockAlertDto> result = new ArrayList<>();
        for (Object row : query.getResultList()) {
            Object[] cols = (Object[]) row;
            StockAlertDto dto = new StockAlertDto();
            dto.setVariantId((UUID) cols[0]);
            dto.setSku((String) cols[1]);
            dto.setProductName((String) cols[2]);
            dto.setMinStock(toBigDecimal(cols[3]));
            dto.setQuantity(toBigDecimal(cols[4]));
            result.add(dto);
        }
        return result;
    }

    /** Ventas por categoría (agrupación en Java: SaleItem solo guarda variantId). */
    @Transactional(readOnly = true)
    public List<SalesByCategoryDto> getSalesByCategory(LocalDate dateFrom, LocalDate dateTo) {
        OffsetDateTime from = dateFrom.atStartOfDay(ECUADOR).toOffsetDateTime();
        OffsetDateTime to = dateTo.plusDays(1).atStartOfDay(ECUADOR).toOffsetDateTime();

        List<Object[]> rows = entityManager.createQuery(
                        "SELECT si.variantId, COALESCE(SUM(si.quantity), 0), COALESCE(SUM(si.lineTotal), 0) "
                                + "FROM SaleItem si JOIN si.sale s "
                                + "WHERE s.createdAt >= :from AND s.createdAt < :to "
                                + "AND s.status IN ('CONFIRMED', 'PARTIALLY_RETURNED') "
                                + "GROUP BY si.variantId", Object[].class)
                .setParameter("from", from)
                .setParameter("to", to)
                .getResultList();

        Set<UUID> variantIds = new HashSet<>();
        for (Object[] row : rows) {
            if (row[0] != null) {
                variantIds.add((UUID) row[0]);
            }
        }
        Map<UUID, String> categoryByVariant = new HashMap<>();
        variantRepository.findAllById(variantIds).forEach(v -> {
            String categoryName = v.getProduct() != null && v.getProduct().getCategory() != null
                    ? v.getProduct().getCategory().getName() : "Sin categoría";
            categoryByVariant.put(v.getId(), categoryName);
        });

        Map<String, SalesByCategoryDto> byCategory = new TreeMap<>();
        for (Object[] row : rows) {
            UUID variantId = (UUID) row[0];
            String category = variantId != null
                    ? categoryByVariant.getOrDefault(variantId, "Sin categoría") : "Servicios";
            SalesByCategoryDto dto = byCategory.computeIfAbsent(category,
                    key -> new SalesByCategoryDto(key, 0, BigDecimal.ZERO));
            dto.setCount(dto.getCount() + ((Number) row[1]).longValue());
            dto.setTotal(dto.getTotal().add((BigDecimal) row[2]));
        }
        return new ArrayList<>(byCategory.values());
    }

    /** Resumen de impuestos por tax_code desde sale_items (base imponible y valor). */
    @Transactional(readOnly = true)
    public List<TaxSummaryDto> getTaxSummary(LocalDate dateFrom, LocalDate dateTo) {
        OffsetDateTime from = dateFrom.atStartOfDay(ECUADOR).toOffsetDateTime();
        OffsetDateTime to = dateTo.plusDays(1).atStartOfDay(ECUADOR).toOffsetDateTime();

        List<Object[]> rows = entityManager.createQuery(
                        "SELECT si.taxCode, COALESCE(SUM(si.lineTotal - si.taxAmount), 0), COALESCE(SUM(si.taxAmount), 0) "
                                + "FROM SaleItem si JOIN si.sale s "
                                + "WHERE s.createdAt >= :from AND s.createdAt < :to "
                                + "AND s.status IN ('CONFIRMED', 'PARTIALLY_RETURNED') "
                                + "GROUP BY si.taxCode", Object[].class)
                .setParameter("from", from)
                .setParameter("to", to)
                .getResultList();

        List<TaxSummaryDto> result = new ArrayList<>();
        for (Object[] row : rows) {
            result.add(new TaxSummaryDto((String) row[0], (BigDecimal) row[1], (BigDecimal) row[2]));
        }
        return result;
    }

    /** Comprobantes electrónicos que no están AUTHORIZED, agrupados por status. */
    @Transactional(readOnly = true)
    public List<PendingDocumentDto> getPendingDocuments() {
        Query query = entityManager.createNativeQuery(
                "SELECT status, COUNT(*) FROM electronic_documents WHERE status <> 'AUTHORIZED' GROUP BY status ORDER BY status");
        List<PendingDocumentDto> result = new ArrayList<>();
        for (Object row : query.getResultList()) {
            Object[] cols = (Object[]) row;
            result.add(new PendingDocumentDto((String) cols[0], ((Number) cols[1]).longValue()));
        }
        return result;
    }

    /** Órdenes de servicio: count y total (estimated_total) por status. */
    @Transactional(readOnly = true)
    public List<ServiceStatusDto> getServicesByStatus() {
        Query query = entityManager.createNativeQuery(
                "SELECT status, COUNT(*), COALESCE(SUM(estimated_total), 0) FROM service_orders GROUP BY status ORDER BY status");
        List<ServiceStatusDto> result = new ArrayList<>();
        for (Object row : query.getResultList()) {
            Object[] cols = (Object[]) row;
            result.add(new ServiceStatusDto((String) cols[0], ((Number) cols[1]).longValue(), toBigDecimal(cols[2])));
        }
        return result;
    }

    /** Top de productos por cantidad vendida en el rango. */
    @Transactional(readOnly = true)
    public List<PopularProductDto> getPopularProducts(LocalDate dateFrom, LocalDate dateTo, int limit) {
        OffsetDateTime from = dateFrom.atStartOfDay(ECUADOR).toOffsetDateTime();
        OffsetDateTime to = dateTo.plusDays(1).atStartOfDay(ECUADOR).toOffsetDateTime();

        List<Object[]> rows = entityManager.createQuery(
                        "SELECT si.description, COALESCE(SUM(si.quantity), 0), COALESCE(SUM(si.lineTotal), 0) "
                                + "FROM SaleItem si JOIN si.sale s "
                                + "WHERE s.createdAt >= :from AND s.createdAt < :to "
                                + "AND s.status IN ('CONFIRMED', 'PARTIALLY_RETURNED') "
                                + "GROUP BY si.description ORDER BY SUM(si.quantity) DESC", Object[].class)
                .setParameter("from", from)
                .setParameter("to", to)
                .getResultList();

        List<PopularProductDto> result = new ArrayList<>();
        for (Object[] row : rows) {
            if (result.size() >= limit) {
                break;
            }
            result.add(new PopularProductDto((String) row[0], (BigDecimal) row[1], (BigDecimal) row[2]));
        }
        return result;
    }

    /** Sesiones de caja cerradas en el rango con expected, counted y difference. */
    @Transactional(readOnly = true)
    public List<CashClosingDto> getCashClosings(LocalDate dateFrom, LocalDate dateTo) {
        OffsetDateTime from = dateFrom.atStartOfDay(ECUADOR).toOffsetDateTime();
        OffsetDateTime to = dateTo.plusDays(1).atStartOfDay(ECUADOR).toOffsetDateTime();

        List<CashClosingDto> result = new ArrayList<>();
        for (CashSession session : cashSessionRepository.findAll()) {
            if (!"CLOSED".equals(session.getStatus()) || session.getClosedAt() == null) {
                continue;
            }
            if (session.getClosedAt().isBefore(from) || !session.getClosedAt().isBefore(to)) {
                continue;
            }
            CashClosingDto dto = new CashClosingDto();
            dto.setSessionId(session.getId());
            dto.setRegisterCode(session.getRegister() != null ? session.getRegister().getCode() : null);
            dto.setOpenedAt(session.getOpenedAt());
            dto.setClosedAt(session.getClosedAt());
            dto.setOpeningAmount(nz(session.getOpeningAmount()));
            dto.setExpectedAmount(nz(session.getExpectedAmount()));
            dto.setCountedAmount(nz(session.getCountedAmount()));
            dto.setDifference(nz(session.getDifferenceAmount()));
            result.add(dto);
        }
        return result;
    }

    private long countLowStockVariants() {
        Query query = entityManager.createNativeQuery(
                "SELECT COUNT(*) FROM ("
                        + "SELECT v.id FROM product_variants v "
                        + "LEFT JOIN stock_balances b ON b.variant_id = v.id "
                        + "WHERE v.active = true "
                        + "GROUP BY v.id, v.min_stock "
                        + "HAVING COALESCE(SUM(b.quantity), 0) <= v.min_stock) t");
        return ((Number) query.getSingleResult()).longValue();
    }

    private BigDecimal nz(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    private BigDecimal toBigDecimal(Object value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        if (value instanceof BigDecimal bd) {
            return bd;
        }
        return new BigDecimal(value.toString());
    }
}
