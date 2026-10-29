package ec.com.megalujos.invoicing.domain;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "electronic_documents")
public class ElectronicDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "issuer_id", nullable = false)
    private UUID issuerId;

    @Column(name = "sale_id")
    private UUID saleId;

    @Column(name = "document_type", nullable = false, length = 10)
    private String documentType;

    @Column(name = "sequence_number", nullable = false)
    private long sequenceNumber;

    @Column(name = "access_key", length = 49)
    private String accessKey;

    @Column(nullable = false, length = 30)
    private String status = "GENERATED"; // GENERATED, SIGNED, SENT, RECEIVED, AUTHORIZED, REJECTED, FAILED

    @Column(name = "authorization_number", length = 100)
    private String authorizationNumber;

    @Column(name = "authorized_at")
    private OffsetDateTime authorizedAt;

    @Column(name = "last_error", columnDefinition = "TEXT")
    private String lastError;

    @Column(name = "reference_document_id")
    private UUID referenceDocumentId;

    @Column(columnDefinition = "TEXT")
    private String reason;

    @Column(name = "idempotency_key")
    private UUID idempotencyKey;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    public ElectronicDocument() {
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

    public String getLastError() {
        return lastError;
    }

    public void setLastError(String lastError) {
        this.lastError = lastError;
    }

    public UUID getReferenceDocumentId() {
        return referenceDocumentId;
    }

    public void setReferenceDocumentId(UUID referenceDocumentId) {
        this.referenceDocumentId = referenceDocumentId;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public UUID getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(UUID idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
