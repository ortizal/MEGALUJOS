package ec.com.megalujos.pos.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import ec.com.megalujos.catalog.domain.Product;
import ec.com.megalujos.catalog.domain.ProductVariant;
import ec.com.megalujos.catalog.domain.ProductVariantRepository;
import ec.com.megalujos.cash.domain.CashMovement;
import ec.com.megalujos.cash.domain.CashMovementRepository;
import ec.com.megalujos.cash.domain.CashSession;
import ec.com.megalujos.cash.domain.CashSessionRepository;
import ec.com.megalujos.common.exception.BusinessRuleException;
import ec.com.megalujos.common.exception.ResourceNotFoundException;
import ec.com.megalujos.common.tax.TaxRule;
import ec.com.megalujos.common.tax.TaxService;
import ec.com.megalujos.inventory.application.StockService;
import ec.com.megalujos.invoicing.domain.OutboxEvent;
import ec.com.megalujos.invoicing.domain.OutboxEventRepository;
import ec.com.megalujos.pos.domain.Sale;
import ec.com.megalujos.pos.domain.SaleItem;
import ec.com.megalujos.pos.domain.SalePayment;
import ec.com.megalujos.pos.domain.SaleRepository;
import ec.com.megalujos.pos.dto.ConfirmSaleRequest;
import ec.com.megalujos.pos.dto.SaleResponseDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class SaleService {

    private final SaleRepository saleRepository;
    private final ProductVariantRepository variantRepository;
    private final StockService stockService;
    private final CashSessionRepository cashSessionRepository;
    private final CashMovementRepository cashMovementRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final TaxService taxService;
    private final ObjectMapper objectMapper;

    public SaleService(SaleRepository saleRepository,
                       ProductVariantRepository variantRepository,
                       StockService stockService,
                       CashSessionRepository cashSessionRepository,
                       CashMovementRepository cashMovementRepository,
                       OutboxEventRepository outboxEventRepository,
                       TaxService taxService,
                       ObjectMapper objectMapper) {
        this.saleRepository = saleRepository;
        this.variantRepository = variantRepository;
        this.stockService = stockService;
        this.cashSessionRepository = cashSessionRepository;
        this.cashMovementRepository = cashMovementRepository;
        this.outboxEventRepository = outboxEventRepository;
        this.taxService = taxService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public SaleResponseDto confirmSale(ConfirmSaleRequest request, UUID idempotencyKey, UUID currentUserId) {
        // 1. Control estricto de idempotencia: Si ya fue procesada, devolver la venta previa sin duplicar
        Optional<Sale> existingSale = saleRepository.findByIdempotencyKey(idempotencyKey);
        if (existingSale.isPresent()) {
            return mapToResponseDto(existingSale.get(), "ORIGINAL_IDEMPOTENT_RESPONSE");
        }

        // 2. Verificar que la sesión de caja exista y esté abierta
        CashSession session = cashSessionRepository.findById(request.getCashSessionId())
                .orElseThrow(() -> new ResourceNotFoundException("Sesión de caja", request.getCashSessionId()));

        if (!"OPEN".equalsIgnoreCase(session.getStatus())) {
            throw new BusinessRuleException("CASH_SESSION_CLOSED", "La sesión de caja está cerrada. No se pueden registrar ventas.");
        }

        // 3. Crear cabecera de la venta
        Sale sale = new Sale();
        sale.setIdempotencyKey(idempotencyKey);
        sale.setCashSessionId(session.getId());
        sale.setCustomerId(request.getCustomerId());
        sale.setStatus("CONFIRMED");
        sale.setNotes(request.getNotes());
        sale.setCreatedAt(OffsetDateTime.now());

        String datePrefix = OffsetDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String randomSuffix = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        sale.setSaleNumber("VTA-" + datePrefix + "-" + randomSuffix);

        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal discountTotal = BigDecimal.ZERO;
        BigDecimal taxTotal = BigDecimal.ZERO;
        BigDecimal grandTotal = BigDecimal.ZERO;

        List<SaleItem> items = new ArrayList<>();

        // 4. Procesar cada ítem: verificar precio oficial, stock disponible y recalcular
        for (ConfirmSaleRequest.SaleItemRequest itemReq : request.getItems()) {
            ProductVariant variant = variantRepository.findById(itemReq.getVariantId())
                    .orElseThrow(() -> new ResourceNotFoundException("Variante de producto", itemReq.getVariantId()));

            Product product = variant.getProduct();
            if (!product.isActive() || !variant.isActive()) {
                throw new BusinessRuleException("INACTIVE_PRODUCT", "El producto " + product.getName() + " está inactivo");
            }

            BigDecimal unitPrice = variant.getSalePrice();
            BigDecimal quantity = itemReq.getQuantity();
            BigDecimal discount = itemReq.getDiscount() != null ? itemReq.getDiscount() : BigDecimal.ZERO;
            if (discount.compareTo(BigDecimal.ZERO) < 0) {
                throw new BusinessRuleException("INVALID_DISCOUNT", "El descuento no puede ser negativo");
            }

            BigDecimal lineBase = unitPrice.multiply(quantity).subtract(discount);
            if (lineBase.compareTo(BigDecimal.ZERO) < 0) {
                lineBase = BigDecimal.ZERO;
            }

            // Tarifa tributaria vigente resuelta en BD (nunca tasas fijas en código)
            TaxRule taxRule = taxService.resolveDefault();
            BigDecimal taxAmount = taxService.taxAmount(lineBase, taxRule.getRate());
            BigDecimal lineTotal = lineBase.add(taxAmount).setScale(2, RoundingMode.HALF_UP);

            SaleItem item = new SaleItem();
            item.setSale(sale);
            item.setVariantId(variant.getId());
            item.setDescription(product.getName() + " - " + variant.getSku());
            item.setQuantity(quantity);
            item.setUnitPrice(unitPrice);
            item.setDiscount(discount);
            item.setTaxCode(taxRule.getCode());
            item.setTaxRate(taxRule.getRate());
            item.setTaxAmount(taxAmount);
            item.setLineTotal(lineTotal);
            items.add(item);

            subtotal = subtotal.add(lineBase);
            discountTotal = discountTotal.add(discount);
            taxTotal = taxTotal.add(taxAmount);
            grandTotal = grandTotal.add(lineTotal);

            // Descontar inventario atómicamente si el producto maneja stock
            if (product.isTrackStock() && !"SERVICE".equalsIgnoreCase(product.getProductType())) {
                stockService.deductStock(
                        variant.getId(),
                        null,
                        quantity,
                        "SALE",
                        "POS_SALE",
                        sale.getId() != null ? sale.getId() : idempotencyKey,
                        null,
                        currentUserId,
                        "Venta POS #" + sale.getSaleNumber()
                );
            }
        }

        sale.setSubtotal(subtotal);
        sale.setDiscountTotal(discountTotal);
        sale.setTaxTotal(taxTotal);
        sale.setGrandTotal(grandTotal);
        sale.setItems(items);

        // 5. Validar medios de pago y registrar pagos
        BigDecimal totalPayments = BigDecimal.ZERO;
        List<SalePayment> payments = new ArrayList<>();
        BigDecimal cashPortion = BigDecimal.ZERO;

        for (ConfirmSaleRequest.SalePaymentRequest payReq : request.getPayments()) {
            if (payReq.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
                throw new BusinessRuleException("INVALID_PAYMENT", "El monto de pago debe ser mayor a cero");
            }

            SalePayment payment = new SalePayment();
            payment.setSale(sale);
            payment.setMethod(payReq.getMethod());
            payment.setAmount(payReq.getAmount());
            payment.setReference(payReq.getReference());
            payments.add(payment);

            totalPayments = totalPayments.add(payReq.getAmount());

            if ("CASH".equalsIgnoreCase(payReq.getMethod())) {
                cashPortion = cashPortion.add(payReq.getAmount());
            }
        }

        if (totalPayments.compareTo(grandTotal) < 0) {
            throw new BusinessRuleException("INSUFFICIENT_PAYMENT",
                    String.format("El pago total (%s) es menor al monto a pagar (%s)", totalPayments, grandTotal));
        }

        sale.setPayments(payments);
        Sale savedSale = saleRepository.save(sale);

        // 6. Registrar movimiento de caja física para la porción en efectivo
        if (cashPortion.compareTo(BigDecimal.ZERO) > 0) {
            CashMovement cashMovement = new CashMovement();
            cashMovement.setSessionId(session.getId());
            cashMovement.setMovementType("SALE_PAYMENT");
            cashMovement.setAmount(cashPortion);
            cashMovement.setSourceType("POS_SALE");
            cashMovement.setSourceId(savedSale.getId());
            cashMovement.setActorId(currentUserId);
            cashMovement.setReason("Cobro de Venta #" + savedSale.getSaleNumber());
            cashMovementRepository.save(cashMovement);
        }

        // 7. Si se solicitó factura electrónica SRI, registrar evento transaccional en outbox
        String invoiceStatus = "NOT_REQUESTED";
        if (request.isRequestInvoice()) {
            try {
                Map<String, Object> outboxPayload = new HashMap<>();
                outboxPayload.put("saleId", savedSale.getId());
                outboxPayload.put("saleNumber", savedSale.getSaleNumber());
                outboxPayload.put("grandTotal", savedSale.getGrandTotal());

                OutboxEvent outboxEvent = new OutboxEvent(
                        "INVOICE_REQUESTED",
                        savedSale.getId(),
                        objectMapper.writeValueAsString(outboxPayload)
                );
                outboxEventRepository.save(outboxEvent);
                invoiceStatus = "QUEUED_SRI";
            } catch (Exception ex) {
                invoiceStatus = "ERROR_QUEUEING_SRI";
            }
        }

        return mapToResponseDto(savedSale, invoiceStatus);
    }

    @Transactional(readOnly = true)
    public SaleResponseDto getSaleDetail(UUID saleId) {
        Sale sale = saleRepository.findById(saleId)
                .orElseThrow(() -> new ResourceNotFoundException("Venta", saleId));
        return mapToResponseDto(sale, "CONFIRMED");
    }

    private SaleResponseDto mapToResponseDto(Sale sale, String invoiceStatus) {
        SaleResponseDto dto = new SaleResponseDto();
        dto.setSaleId(sale.getId());
        dto.setSaleNumber(sale.getSaleNumber());
        dto.setStatus(sale.getStatus());
        dto.setSubtotal(sale.getSubtotal());
        dto.setDiscountTotal(sale.getDiscountTotal());
        dto.setTaxTotal(sale.getTaxTotal());
        dto.setGrandTotal(sale.getGrandTotal());
        dto.setIdempotencyKey(sale.getIdempotencyKey());
        dto.setCreatedAt(sale.getCreatedAt());
        dto.setInvoiceStatus(invoiceStatus);

        List<SaleResponseDto.SaleItemResponseDto> itemDtos = new ArrayList<>();
        if (sale.getItems() != null) {
            for (SaleItem item : sale.getItems()) {
                SaleResponseDto.SaleItemResponseDto idto = new SaleResponseDto.SaleItemResponseDto();
                idto.setId(item.getId());
                idto.setDescription(item.getDescription());
                idto.setQuantity(item.getQuantity());
                idto.setUnitPrice(item.getUnitPrice());
                idto.setDiscount(item.getDiscount());
                idto.setTaxAmount(item.getTaxAmount());
                idto.setLineTotal(item.getLineTotal());
                itemDtos.add(idto);
            }
        }
        dto.setItems(itemDtos);

        List<SaleResponseDto.SalePaymentResponseDto> paymentDtos = new ArrayList<>();
        if (sale.getPayments() != null) {
            for (SalePayment pay : sale.getPayments()) {
                paymentDtos.add(new SaleResponseDto.SalePaymentResponseDto(
                        pay.getMethod(), pay.getAmount(), pay.getReference()
                ));
            }
        }
        dto.setPayments(paymentDtos);

        return dto;
    }
}
