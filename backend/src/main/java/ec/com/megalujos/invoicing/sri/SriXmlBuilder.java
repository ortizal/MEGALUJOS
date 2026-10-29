package ec.com.megalujos.invoicing.sri;

import ec.com.megalujos.invoicing.domain.CustomerSnapshotReader;
import ec.com.megalujos.invoicing.domain.ElectronicDocument;
import ec.com.megalujos.invoicing.domain.Issuer;
import ec.com.megalujos.pos.domain.Sale;
import ec.com.megalujos.pos.domain.SaleItem;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Genera el XML del comprobante electrónico siguiendo la estructura documentada
 * (factura: infoTributaria + infoFactura + detalles + infoAdicional; nota de
 * crédito: infoTributaria + infoNotaCredito + detalles + infoAdicional).
 *
 * IMPORTANTE: la estructura generada DEBE validarse contra el XSD oficial del SRI
 * vigente (fecha de emisión, versiones de esquema, catálogos de formas de pago,
 * codigos de impuesto, identificación del comprador, etc.) ANTES de usarla en el
 * ambiente de certificación o producción. Este adaptador es deliberadamente
 * configurable y no asume esquemas tributarios oficiales.
 */
@Component
public class SriXmlBuilder {

    private static final DateTimeFormatter XML_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final String FORMA_PAGO_DEFAULT = "01"; // TODO validar contra catálogo SRI vigente
    private static final String CODIGO_IMPUESTO_IVA = "2"; // TODO validar contra catálogo SRI vigente

