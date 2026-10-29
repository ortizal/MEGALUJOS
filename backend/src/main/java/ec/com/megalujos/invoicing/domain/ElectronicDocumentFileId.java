package ec.com.megalujos.invoicing.domain;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

public class ElectronicDocumentFileId implements Serializable {

    private UUID documentId;
    private String fileType;

    public ElectronicDocumentFileId() {
    }

    public ElectronicDocumentFileId(UUID documentId, String fileType) {
        this.documentId = documentId;
        this.fileType = fileType;
    }

    public UUID getDocumentId() {
        return documentId;
    }

    public void setDocumentId(UUID documentId) {
        this.documentId = documentId;
    }

    public String getFileType() {
        return fileType;
    }

    public void setFileType(String fileType) {
        this.fileType = fileType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ElectronicDocumentFileId that = (ElectronicDocumentFileId) o;
        return Objects.equals(documentId, that.documentId) && Objects.equals(fileType, that.fileType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(documentId, fileType);
    }
}
