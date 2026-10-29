package ec.com.megalujos.invoicing.domain;

import jakarta.persistence.*;

import java.util.UUID;

@Entity
@Table(name = "electronic_document_files")
@IdClass(ElectronicDocumentFileId.class)
public class ElectronicDocumentFile {

    @Id
    @Column(name = "document_id")
    private UUID documentId;

    @Id
    @Column(name = "file_type", length = 20)
    private String fileType; // XML_ORIGINAL, XML_SIGNED, XML_AUTHORIZED, RIDE_PDF

    @Column(name = "media_id", nullable = false)
    private UUID mediaId;

    public ElectronicDocumentFile() {
    }

    public ElectronicDocumentFile(UUID documentId, String fileType, UUID mediaId) {
        this.documentId = documentId;
        this.fileType = fileType;
        this.mediaId = mediaId;
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

    public UUID getMediaId() {
        return mediaId;
    }

    public void setMediaId(UUID mediaId) {
        this.mediaId = mediaId;
    }
}
