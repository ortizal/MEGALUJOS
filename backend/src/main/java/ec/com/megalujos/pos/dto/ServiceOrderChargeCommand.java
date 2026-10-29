package ec.com.megalujos.pos.dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Datos que el módulo de servicios pasa a POS para cobrar el saldo de una orden.
 * Mantiene la dependencia servicios → aplicación de POS (nunca entity-to-entity).
 */
public class ServiceOrderChargeCommand {

    private UUID serviceOrderId;
    private String orderNumber;
    private UUID customerId;
    private UUID cashSessionId;
    private BigDecimal laborAmount = BigDecimal.ZERO;
    private List<ChargeLine> lines = new ArrayList<>();
    private List<AdvanceInfo> advances = new ArrayList<>();
    private List<ChargePayment> payments = new ArrayList<>();

    public ServiceOrderChargeCommand() {
    }

    public UUID getServiceOrderId() {
        return serviceOrderId;
    }

    public void setServiceOrderId(UUID serviceOrderId) {
        this.serviceOrderId = serviceOrderId;
    }

    public String getOrderNumber() {
        return orderNumber;
    }

    public void setOrderNumber(String orderNumber) {
        this.orderNumber = orderNumber;
    }

    public UUID getCustomerId() {
        return customerId;
    }

    public void setCustomerId(UUID customerId) {
        this.customerId = customerId;
    }

    public UUID getCashSessionId() {
        return cashSessionId;
    }

    public void setCashSessionId(UUID cashSessionId) {
        this.cashSessionId = cashSessionId;
    }

    public BigDecimal getLaborAmount() {
        return laborAmount;
    }

    public void setLaborAmount(BigDecimal laborAmount) {
        this.laborAmount = laborAmount;
    }

    public List<ChargeLine> getLines() {
        return lines;
    }

    public void setLines(List<ChargeLine> lines) {
        this.lines = lines;
    }

    public List<AdvanceInfo> getAdvances() {
        return advances;
    }

    public void setAdvances(List<AdvanceInfo> advances) {
        this.advances = advances;
    }

    public List<ChargePayment> getPayments() {
        return payments;
    }

    public void setPayments(List<ChargePayment> payments) {
        this.payments = payments;
    }

    public static class ChargeLine {

        private String description;
        private BigDecimal quantity;
        private BigDecimal unitPrice;

        public ChargeLine() {
        }

        public ChargeLine(String description, BigDecimal quantity, BigDecimal unitPrice) {
            this.description = description;
            this.quantity = quantity;
            this.unitPrice = unitPrice;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public BigDecimal getQuantity() {
            return quantity;
        }

        public void setQuantity(BigDecimal quantity) {
            this.quantity = quantity;
        }

        public BigDecimal getUnitPrice() {
            return unitPrice;
        }

        public void setUnitPrice(BigDecimal unitPrice) {
            this.unitPrice = unitPrice;
        }
    }

    public static class AdvanceInfo {

        private UUID id;
        private BigDecimal amount;
        private BigDecimal appliedAmount = BigDecimal.ZERO;

        public AdvanceInfo() {
        }

        public AdvanceInfo(UUID id, BigDecimal amount, BigDecimal appliedAmount) {
            this.id = id;
            this.amount = amount;
            this.appliedAmount = appliedAmount;
        }

        public UUID getId() {
            return id;
        }

        public void setId(UUID id) {
            this.id = id;
        }

        public BigDecimal getAmount() {
            return amount;
        }

        public void setAmount(BigDecimal amount) {
            this.amount = amount;
        }

        public BigDecimal getAppliedAmount() {
            return appliedAmount;
        }

        public void setAppliedAmount(BigDecimal appliedAmount) {
            this.appliedAmount = appliedAmount;
        }
    }

    public static class ChargePayment {

        private String method;
        private BigDecimal amount;
        private String reference;

        public ChargePayment() {
        }

        public String getMethod() {
            return method;
        }

        public void setMethod(String method) {
            this.method = method;
        }

        public BigDecimal getAmount() {
            return amount;
        }

        public void setAmount(BigDecimal amount) {
            this.amount = amount;
        }

        public String getReference() {
            return reference;
        }

        public void setReference(String reference) {
            this.reference = reference;
        }
    }
}
