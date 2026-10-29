package ec.com.megalujos.common.print.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.Map;
import java.util.UUID;

public class PrintJobRequest {

    @NotBlank
    @Pattern(regexp = "SALE_TICKET|CASH_OPENING|CASH_CLOSING|SERVICE_ORDER|PRODUCT_LABEL|RIDE",
            message = "jobType no válido")
    private String jobType;

    @NotBlank
    @Size(max = 50)
    private String sourceType;

    @NotNull
    private UUID sourceId;

    @NotBlank
    @Pattern(regexp = "THERMAL_80|THERMAL_58|A4", message = "format debe ser THERMAL_80, THERMAL_58 o A4")
    private String format = "THERMAL_80";

    @Min(1)
    @Max(20)
    private int copies = 1;

    @Size(max = 120)
    private String printerName;

    /** Payload opcional; para SALE_TICKET se rellena automáticamente desde la venta si viene vacío. */
    private Map<String, Object> payload;

    public PrintJobRequest() {
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
}
