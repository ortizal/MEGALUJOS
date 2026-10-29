package ec.com.megalujos.invoicing.dto;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ElectronicDocumentDetailDto extends ElectronicDocumentSummaryDto {

    private String reason;
    private List<DocumentEventDto> events = new ArrayList<>();
    private List<DocumentFileDto> files = new ArrayList<>();

    public ElectronicDocumentDetailDto() {
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public List<DocumentEventDto> getEvents() {
        return events;
    }

    public void setEvents(List<DocumentEventDto> events) {
        this.events = events;
    }

    public List<DocumentFileDto> getFiles() {
        return files;
    }

    public void setFiles(List<DocumentFileDto> files) {
        this.files = files;
    }
}
