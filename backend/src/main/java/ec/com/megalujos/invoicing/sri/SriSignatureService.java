package ec.com.megalujos.invoicing.sri;

import org.springframework.stereotype.Component;

/**
 * Firma XAdES-BES del comprobante. PENDIENTE de validación de la ficha técnica
 * SRI vigente: carga del certificado PKCS12 (megalujos.sri.certificate-path /
 * certificate-password), cálculo de huella, timestamp y estructura exacta de la
 * referencia de firma. Este componente NO simula la firma: lanza
 * UnsupportedOperationException hasta que se implemente contra la especificación oficial.
 */
@Component
public class SriSignatureService {

    public byte[] sign(byte[] xml) {
        throw new UnsupportedOperationException(
                "Firma XAdES-BES pendiente de validación de ficha técnica SRI; configurar certificado");
    }
}
