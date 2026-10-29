package ec.com.megalujos.invoicing.pdf;

import ec.com.megalujos.catalog.domain.MediaFile;
import ec.com.megalujos.common.exception.BusinessRuleException;
import ec.com.megalujos.common.files.FileStorageService;
import ec.com.megalujos.invoicing.domain.*;
import ec.com.megalujos.pos.domain.Sale;
import ec.com.megalujos.pos.domain.SaleItem;
import ec.com.megalujos.pos.domain.SaleRepository;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.UUID;

/**
 * Genera el RIDE (Representación Impresa del Comprobante Electrónico) en PDF con
 * los datos del comprobante: emisor, secuencial, cliente, ítems, totales y clave
 * de acceso. Se almacena como archivo privado (isPublic=false) y se asocia a
 * electronic_document_files como RIDE_PDF.
 */
@Service
public class RidePdfService {

    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final ZoneId ECUADOR = ZoneId.of("America/Guayaquil");

    private final ElectronicDocumentRepository documentRepository;
    private final ElectronicDocumentFileRepository fileRepository;
    private final IssuerRepository issuerRepository;
    private final SaleRepository saleRepository;
    private final CustomerSnapshotReader customerSnapshotReader;
    private final FileStorageService fileStorageService;

    public RidePdfService(ElectronicDocumentRepository documentRepository,
                          ElectronicDocumentFileRepository fileRepository,
                          IssuerRepository issuerRepository,
                          SaleRepository saleRepository,
                          CustomerSnapshotReader customerSnapshotReader,
                          FileStorageService fileStorageService) {
        this.documentRepository = documentRepository;
        this.fileRepository = fileRepository;
        this.issuerRepository = issuerRepository;
        this.saleRepository = saleRepository;
        this.customerSnapshotReader = customerSnapshotReader;
        this.fileStorageService = fileStorageService;
    }

    /**
     * Devuelve el mediaId del RIDE del documento; genera y almacena el PDF si el
     * comprobante está AUTHORIZED y aún no existe el archivo asociado.
     */
    @Transactional
    public UUID getOrGenerateRide(UUID documentId) {
        Optional<ElectronicDocumentFile> existing = fileRepository.findByDocumentIdAndFileType(documentId, "RIDE_PDF");
        if (existing.isPresent()) {
            return existing.get().getMediaId();
        }

        ElectronicDocument document = documentRepository.findById(documentId)
                .orElseThrow(() -> new BusinessRuleException("DOCUMENT_NOT_FOUND", "Comprobante no encontrado"));
        if (!"AUTHORIZED".equals(document.getStatus())) {
            throw new BusinessRuleException("DOCUMENT_NOT_AUTHORIZED",
                    "El RIDE solo se genera para comprobantes AUTHORIZED (estado actual: " + document.getStatus() + ")");
        }

        byte[] pdfBytes = generatePdf(document);
        MultipartFile file = new ByteArrayMultipartFile(
                "ride", "RIDE-" + document.getAccessKey() + ".pdf", "application/pdf", pdfBytes);
        MediaFile media = fileStorageService.store(file, false);
        fileRepository.save(new ElectronicDocumentFile(documentId, "RIDE_PDF", media.getId()));
        return media.getId();
    }

    public byte[] loadPdfBytes(UUID mediaId) {
        MediaFile media = fileStorageService.getRequired(mediaId);
        try {
            return java.nio.file.Files.readAllBytes(fileStorageService.resolve(media.getStorageKey()));
        } catch (IOException ex) {
            throw new BusinessRuleException("RIDE_READ_ERROR", "No se pudo leer el RIDE: " + ex.getMessage());
        }
    }

