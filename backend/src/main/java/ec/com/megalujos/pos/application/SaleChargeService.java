package ec.com.megalujos.pos.application;

import ec.com.megalujos.cash.domain.CashMovement;
import ec.com.megalujos.cash.domain.CashMovementRepository;
import ec.com.megalujos.cash.domain.CashSession;
import ec.com.megalujos.cash.domain.CashSessionRepository;
import ec.com.megalujos.common.exception.BusinessRuleException;
import ec.com.megalujos.common.exception.ResourceNotFoundException;
import ec.com.megalujos.common.tax.TaxRule;
import ec.com.megalujos.common.tax.TaxService;
import ec.com.megalujos.pos.domain.Sale;
import ec.com.megalujos.pos.domain.SaleItem;
import ec.com.megalujos.pos.domain.SalePayment;
import ec.com.megalujos.pos.domain.SaleRepository;
import ec.com.megalujos.pos.dto.ServiceChargeResultDto;
import ec.com.megalujos.pos.dto.ServiceOrderChargeCommand;
import ec.com.megalujos.pos.dto.SaleResponseDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Genera la venta de cobro del saldo de una orden de servicio.
 * Los anticipos se aplican como pagos APPLIED_ADVANCE (una sola vez) y el resto
 * se cobra con los medios de pago recibidos.
 */
@Service
public class SaleChargeService {

    private static final List<String> CHARGE_PAYMENT_METHODS = List.of("CASH", "TRANSFER", "CARD", "OTHER");

    private final SaleRepository saleRepository;
    private final CashSessionRepository cashSessionRepository;
    private final CashMovementRepository cashMovementRepository;
    private final TaxService taxService;

    public SaleChargeService(SaleRepository saleRepository,
                             CashSessionRepository cashSessionRepository,
                             CashMovementRepository cashMovementRepository,
                             TaxService taxService) {
        this.saleRepository = saleRepository;
        this.cashSessionRepository = cashSessionRepository;
        this.cashMovementRepository = cashMovementRepository;
        this.taxService = taxService;
    }

