package ec.com.megalujos.invoicing.dto;

import java.util.UUID;

/**
 * Respuesta mínima de los endpoints de emisión (202 Accepted):
 * identificador del comprobante y su estado inicial (GENERATED).
 */
public class IssueDocumentResponse {

    private UUID documentId;
    private String status;

    public IssueDocumentResponse() {
    }

    public IssueDocumentResponse(UUID documentId, String status) {
        this.documentId = documentId;
        this.status = status;
    }

    public static IssueDocumentResponse from(ElectronicDocumentSummaryDto summary) {
        return new IssueDocumentResponse(summary.getId(), summary.getStatus());
    }

    public UUID getDocumentId() {
        return documentId;
    }

    public void setDocumentId(UUID documentId) {
        this.documentId = documentId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
