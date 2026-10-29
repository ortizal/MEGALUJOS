package ec.com.megalujos.invoicing.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public class IssueDocumentRequest {

    @NotNull
    private UUID saleId;

    private String documentType = "01";

    public IssueDocumentRequest() {
    }

    public UUID getSaleId() {
        return saleId;
    }

    public void setSaleId(UUID saleId) {
        this.saleId = saleId;
    }

    public String getDocumentType() {
        return documentType;
    }

    public void setDocumentType(String documentType) {
        this.documentType = documentType;
    }
}