    @Transactional
    public ServiceChargeResultDto charge(ServiceOrderChargeCommand command, UUID idempotencyKey, UUID actorId) {
        // Idempotencia: la misma clave devuelve la venta original sin duplicar cobro
        if (idempotencyKey != null) {
            Optional<Sale> existing = saleRepository.findByIdempotencyKey(idempotencyKey);
            if (existing.isPresent()) {
                ServiceChargeResultDto replay = new ServiceChargeResultDto();
                replay.setSale(toSaleResponse(existing.get(), "ORIGINAL_IDEMPOTENT_RESPONSE"));
                replay.setReplay(true);
                return replay;
            }
        }

        CashSession session = cashSessionRepository.findById(command.getCashSessionId())
                .orElseThrow(() -> new ResourceNotFoundException("Sesión de caja", command.getCashSessionId()));
        if (!"OPEN".equals(session.getStatus())) {
            throw new BusinessRuleException("CASH_SESSION_CLOSED", "La sesión de caja está cerrada. No se puede cobrar la orden.");
        }

        TaxRule taxRule = taxService.resolveDefault();

        Sale sale = new Sale();
        sale.setIdempotencyKey(idempotencyKey != null ? idempotencyKey : UUID.randomUUID());
        sale.setCashSessionId(session.getId());
        sale.setCustomerId(command.getCustomerId());
        sale.setStatus("CONFIRMED");
        sale.setNotes("Cobro de orden de servicio " + command.getOrderNumber());
        sale.setCreatedAt(OffsetDateTime.now());
        String datePrefix = OffsetDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String randomSuffix = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        sale.setSaleNumber("VTA-" + datePrefix + "-" + randomSuffix);

        List<SaleItem> items = new ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal taxTotal = BigDecimal.ZERO;
        BigDecimal grandTotal = BigDecimal.ZERO;

        if (command.getLaborAmount() != null && command.getLaborAmount().compareTo(BigDecimal.ZERO) > 0) {
            SaleItem laborItem = buildLine(sale, "Mano de obra - " + command.getOrderNumber(),
                    BigDecimal.ONE, command.getLaborAmount(), command.getServiceOrderId(), taxRule);
            items.add(laborItem);
            subtotal = subtotal.add(laborItem.getLineTotal().subtract(laborItem.getTaxAmount()));
            taxTotal = taxTotal.add(laborItem.getTaxAmount());
            grandTotal = grandTotal.add(laborItem.getLineTotal());
        }

        for (ServiceOrderChargeCommand.ChargeLine line : command.getLines()) {
            SaleItem item = buildLine(sale, line.getDescription(), line.getQuantity(), line.getUnitPrice(),
                    command.getServiceOrderId(), taxRule);
            items.add(item);
            subtotal = subtotal.add(item.getLineTotal().subtract(item.getTaxAmount()));
            taxTotal = taxTotal.add(item.getTaxAmount());
            grandTotal = grandTotal.add(item.getLineTotal());
        }

        if (items.isEmpty()) {
            throw new BusinessRuleException("EMPTY_SERVICE_CHARGE", "La orden no tiene líneas para cobrar");
        }

        sale.setSubtotal(subtotal);
        sale.setTaxTotal(taxTotal);
        sale.setGrandTotal(grandTotal);
        sale.setItems(items);

        // 1. Anticipos disponibles (aplicados una sola vez, nunca más de su monto)
        BigDecimal advanceToApply = BigDecimal.ZERO;
        for (ServiceOrderChargeCommand.AdvanceInfo advance : command.getAdvances()) {
            advanceToApply = advanceToApply.add(advanceCapacity(advance));
        }
        if (advanceToApply.compareTo(grandTotal) > 0) {
            advanceToApply = grandTotal;
        }

        // 2. Pagos recibidos: deben cubrir al menos el saldo restante (el exceso en efectivo es cambio)
        BigDecimal paymentsReceived = BigDecimal.ZERO;
        BigDecimal cashReceived = BigDecimal.ZERO;
        BigDecimal nonCashReceived = BigDecimal.ZERO;
        String cashReference = null;
        List<SalePayment> payments = new ArrayList<>();

        for (ServiceOrderChargeCommand.ChargePayment paymentReq : command.getPayments()) {
            if (paymentReq.getAmount() == null || paymentReq.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
                throw new BusinessRuleException("INVALID_PAYMENT", "El monto de pago debe ser mayor a cero");
            }
            String method = paymentReq.getMethod() != null ? paymentReq.getMethod().trim().toUpperCase() : "";
            if (!CHARGE_PAYMENT_METHODS.contains(method)) {
                throw new BusinessRuleException("INVALID_PAYMENT_METHOD", "Medio de pago no válido: " + paymentReq.getMethod());
            }

            paymentsReceived = paymentsReceived.add(paymentReq.getAmount());
            if ("CASH".equals(method)) {
                cashReceived = cashReceived.add(paymentReq.getAmount());
                if (cashReference == null) {
                    cashReference = paymentReq.getReference();
                }
                continue;
            }

            SalePayment payment = new SalePayment();
            payment.setSale(sale);
            payment.setMethod(method);
            payment.setAmount(paymentReq.getAmount());
            payment.setReference(paymentReq.getReference());
            payments.add(payment);
            nonCashReceived = nonCashReceived.add(paymentReq.getAmount());
        }

        BigDecimal requiredFromPayments = grandTotal.subtract(advanceToApply);
        if (paymentsReceived.compareTo(requiredFromPayments) < 0) {
            throw new BusinessRuleException("INSUFFICIENT_PAYMENT",
                    String.format("Los pagos recibidos (%s) son menores al saldo por cobrar (%s)",
                            paymentsReceived, requiredFromPayments));
        }

        // Efectivo realmente retenido en caja: lo entregado de más se devuelve como cambio
        BigDecimal cashPortion = cashReceived.min(requiredFromPayments.subtract(nonCashReceived).max(BigDecimal.ZERO));
        if (cashPortion.compareTo(BigDecimal.ZERO) > 0) {
            SalePayment cashPayment = new SalePayment();
            cashPayment.setSale(sale);
            cashPayment.setMethod("CASH");
            cashPayment.setAmount(cashPortion);
            cashPayment.setReference(cashReference);
            payments.add(cashPayment);
        }

        // 3. Anticipos aplicados como pagos de la venta
        List<ServiceChargeResultDto.AppliedAdvanceDto> allocations = new ArrayList<>();
        BigDecimal appliedLeft = advanceToApply;
        for (ServiceOrderChargeCommand.AdvanceInfo advance : command.getAdvances()) {
            BigDecimal capacity = advanceCapacity(advance);
            if (capacity.compareTo(BigDecimal.ZERO) <= 0 || appliedLeft.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            BigDecimal applied = capacity.min(appliedLeft);
            SalePayment payment = new SalePayment();
            payment.setSale(sale);
            payment.setMethod("APPLIED_ADVANCE");
            payment.setAmount(applied);
            payment.setReference("ANTICIPO " + command.getOrderNumber());
            payments.add(payment);

            allocations.add(new ServiceChargeResultDto.AppliedAdvanceDto(advance.getId(), applied));
            appliedLeft = appliedLeft.subtract(applied);
        }

        sale.setPayments(payments);
        Sale saved = saleRepository.save(sale);

        if (cashPortion.compareTo(BigDecimal.ZERO) > 0) {
            CashMovement movement = new CashMovement();
            movement.setSessionId(session.getId());
            movement.setMovementType("SALE_PAYMENT");
            movement.setAmount(cashPortion);
            movement.setSourceType("SERVICE_ORDER");
            movement.setSourceId(command.getServiceOrderId());
            movement.setActorId(actorId);
            movement.setReason("Cobro de orden " + command.getOrderNumber() + " venta #" + saved.getSaleNumber());
            cashMovementRepository.save(movement);
        }

        ServiceChargeResultDto result = new ServiceChargeResultDto();
        result.setSale(toSaleResponse(saved, "CONFIRMED"));
        result.setAppliedAdvances(allocations);
        result.setReplay(false);
        return result;
    }

    private BigDecimal advanceCapacity(ServiceOrderChargeCommand.AdvanceInfo advance) {
        BigDecimal applied = advance.getAppliedAmount() != null ? advance.getAppliedAmount() : BigDecimal.ZERO;
        BigDecimal capacity = advance.getAmount().subtract(applied);
        return capacity.compareTo(BigDecimal.ZERO) > 0 ? capacity : BigDecimal.ZERO;
    }

    private SaleItem buildLine(Sale sale, String description, BigDecimal quantity, BigDecimal unitPrice,
                               UUID serviceOrderId, TaxRule taxRule) {
        BigDecimal base = unitPrice.multiply(quantity).setScale(2, RoundingMode.HALF_UP);
        BigDecimal taxAmount = taxService.taxAmount(base, taxRule.getRate());
        BigDecimal lineTotal = base.add(taxAmount).setScale(2, RoundingMode.HALF_UP);

        SaleItem item = new SaleItem();
        item.setSale(sale);
        item.setVariantId(null);
        item.setServiceOrderId(serviceOrderId);
        item.setDescription(description);
        item.setQuantity(quantity);
        item.setUnitPrice(unitPrice);
        item.setDiscount(BigDecimal.ZERO);
        item.setTaxCode(taxRule.getCode());
        item.setTaxRate(taxRule.getRate());
        item.setTaxAmount(taxAmount);
        item.setLineTotal(lineTotal);
        return item;
    }

    private SaleResponseDto toSaleResponse(Sale sale, String invoiceStatus) {
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
        for (SaleItem item : sale.getItems()) {
            SaleResponseDto.SaleItemResponseDto itemDto = new SaleResponseDto.SaleItemResponseDto();
            itemDto.setId(item.getId());
            itemDto.setDescription(item.getDescription());
            itemDto.setQuantity(item.getQuantity());
            itemDto.setUnitPrice(item.getUnitPrice());
            itemDto.setDiscount(item.getDiscount());
            itemDto.setTaxAmount(item.getTaxAmount());
            itemDto.setLineTotal(item.getLineTotal());
            itemDtos.add(itemDto);
        }
        dto.setItems(itemDtos);

        List<SaleResponseDto.SalePaymentResponseDto> paymentDtos = new ArrayList<>();
        for (SalePayment payment : sale.getPayments()) {
            paymentDtos.add(new SaleResponseDto.SalePaymentResponseDto(
                    payment.getMethod(), payment.getAmount(), payment.getReference()));
        }
        dto.setPayments(paymentDtos);
        return dto;
    }
}