    public String buildInvoiceXml(ElectronicDocument document, Issuer issuer, Sale sale,
                                  CustomerSnapshotReader.CustomerSnapshot customer, List<SaleItem> items) {
        StringBuilder xml = new StringBuilder(4096);
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<factura id=\"comprobante\" version=\"1.1.0\">\n"); // TODO validar versión contra XSD vigente
        appendInfoTributaria(xml, document, issuer);
        xml.append("  <infoFactura>\n");
        xml.append("    <fechaEmision>").append(esc(formatDate(sale.getCreatedAt()))).append("</fechaEmision>\n");
        xml.append("    <dirEstablecimiento>").append(esc(nz(issuer.getMainAddress()))).append("</dirEstablecimiento>\n");
        if (issuer.getSpecialTaxpayerCode() != null && !issuer.getSpecialTaxpayerCode().isBlank()) {
            xml.append("    <contribuyenteEspecial>").append(esc(issuer.getSpecialTaxpayerCode())).append("</contribuyenteEspecial>\n");
        }
        xml.append("    <obligadoContabilidad>").append(esc(nz(issuer.getObligedAccounting()))).append("</obligadoContabilidad>\n");
        appendComprador(xml, customer);
        xml.append("    <totalSinImpuestos>").append(money(sale.getSubtotal())).append("</totalSinImpuestos>\n");
        xml.append("    <totalDescuento>").append(money(sale.getDiscountTotal())).append("</totalDescuento>\n");
        xml.append("    <totalConImpuestos>\n");
        appendTotalsImpuestos(xml, items);
        xml.append("    </totalConImpuestos>\n");
        xml.append("    <importeTotal>").append(money(sale.getGrandTotal())).append("</importeTotal>\n");
        xml.append("    <moneda>DOLAR</moneda>\n");
        xml.append("    <pagos>\n");
        xml.append("      <pago>\n");
        xml.append("        <formaPago>").append(esc(FORMA_PAGO_DEFAULT)).append("</formaPago>\n");
        xml.append("        <total>").append(money(sale.getGrandTotal())).append("</total>\n");
        xml.append("        <plazo>0</plazo>\n");
        xml.append("      </pago>\n");
        xml.append("    </pagos>\n");
        xml.append("  </infoFactura>\n");
        appendDetalles(xml, items);
        appendInfoAdicional(xml, sale);
        xml.append("</factura>\n");
        return xml.toString();
    }

    public String buildCreditNoteXml(ElectronicDocument document, Issuer issuer, Sale sale,
                                     CustomerSnapshotReader.CustomerSnapshot customer, List<SaleItem> items,
                                     ElectronicDocument originalInvoice, String reason) {
        StringBuilder xml = new StringBuilder(4096);
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<notaCredito id=\"comprobante\" version=\"1.1.0\">\n"); // TODO validar versión contra XSD vigente
        appendInfoTributaria(xml, document, issuer);
        xml.append("  <infoNotaCredito>\n");
        xml.append("    <fechaEmision>").append(esc(formatDate(document.getCreatedAt()))).append("</fechaEmision>\n");
        xml.append("    <dirEstablecimiento>").append(esc(nz(issuer.getMainAddress()))).append("</dirEstablecimiento>\n");
        if (issuer.getSpecialTaxpayerCode() != null && !issuer.getSpecialTaxpayerCode().isBlank()) {
            xml.append("    <contribuyenteEspecial>").append(esc(issuer.getSpecialTaxpayerCode())).append("</contribuyenteEspecial>\n");
        }
        xml.append("    <obligadoContabilidad>").append(esc(nz(issuer.getObligedAccounting()))).append("</obligadoContabilidad>\n");
        appendComprador(xml, customer);
        xml.append("    <numDocModificado>").append(esc(originalInvoice.getAccessKey())).append("</numDocModificado>\n");
        xml.append("    <fechaModificado>").append(esc(formatDate(originalInvoice.getCreatedAt()))).append("</fechaModificado>\n");
        xml.append("    <motivo>").append(esc(reason != null ? reason : "")).append("</motivo>\n");
        xml.append("    <totalSinImpuestos>").append(money(sale.getSubtotal())).append("</totalSinImpuestos>\n");
        xml.append("    <totalDescuento>").append(money(sale.getDiscountTotal())).append("</totalDescuento>\n");
        xml.append("    <totalConImpuestos>\n");
        appendTotalsImpuestos(xml, items);
        xml.append("    </totalConImpuestos>\n");
        xml.append("    <importeTotal>").append(money(sale.getGrandTotal())).append("</importeTotal>\n");
        xml.append("    <moneda>DOLAR</moneda>\n");
        xml.append("  </infoNotaCredito>\n");
        appendDetalles(xml, items);
        appendInfoAdicional(xml, sale);
        xml.append("</notaCredito>\n");
        return xml.toString();
    }

    private void appendInfoTributaria(StringBuilder xml, ElectronicDocument document, Issuer issuer) {
        xml.append("  <infoTributaria>\n");
        xml.append("    <ambiente>").append(esc(ambienteDigit(issuer.getEnvironment()))).append("</ambiente>\n");
        xml.append("    <tipoEmision>1</tipoEmision>\n"); // TODO validar contra catálogo SRI vigente
        xml.append("    <razonSocial>").append(esc(issuer.getLegalName())).append("</razonSocial>\n");
        xml.append("    <nombreComercial>").append(esc(nz(issuer.getTradeName()))).append("</nombreComercial>\n");
        xml.append("    <ruc>").append(esc(issuer.getTaxId())).append("</ruc>\n");
        xml.append("    <claveAcceso>").append(esc(document.getAccessKey())).append("</claveAcceso>\n");
        xml.append("    <codDoc>").append(esc(document.getDocumentType())).append("</codDoc>\n");
        xml.append("    <estab>").append(esc(issuer.getEstablishmentCode())).append("</estab>\n");
        xml.append("    <ptoEmi>").append(esc(issuer.getEmissionPointCode())).append("</ptoEmi>\n");
        xml.append("    <secuencial>").append(padLeft(Long.toString(document.getSequenceNumber()), 9)).append("</secuencial>\n");
        xml.append("  </infoTributaria>\n");
    }

    private void appendComprador(StringBuilder xml, CustomerSnapshotReader.CustomerSnapshot customer) {
        String docType = "01"; // TODO validar catálogo de identificación contra SRI vigente
        String doc = "9999999999999";
        String name = "CONSUMIDOR FINAL";
        String address = "";
        String email = "";
        if (customer != null) {
            if (customer.documentNumber() != null && !customer.documentNumber().isBlank()) {
                doc = customer.documentNumber();
            }
            if (customer.name() != null && !customer.name().isBlank()) {
                name = customer.name();
            }
            if (customer.address() != null) {
                address = customer.address();
            }
            if (customer.email() != null) {
                email = customer.email();
            }
            if ("RUC".equalsIgnoreCase(customer.documentType())) {
                docType = "04";
            } else if ("CEDULA".equalsIgnoreCase(customer.documentType())) {
                docType = "05";
            } else if ("PASAPORTE".equalsIgnoreCase(customer.documentType())) {
                docType = "06";
            } else if ("CONSUMIDOR_FINAL".equalsIgnoreCase(customer.documentType())) {
                docType = "07";
                doc = "9999999999999";
            }
        }
        xml.append("    <comprador>\n");
        xml.append("      <tipoIdentificacionComprador>").append(esc(docType)).append("</tipoIdentificacionComprador>\n");
        xml.append("      <razonSocialComprador>").append(esc(name)).append("</razonSocialComprador>\n");
        xml.append("      <identificacionComprador>").append(esc(doc)).append("</identificacionComprador>\n");
        if (!address.isBlank()) {
            xml.append("      <direccionComprador>").append(esc(address)).append("</direccionComprador>\n");
        }
        if (!email.isBlank()) {
            xml.append("      <emailComprador>").append(esc(email)).append("</emailComprador>\n");
        }
        xml.append("    </comprador>\n");
    }

    private void appendTotalsImpuestos(StringBuilder xml, List<SaleItem> items) {
        // Agrupa por (taxCode, taxRate) sin hardcodear tasas: usa el snapshot de la venta.
        var grouped = items.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        item -> item.getTaxCode() + "|" + item.getTaxRate().toPlainString()));
        for (var entry : grouped.entrySet()) {
            BigDecimal taxable = items.stream()
                    .filter(i -> (i.getTaxCode() + "|" + i.getTaxRate().toPlainString()).equals(entry.getKey()))
                    .map(i -> i.getLineTotal().subtract(i.getTaxAmount()))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal taxAmount = entry.getValue().stream()
                    .map(SaleItem::getTaxAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal rate = entry.getValue().get(0).getTaxRate();
            xml.append("      <impuesto>\n");
            xml.append("        <codigo>").append(esc(CODIGO_IMPUESTO_IVA)).append("</codigo>\n");
            xml.append("        <codigoPorcentaje>").append(esc(CODIGO_IMPUESTO_IVA)).append("</codigoPorcentaje>\n"); // TODO catálogo SRI
            xml.append("        <tarifa>").append(rate.setScale(2, RoundingMode.HALF_UP).toPlainString()).append("</tarifa>\n");
            xml.append("        <baseImponible>").append(money(taxable)).append("</baseImponible>\n");
            xml.append("        <valor>").append(money(taxAmount)).append("</valor>\n");
            xml.append("      </impuesto>\n");
        }
    }

    private void appendDetalles(StringBuilder xml, List<SaleItem> items) {
        xml.append("  <detalles>\n");
        int lineNumber = 1;
        for (SaleItem item : items) {
            BigDecimal quantity = item.getQuantity();
            BigDecimal unitPrice = item.getUnitPrice();
            BigDecimal lineTotal = item.getLineTotal();
            xml.append("    <detalle>\n");
            xml.append("      <codigoInterno>").append(esc("LI" + lineNumber)).append("</codigoInterno>\n");
            xml.append("      <descripcion>").append(esc(item.getDescription())).append("</descripcion>\n");
            xml.append("      <cantidad>").append(quantity.setScale(3, RoundingMode.HALF_UP).toPlainString()).append("</cantidad>\n");
            xml.append("      <precioUnitario>").append(unitPrice.setScale(4, RoundingMode.HALF_UP).toPlainString()).append("</precioUnitario>\n");
            xml.append("      <descuento>").append(money(item.getDiscount())).append("</descuento>\n");
            xml.append("      <precioTotalSinImpuesto>").append(money(lineTotal.subtract(item.getTaxAmount()))).append("</precioTotalSinImpuesto>\n");
            xml.append("      <impuestos>\n");
            xml.append("        <impuesto>\n");
            xml.append("          <codigo>").append(esc(CODIGO_IMPUESTO_IVA)).append("</codigo>\n");
            xml.append("          <codigoPorcentaje>").append(esc(CODIGO_IMPUESTO_IVA)).append("</codigoPorcentaje>\n"); // TODO catálogo SRI
            xml.append("          <tarifa>").append(item.getTaxRate().setScale(2, RoundingMode.HALF_UP).toPlainString()).append("</tarifa>\n");
            xml.append("          <baseImponible>").append(money(lineTotal.subtract(item.getTaxAmount()))).append("</baseImponible>\n");
            xml.append("          <valor>").append(money(item.getTaxAmount())).append("</valor>\n");
            xml.append("        </impuesto>\n");
            xml.append("      </impuestos>\n");
            xml.append("    </detalle>\n");
            lineNumber++;
        }
        xml.append("  </detalles>\n");
    }

    private void appendInfoAdicional(StringBuilder xml, Sale sale) {
        xml.append("  <infoAdicional>\n");
        xml.append("    <campoAdicional nombre=\"Venta\">").append(esc(sale.getSaleNumber())).append("</campoAdicional>\n");
        if (sale.getNotes() != null && !sale.getNotes().isBlank()) {
            xml.append("    <campoAdicional nombre=\"Observaciones\">").append(esc(sale.getNotes())).append("</campoAdicional>\n");
        }
        xml.append("  </infoAdicional>\n");
    }

    private String ambienteDigit(String environment) {
        return "PRODUCTION".equalsIgnoreCase(environment) ? "2" : "1";
    }

    private String formatDate(OffsetDateTime dateTime) {
        return dateTime.atZoneSameInstant(java.time.ZoneId.of("America/Guayaquil")).format(XML_DATE);
    }

    private String money(BigDecimal value) {
        return value == null ? "0.00" : value.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private String nz(String value) {
        return value == null ? "" : value;
    }

    private String padLeft(String value, int length) {
        return value.length() >= length ? value : "0".repeat(length - value.length()) + value;
    }

    /** Escaping XML mínimo y correcto para texto de elementos. */
    static String esc(String value) {
        if (value == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder(value.length() + 16);
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '&' -> sb.append("&amp;");
                case '<' -> sb.append("&lt;");
                case '>' -> sb.append("&gt;");
                case '"' -> sb.append("&quot;");
                case '\'' -> sb.append("&apos;");
                default -> {
                    if (c == 0x9 || c == 0xA || c == 0xD || (c >= 0x20 && c <= 0xD7FF)) {
                        sb.append(c);
                    }
                }
            }
        }
        return sb.toString();
    }
}
