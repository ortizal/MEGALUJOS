package ec.com.megalujos.pos.dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Detalle extendido de una venta: agrega cliente, caja e historial de devoluciones. */
public class SaleDetailDto extends SaleResponseDto {

    private UUID customerId;
    private UUID cashSessionId;
    private BigDecimal totalReturned = BigDecimal.ZERO;
    private List<SaleReturnDto> returns = new ArrayList<>();

    public SaleDetailDto() {
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

    public BigDecimal getTotalReturned() {
        return totalReturned;
    }

    public void setTotalReturned(BigDecimal totalReturned) {
        this.totalReturned = totalReturned;
    }

    public List<SaleReturnDto> getReturns() {
        return returns;
    }

    public void setReturns(List<SaleReturnDto> returns) {
        this.returns = returns;
    }
}
