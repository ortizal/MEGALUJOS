package ec.com.megalujos.invoicing.sri;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Cliente nulo inyectado cuando megalujos.sri.enabled=false: no realiza
 * ninguna llamada de red ni simula autorizaciones.
 */
@Component
@ConditionalOnProperty(name = "megalujos.sri.enabled", havingValue = "false", matchIfMissing = true)
public class NullSriClient implements SriClient {

    @Override
    public SriReceptionResponse send(String accessKey, byte[] signedXml) {
        throw new UnsupportedOperationException(
                "Integración SRI deshabilitada (sri.enabled=false): no se enviarán comprobantes");
    }

    @Override
    public SriAuthorizationResponse queryAuthorization(String accessKey) {
        throw new UnsupportedOperationException(
                "Integración SRI deshabilitada (sri.enabled=false): no se consultarán autorizaciones");
    }
}
