package ec.com.megalujos.services.domain;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

/** Clave compuesta de service_attachments (service_order_id, media_id). */
public class ServiceAttachmentId implements Serializable {

    private UUID serviceOrderId;
    private UUID mediaId;

    public ServiceAttachmentId() {
    }

    public ServiceAttachmentId(UUID serviceOrderId, UUID mediaId) {
        this.serviceOrderId = serviceOrderId;
        this.mediaId = mediaId;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ServiceAttachmentId other)) {
            return false;
        }
        return Objects.equals(serviceOrderId, other.serviceOrderId) && Objects.equals(mediaId, other.mediaId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(serviceOrderId, mediaId);
    }
}