    private byte[] generatePdf(ElectronicDocument document) {
        Issuer issuer = issuerRepository.findById(document.getIssuerId())
                .orElseThrow(() -> new BusinessRuleException("ISSUER_NOT_FOUND", "Emisor no encontrado"));
        Sale sale = document.getSaleId() != null
                ? saleRepository.findById(document.getSaleId()).orElse(null) : null;
        CustomerSnapshotReader.CustomerSnapshot customer = sale != null
                ? customerSnapshotReader.findByCustomerId(sale.getCustomerId()) : null;

        try (PDDocument pdDocument = new PDDocument();
             ByteArrayOutputStream out = new ByteArrayOutputStream();
              PDPageContentStream content = new PDPageContentStream(
                      pdDocument, new PDPage())) {

            PDType1Font bold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
            PDType1Font regular = new PDType1Font(Standard14Fonts.FontName.HELVETICA);

            float y = 780f;
            float left = 40f;

            content.beginText();
            content.setFont(bold, 14);
            content.newLineAtOffset(left, y);
            content.showText("RIDE - Representación Impresa del Comprobante Electrónico");
            content.endText();

            y -= 24;
            y = writeLine(content, regular, left, y, "Emisor: " + issuer.getLegalName()
                    + (issuer.getTradeName() != null ? " (" + issuer.getTradeName() + ")" : ""));
            y = writeLine(content, regular, left, y, "RUC: " + issuer.getTaxId());
            y = writeLine(content, regular, left, y, "Establecimiento: " + issuer.getEstablishmentCode()
                    + "  Punto de emisión: " + issuer.getEmissionPointCode()
                    + "  Ambiente: " + issuer.getEnvironment());
            if (issuer.getMainAddress() != null && !issuer.getMainAddress().isBlank()) {
                y = writeLine(content, regular, left, y, "Dirección: " + issuer.getMainAddress());
            }

            y -= 8;
            y = writeLine(content, bold, left, y, "Comprobante: " + documentTypeName(document.getDocumentType())
                    + "  Secuencial: " + String.format("%09d", document.getSequenceNumber()));
            y = writeLine(content, regular, left, y, "Clave de acceso: " + document.getAccessKey());
            if (document.getAuthorizationNumber() != null) {
                y = writeLine(content, regular, left, y, "Autorización: " + document.getAuthorizationNumber()
                        + "  Fecha: " + format(document.getAuthorizedAt()));
            }
            y = writeLine(content, regular, left, y, "Fecha emisión: " + format(document.getCreatedAt()));

            y -= 8;
            y = writeLine(content, bold, left, y, "Comprador:");
            y = writeLine(content, regular, left, y, "  " + (customer != null ? customer.name() : "CONSUMIDOR FINAL"));
            y = writeLine(content, regular, left, y, "  Identificación: "
                    + (customer != null && customer.documentNumber() != null ? customer.documentNumber() : "9999999999999"));

            if (sale != null) {
                y -= 10;
                y = writeLine(content, bold, left, y, "Detalle de ítems:");
                for (SaleItem item : sale.getItems()) {
                    y = writeLine(content, regular, left, y, String.format(
                            "  %s  x%s  $%s  Total $%s",
                            truncate(item.getDescription(), 40),
                            item.getQuantity().setScale(3, RoundingMode.HALF_UP).toPlainString(),
                            item.getUnitPrice().setScale(2, RoundingMode.HALF_UP).toPlainString(),
                            item.getLineTotal().setScale(2, RoundingMode.HALF_UP).toPlainString()));
                }

                y -= 10;
                y = writeLine(content, bold, left, y, "Totales:");
                y = writeLine(content, regular, left, y, "  Subtotal: $" + money(sale.getSubtotal()));
                y = writeLine(content, regular, left, y, "  Descuento: $" + money(sale.getDiscountTotal()));
                y = writeLine(content, regular, left, y, "  IVA: $" + money(sale.getTaxTotal()));
                y = writeLine(content, bold, left, y, "  TOTAL: $" + money(sale.getGrandTotal()));
            }

            pdDocument.save(out);
            return out.toByteArray();
        } catch (IOException ex) {
            throw new BusinessRuleException("RIDE_GENERATION_ERROR", "No se pudo generar el PDF del RIDE: " + ex.getMessage());
        }
    }

    private float writeLine(PDPageContentStream content, PDType1Font font, float x, float y, String text)
            throws IOException {
        content.beginText();
        content.setFont(font, 10);
        content.newLineAtOffset(x, y);
        content.showText(sanitize(text));
        content.endText();
        return y - 16f;
    }

    private String sanitize(String text) {
        return text == null ? "" : text.replaceAll("[\\r\\n\\t]", " ");
    }

    private String truncate(String value, int max) {
        if (value == null) {
            return "";
        }
        return value.length() > max ? value.substring(0, max - 1) + "…" : value;
    }

    private String money(BigDecimal value) {
        return value == null ? "0.00" : value.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private String format(OffsetDateTime dateTime) {
        return dateTime == null ? "-" : dateTime.atZoneSameInstant(ECUADOR).format(DATE_TIME);
    }

    private String documentTypeName(String code) {
        return "04".equals(code) ? "Nota de Crédito" : "Factura";
    }

    /** MultipartFile en memoria para reutilizar FileStorageService.store sin multipart real. */
    private static final class ByteArrayMultipartFile implements MultipartFile {

        private final String name;
        private final String originalFilename;
        private final String contentType;
        private final byte[] bytes;

        private ByteArrayMultipartFile(String name, String originalFilename, String contentType, byte[] bytes) {
            this.name = name;
            this.originalFilename = originalFilename;
            this.contentType = contentType;
            this.bytes = bytes;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String getOriginalFilename() {
            return originalFilename;
        }

        @Override
        public String getContentType() {
            return contentType;
        }

        @Override
        public boolean isEmpty() {
            return bytes == null || bytes.length == 0;
        }

        @Override
        public long getSize() {
            return bytes != null ? bytes.length : 0;
        }

        @Override
        public byte[] getBytes() {
            return bytes;
        }

        @Override
        public InputStream getInputStream() {
            return new ByteArrayInputStream(bytes);
        }

        @Override
        public void transferTo(File dest) throws IOException, IllegalStateException {
            Files.write(dest.toPath(), bytes);
        }

        @Override
        public void transferTo(Path dest) throws IOException, IllegalStateException {
            Files.write(dest, bytes);
        }
    }
}
