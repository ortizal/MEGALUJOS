package ec.com.megalujos.invoicing.sri;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;

/**
 * Cliente HTTP real hacia recepción/autorización del SRI. El parseo de la
 * respuesta es deliberadamente básico (códigos de estado del comprobante en el
 * XML de respuesta) y debe afinarse contra la ficha técnica oficial vigente.
 * Si el parseo falla, se propaga excepción para que el worker reintente o marque FAILED.
 */
@Component
@ConditionalOnProperty(name = "megalujos.sri.enabled", havingValue = "true")
public class HttpSriClient implements SriClient {

    private static final Logger log = LoggerFactory.getLogger(HttpSriClient.class);

    private final RestClient restClient;
    private final String receptionUrl;
    private final String authorizationUrl;

    public HttpSriClient(@Value("${megalujos.sri.reception-url}") String receptionUrl,
                         @Value("${megalujos.sri.authorization-url}") String authorizationUrl,
                         @Value("${megalujos.sri.http.connect-timeout-ms:10000}") long connectTimeoutMs,
                         @Value("${megalujos.sri.http.read-timeout-ms:30000}") long readTimeoutMs) {
        this.receptionUrl = receptionUrl;
        this.authorizationUrl = authorizationUrl;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofMillis(connectTimeoutMs));
        factory.setReadTimeout(Duration.ofMillis(readTimeoutMs));
        this.restClient = RestClient.builder()
                .requestFactory(factory)
                .build();
    }

    @Override
    public SriReceptionResponse send(String accessKey, byte[] signedXml) {
        String body = envelopReception(signedXml);
        try {
            String response = restClient.post()
                    .uri(receptionUrl)
                    .contentType(MediaType.APPLICATION_XML)
                    .accept(MediaType.APPLICATION_XML)
                    .body(body)
                    .retrieve()
                    .body(String.class);
            return parseReception(response);
        } catch (Exception ex) {
            log.error("Fallo de recepción SRI para clave {}: {}", accessKey, ex.getMessage());
            throw new SriTransportException("Error llamando recepción SRI: " + ex.getMessage(), ex);
        }
    }

    @Override
    public SriAuthorizationResponse queryAuthorization(String accessKey) {
        String body = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" "
                + "xmlns:ec=\"http://ec.gob.sri.ws.autorizacion\">\n"
                + "  <soapenv:Header/>\n"
                + "  <soapenv:Body>\n"
                + "    <ec:consultaAutorizacion><numAutorizacion>" + accessKey + "</numAutorizacion></ec:consultaAutorizacion>\n"
                + "  </soapenv:Body>\n"
                + "</soapenv:Envelope>";
        try {
            String response = restClient.post()
                    .uri(authorizationUrl)
                    .contentType(MediaType.APPLICATION_XML)
                    .accept(MediaType.APPLICATION_XML)
                    .body(body)
                    .retrieve()
                    .body(String.class);
            return parseAuthorization(accessKey, response);
        } catch (Exception ex) {
            log.error("Fallo de autorización SRI para clave {}: {}", accessKey, ex.getMessage());
            throw new SriTransportException("Error llamando autorización SRI: " + ex.getMessage(), ex);
        }
    }

    private String envelopReception(byte[] signedXml) {
        String xml = new String(signedXml, java.nio.charset.StandardCharsets.UTF_8);
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" "
                + "xmlns:ec=\"http://ec.gob.sri.ws.recepcion\">\n"
                + "  <soapenv:Header/>\n"
                + "  <soapenv:Body>\n"
                + "    <ec:validar><![CDATA[" + xml + "]]></ec:validar>\n"
                + "  </soapenv:Body>\n"
                + "</soapenv:Envelope>";
    }

    /**
     * Parseo básico: busca etiquetas <estado> en la respuesta de recepción.
     * RECIBIDO = aceptado para procesamiento; cualquier otro estado = no aceptado.
     */
    SriReceptionResponse parseReception(String responseXml) {
        if (responseXml == null || responseXml.isBlank()) {
            throw new SriParseException("Respuesta vacía de recepción SRI");
        }
        String state = extractTag(responseXml, "estado");
        if (state == null) {
            throw new SriParseException("No se pudo interpretar la respuesta de recepción SRI (sin <estado>)");
        }
        boolean accepted = "RECIBIDO".equalsIgnoreCase(state.trim());
        return new SriReceptionResponse(accepted, state.trim(), extractTag(responseXml, "mensaje"));
    }

    /**
     * Parseo básico: <estado>AUTORIZADO</estado> y <numAutorizacion> en la respuesta.
     * Si no hay etiqueta estado, se lanza SriParseException (worker reintentará o marcará FAILED).
     */
    SriAuthorizationResponse parseAuthorization(String accessKey, String responseXml) {
        if (responseXml == null || responseXml.isBlank()) {
            throw new SriParseException("Respuesta vacía de autorización SRI");
        }
        String state = extractTag(responseXml, "estado");
        if (state == null) {
            throw new SriParseException("No se pudo interpretar la respuesta de autorización SRI (sin <estado>)");
        }
        boolean authorized = "AUTORIZADO".equalsIgnoreCase(state.trim());
        String number = extractTag(responseXml, "numAutorizacion");
        if (number == null) {
            number = authorized ? accessKey : null;
        }
        return new SriAuthorizationResponse(authorized, number, extractTag(responseXml, "mensaje"), null);
    }

    private String extractTag(String xml, String tag) {
        int start = xml.indexOf("<" + tag + ">");
        int end = xml.indexOf("</" + tag + ">");
        if (start < 0 || end < 0 || end < start) {
            return null;
        }
        return xml.substring(start + tag.length() + 2, end).trim();
    }

    public static class SriTransportException extends RuntimeException {
        public SriTransportException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public static class SriParseException extends RuntimeException {
        public SriParseException(String message) {
            super(message);
        }
    }
}
