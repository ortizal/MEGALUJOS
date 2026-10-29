package ec.com.megalujos.invoicing.sri;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;

/**
 * Construye la clave de acceso del comprobante electrónico (49 dígitos según
 * la ficha técnica SRI):
 * fecha(8) + ruc(13) + tipo(2) + establecimiento(3) + punto de emisión(3)
 * + secuencial(9) + código numérico aleatorio(8) + ambiente(1) + estado(1)
 * + dígito verificador módulo 11.
 *
 * El dígito verificador usa el módulo 11: alfabeto de pesos 2..7 de
 * derecha a izquierda sobre los 48 dígitos; verificador = (11 - suma % 11)
 * con 10 u 11 mapeados a 0.
 */
@Component
public class AccessKeyGenerator {

    private static final int[] WEIGHTS = {2, 3, 4, 5, 6, 7};
    private final SecureRandom secureRandom = new SecureRandom();

    public String build(String ruc, String documentType, String establishmentCode,
                        String emissionPointCode, long sequenceNumber, String environment,
                        java.time.LocalDate emissionDate) {
        String environmentDigit = "PRODUCTION".equalsIgnoreCase(environment) ? "2" : "1";
        String body = padLeft(emissionDate.format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd")), 8)
                + pad(ruc, 13)
                + pad(documentType, 2)
                + pad(establishmentCode, 3)
                + pad(emissionPointCode, 3)
                + padLeft(Long.toString(sequenceNumber), 9)
                + randomDigits(8)
                + environmentDigit
                + "1"; // estado: 1 = contribuyente normal (2 = sin conexión)
        if (body.length() != 48) {
            throw new IllegalStateException("Cuerpo de clave de acceso inválido: se esperaban 48 dígitos, hay " + body.length());
        }
        return body + mod11CheckDigit(body);
    }

    public String build(String ruc, String documentType, String establishmentCode,
                        String emissionPointCode, long sequenceNumber, String environment) {
        return build(ruc, documentType, establishmentCode, emissionPointCode,
                sequenceNumber, environment, java.time.LocalDate.now());
    }

    /**
     * Módulo 11: pesos 2..7 de derecha a izquierda sobre los 48 dígitos;
     * resto = suma % 11; verificador = 11 - resto (10 u 11 → 0).
     */
    static char mod11CheckDigit(String digits) {
        int sum = 0;
        int weightIndex = 0;
        for (int i = digits.length() - 1; i >= 0; i--) {
            int digit = digits.charAt(i) - '0';
            sum += digit * WEIGHTS[weightIndex];
            weightIndex = (weightIndex + 1) % WEIGHTS.length;
        }
        int remainder = sum % 11;
        int verifier = 11 - remainder;
        if (verifier == 11 || verifier == 10) {
            verifier = 0;
        }
        return (char) ('0' + verifier);
    }

    private String randomDigits(int count) {
        StringBuilder sb = new StringBuilder(count);
        for (int i = 0; i < count; i++) {
            sb.append(secureRandom.nextInt(10));
        }
        return sb.toString();
    }

    private String pad(String value, int length) {
        if (value == null) {
            return "0".repeat(length);
        }
        String clean = value.trim();
        if (clean.length() > length) {
            return clean.substring(clean.length() - length);
        }
        return "0".repeat(length - clean.length()) + clean;
    }

    private String padLeft(String value, int length) {
        if (value.length() > length) {
            return value.substring(value.length() - length);
        }
        return "0".repeat(length - value.length()) + value;
    }
}
