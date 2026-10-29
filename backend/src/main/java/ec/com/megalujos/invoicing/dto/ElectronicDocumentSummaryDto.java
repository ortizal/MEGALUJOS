package ec.com.megalujos.invoicing.dto;

import ec.com.megalujos.invoicing.domain.ElectronicDocument;

import java.time.OffsetDateTime;
import java.util.UUID;

public class ElectronicDocumentSummaryDto {

    private UUID id;
    private UUID issuerId;
    private UUID saleId;
    private String documentType;
    private long sequenceNumber;
    private String accessKey;
    private String status;
    private String authorizationNumber;
    private OffsetDateTime authorizedAt;
    private UUID referenceDocumentId;
    private String lastError;
    private OffsetDateTime createdAt;

    public ElectronicDocumentSummaryDto() {
    }

    public static ElectronicDocumentSummaryDto from(ElectronicDocument doc) {
        ElectronicDocumentSummaryDto dto = new ElectronicDocumentSummaryDto();
        dto.setId(doc.getId());
        dto.setIssuerId(doc.getIssuerId());
        dto.setSaleId(doc.getSaleId());
        dto.setDocumentType(doc.getDocumentType());
        dto.setSequenceNumber(doc.getSequenceNumber());
        dto.setAccessKey(doc.getAccessKey());
        dto.setStatus(doc.getStatus());
        dto.setAuthorizationNumber(doc.getAuthorizationNumber());
        dto.setAuthorizedAt(doc.getAuthorizedAt());
        dto.setReferenceDocumentId(doc.getReferenceDocumentId());
        dto.setLastError(doc.getLastError());
        dto.setCreatedAt(doc.getCreatedAt());
        return dto;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getIssuerId() {
        return issuerId;
    }

    public void setIssuerId(UUID issuerId) {
        this.issuerId = issuerId;
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

    public long getSequenceNumber() {
        return sequenceNumber;
    }

    public void setSequenceNumber(long sequenceNumber) {
        this.sequenceNumber = sequenceNumber;
    }

    public String getAccessKey() {
        return accessKey;
    }

    public void setAccessKey(String accessKey) {
        this.accessKey = accessKey;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getAuthorizationNumber() {
        return authorizationNumber;
    }

    public void setAuthorizationNumber(String authorizationNumber) {
        this.authorizationNumber = authorizationNumber;
    }

    public OffsetDateTime getAuthorizedAt() {
        return authorizedAt;
    }

    public void setAuthorizedAt(OffsetDateTime authorizedAt) {
        this.authorizedAt = authorizedAt;
    }

    public UUID getReferenceDocumentId() {
        return referenceDocumentId;
    }

    public void setReferenceDocumentId(UUID referenceDocumentId) {
        this.referenceDocumentId = referenceDocumentId;
    }

    public String getLastError() {
        return lastError;
    }

    public void setLastError(String lastError) {
        this.lastError = lastError;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
