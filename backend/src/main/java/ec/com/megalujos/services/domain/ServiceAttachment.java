package ec.com.megalujos.services.domain;

import jakarta.persistence.*;

import java.util.UUID;

/** Archivo asociado a una orden (diseño, previsualización, foto, documentación). */
@Entity
@Table(name = "service_attachments")
@IdClass(ServiceAttachmentId.class)
public class ServiceAttachment {

    @Id
    @Column(name = "service_order_id")
    private UUID serviceOrderId;

    @Id
    @Column(name = "media_id")
    private UUID mediaId;

    @Column(name = "attachment_type", length = 40)
    private String attachmentType = "DESIGN"; // DESIGN, PREVIEW, PHOTO, DOCUMENT

    public ServiceAttachment() {
    }

    public UUID getServiceOrderId() {
        return serviceOrderId;
    }

    public void setServiceOrderId(UUID serviceOrderId) {
        this.serviceOrderId = serviceOrderId;
    }

    public UUID getMediaId() {
        return mediaId;
    }

    public void setMediaId(UUID mediaId) {
        this.mediaId = mediaId;
    }

    public String getAttachmentType() {
        return attachmentType;
    }

    public void setAttachmentType(String attachmentType) {
        this.attachmentType = attachmentType;
    }
}
