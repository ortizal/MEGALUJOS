package ec.com.megalujos.invoicing.domain;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "document_sequences")
public class DocumentSequence {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "issuer_id", nullable = false)
    private UUID issuerId;

    @Column(name = "document_type", nullable = false, length = 10)
    private String documentType;

    @Column(name = "next_number", nullable = false)
    private long nextNumber = 1;

    public DocumentSequence() {
    }

    public DocumentSequence(UUID issuerId, String documentType, long nextNumber) {
        this.issuerId = issuerId;
        this.documentType = documentType;
        this.nextNumber = nextNumber;
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

    public String getDocumentType() {
        return documentType;
    }

    public void setDocumentType(String documentType) {
        this.documentType = documentType;
    }

    public long getNextNumber() {
        return nextNumber;
    }

    public void setNextNumber(long nextNumber) {
        this.nextNumber = nextNumber;
    }
}
