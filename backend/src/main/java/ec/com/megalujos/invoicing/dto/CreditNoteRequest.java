package ec.com.megalujos.invoicing.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public class CreditNoteRequest {

    /** Venta original cuyo comprobante se compensa con la nota de crédito. */
    @NotNull
    private UUID saleId;

    @NotBlank
    @Size(max = 20)
    private String reasonCode;

    @NotBlank
    @Size(max = 2000)
    private String reason;

    public CreditNoteRequest() {
    }

    public UUID getSaleId() {
        return saleId;
    }

    public void setSaleId(UUID saleId) {
        this.saleId = saleId;
    }

    public String getReasonCode() {
        return reasonCode;
    }

    public void setReasonCode(String reasonCode) {
        this.reasonCode = reasonCode;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
