package ec.com.megalujos.services.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public class AttachMediaRequest {

    @NotNull(message = "El archivo (mediaId) es obligatorio")
    private UUID mediaId;

    /** DESIGN, PREVIEW, PHOTO o DOCUMENT (por defecto DESIGN). */
    private String attachmentType;

    public AttachMediaRequest() {
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
