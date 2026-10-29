package ec.com.megalujos.common.print.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import ec.com.megalujos.common.print.domain.PrintJob;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

public class PrintJobResponseDto {

    private UUID id;
    private String jobType;
    private String sourceType;
    private UUID sourceId;
    private String format;
    private String status;
    private int copies;
    private String printerName;
    private Map<String, Object> payload;
    private OffsetDateTime createdAt;
    private OffsetDateTime printedAt;

    public PrintJobResponseDto() {
    }

    public static PrintJobResponseDto from(PrintJob job, ObjectMapper objectMapper) {
        PrintJobResponseDto dto = new PrintJobResponseDto();
        dto.setId(job.getId());
        dto.setJobType(job.getJobType());
        dto.setSourceType(job.getSourceType());
        dto.setSourceId(job.getSourceId());
        dto.setFormat(job.getFormat());
        dto.setStatus(job.getStatus());
        dto.setCopies(job.getCopies());
        dto.setPrinterName(job.getPrinterName());
        dto.setCreatedAt(job.getCreatedAt());
        dto.setPrintedAt(job.getPrintedAt());
        try {
            JsonNode node = objectMapper.readTree(job.getPayload() != null ? job.getPayload() : "{}");
            dto.setPayload(objectMapper.convertValue(node, Map.class));
        } catch (Exception ex) {
            dto.setPayload(Map.of());
        }
        return dto;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getJobType() {
        return jobType;
    }

    public void setJobType(String jobType) {
        this.jobType = jobType;
    }

    public String getSourceType() {
        return sourceType;
    }

    public void setSourceType(String sourceType) {
        this.sourceType = sourceType;
    }

    public UUID getSourceId() {
        return sourceId;
    }

    public void setSourceId(UUID sourceId) {
        this.sourceId = sourceId;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getCopies() {
        return copies;
    }

    public void setCopies(int copies) {
        this.copies = copies;
    }

    public String getPrinterName() {
        return printerName;
    }

    public void setPrinterName(String printerName) {
        this.printerName = printerName;
    }

    public Map<String, Object> getPayload() {
        return payload;
    }

    public void setPayload(Map<String, Object> payload) {
        this.payload = payload;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public OffsetDateTime getPrintedAt() {
        return printedAt;
    }

    public void setPrintedAt(OffsetDateTime printedAt) {
        this.printedAt = printedAt;
    }
}
