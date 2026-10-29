package ec.com.megalujos.invoicing.sri;

/**
 * Adaptador delimitado hacia los servicios web del SRI. La implementación real
 * (HttpSriClient) solo se activa con megalujos.sri.enabled=true; con enabled=false
 * se inyecta NullSriClient que no realiza llamadas de red.
 */
public interface SriClient {

    /** Envía el comprobante (XML firmado) al servicio de recepción. */
    SriReceptionResponse send(String accessKey, byte[] signedXml);

    /** Consulta el estado de autorización del comprobante por clave de acceso. */
    SriAuthorizationResponse queryAuthorization(String accessKey);

    record SriReceptionResponse(boolean accepted, String state, String message) {
    }

    record SriAuthorizationResponse(boolean authorized, String authorizationNumber,
                                    String message, String authorizedXml) {
    }
}
