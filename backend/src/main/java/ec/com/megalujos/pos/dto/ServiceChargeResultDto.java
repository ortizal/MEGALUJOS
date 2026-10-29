package ec.com.megalujos.pos.dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Resultado del cobro de una orden de servicio: venta generada + anticipos aplicados. */
public class ServiceChargeResultDto {

    private SaleResponseDto sale;
    private boolean replay;
    private List<AppliedAdvanceDto> appliedAdvances = new ArrayList<>();

    public ServiceChargeResultDto() {
    }

    public SaleResponseDto getSale() {
        return sale;
    }

    public void setSale(SaleResponseDto sale) {
        this.sale = sale;
    }

    public boolean isReplay() {
        return replay;
    }

    public void setReplay(boolean replay) {
        this.replay = replay;
    }

    public List<AppliedAdvanceDto> getAppliedAdvances() {
        return appliedAdvances;
    }

    public void setAppliedAdvances(List<AppliedAdvanceDto> appliedAdvances) {
        this.appliedAdvances = appliedAdvances;
    }

    public static class AppliedAdvanceDto {

        private UUID advanceId;
        private BigDecimal appliedAmount;

        public AppliedAdvanceDto() {
        }

        public AppliedAdvanceDto(UUID advanceId, BigDecimal appliedAmount) {
            this.advanceId = advanceId;
            this.appliedAmount = appliedAmount;
        }

        public UUID getAdvanceId() {
            return advanceId;
        }

        public void setAdvanceId(UUID advanceId) {
            this.advanceId = advanceId;
        }

        public BigDecimal getAppliedAmount() {
            return appliedAmount;
        }

        public void setAppliedAmount(BigDecimal appliedAmount) {
            this.appliedAmount = appliedAmount;
        }
    }
}
