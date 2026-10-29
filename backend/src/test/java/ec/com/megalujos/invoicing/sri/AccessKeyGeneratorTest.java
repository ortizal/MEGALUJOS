package ec.com.megalujos.invoicing.sri;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitarios de AccessKeyGenerator: estructura de 49 dígitos
 * (fecha(8)+ruc(13)+tipo(2)+estab(3)+punto(3)+secuencial(9)+código(8)+ambiente(1)+verificador(1)),
 * prefijo determinista y dígito verificador módulo 11.
 * La verificación del dígito se hace con una implementación independiente en el test.
 */
class AccessKeyGeneratorTest {

    private final AccessKeyGenerator generator = new AccessKeyGenerator();

    /**
     * Implementación independiente del módulo 11 (no reutiliza la del SUT).
     * Pesos 2..7 de derecha a izquierda; verificador = 11 - (suma % 11);
     * si el resultado es 10 u 11 se coloca 0.
     */
    private static char checkDigitIndependiente(String digits) {
        int suma = 0;
        int peso = 2;
        for (int i = digits.length() - 1; i >= 0; i--) {
            suma += (digits.charAt(i) - '0') * peso;
            peso = (peso == 7) ? 2 : peso + 1;
        }
        int verificador = 11 - (suma % 11);
        if (verificador == 10 || verificador == 11) {
            verificador = 0;
        }
        return (char) ('0' + verificador);
    }

    @Test
    void build_claveEsDe49Digitos() {
        // When
        String key = generator.build("1790010010001", "01", "001", "001", 12345678, "PRODUCTION", LocalDate.of(2026, 1, 15));

        // Then
        assertEquals(49, key.length());
        assertTrue(key.chars().allMatch(c -> c >= '0' && c <= '9'), "La clave debe ser solo dígitos");
    }

    @Test
    void build_prefijoEsFechaRucTipoEstabPuntoSecuencial() {
        // When
        String key = generator.build("1790010010001", "01", "001", "002", 12345678, "PRODUCTION", LocalDate.of(2026, 1, 15));

        // Then: fecha(8) + ruc(13) + tipo(2) + estab(3) + punto(3) + secuencial(9)
        assertEquals("20260115", key.substring(0, 8));
        assertEquals("1790010010001", key.substring(8, 21));
        assertEquals("01", key.substring(21, 23));
        assertEquals("001", key.substring(23, 26));
        assertEquals("002", key.substring(26, 29));
        assertEquals("012345678", key.substring(29, 38));
    }

    @Test
    void build_secuencialSeRellenaCerosALaIzquierda() {
        // When
        String key = generator.build("1790010010001", "01", "001", "001", 42, "PRODUCTION", LocalDate.of(2026, 1, 15));

        // Then: secuencial 9 dígitos
        assertEquals("000000042", key.substring(29, 38));
    }

    @Test
    void build_ambienteProductionEs2YSistemaEs1() {
        // When
        String prod = generator.build("1790010010001", "01", "001", "001", 1, "PRODUCTION", LocalDate.of(2026, 1, 15));
        String sist = generator.build("1790010010001", "01", "001", "001", 1, "SISTEMA", LocalDate.of(2026, 1, 15));

        // Then: dígito de ambiente en la posición 46 (índice 46), estado en 47, verificador en 48
        assertEquals('2', prod.charAt(46));
        assertEquals('1', sist.charAt(46));
    }

    @Test
    void build_digitoVerificadorModulo11ValidoSegunImplementacionIndependiente() {
        // When: varias combinaciones
        String[] keys = {
                generator.build("1790010010001", "01", "001", "001", 12345678, "PRODUCTION", LocalDate.of(2026, 1, 15)),
                generator.build("1792010400001", "04", "002", "003", 99999999, "SISTEMA", LocalDate.of(2026, 3, 31)),
                generator.build("0999999999001", "18", "010", "010", 1, "PRODUCTION", LocalDate.of(2025, 12, 31)),
                generator.build("1234567890123", "07", "999", "999", 55555555, "SISTEMA", LocalDate.of(2026, 6, 1))
        };

        // Then: para cada clave, el cuerpo (48 dígitos) y el verificador coinciden
        for (String key : keys) {
            String body = key.substring(0, 48);
            char verificador = key.charAt(48);
            assertEquals(checkDigitIndependiente(body), verificador,
                    "Dígito verificador inválido para la clave " + key);
        }
    }

    @Test
    void mod11CoincideConLaImplementacionIndependienteParaCuerposFijos() {
        // When / Then
        String cuerpo38 = "383838383838383838383838383838383838383838383838";
        assertEquals(checkDigitIndependiente(cuerpo38),
                AccessKeyGenerator.mod11CheckDigit(cuerpo38));
        String cuerpo12 = "123456789012345678901234567890123456789012345678";
        assertEquals(checkDigitIndependiente(cuerpo12),
                AccessKeyGenerator.mod11CheckDigit(cuerpo12));
    }

    @Test
    void build_esDeterministaEnElCodigoNumerico() {
        // When: dos claves con los mismos datos de entrada
        String first = generator.build("1790010010001", "01", "001", "001", 12345678, "PRODUCTION", LocalDate.of(2026, 1, 15));
        String second = generator.build("1790010010001", "01", "001", "001", 12345678, "PRODUCTION", LocalDate.of(2026, 1, 15));

        // Then: el prefijo determinista (fecha+ruc+tipo+estab+punto+secuencial = 38 dígitos) y el ambiente son deterministas;
        // los 8 dígitos aleatorios (posiciones 38..45) pueden variar entre emisiones.
        assertEquals(first.substring(0, 38), second.substring(0, 38));
        assertEquals(first.charAt(46), second.charAt(46));
    }

    @Test
    void build_rucMasLargoSeTruncaAUltimos13Digitos() {
        // When: RUC de 16 dígitos => se quedan los últimos 13
        String key = generator.build("1234567890123456", "01", "001", "001", 1, "SISTEMA", LocalDate.of(2026, 1, 15));

        // Then
        assertEquals("4567890123456", key.substring(8, 21));
    }

    @Test
    void build_cuerpoSiempreTiene48DigitosConEntradasMinimas() {
        // When: campos cortos y secuencial pequeño
        String key = generator.build("1", "1", "1", "1", 1, "SISTEMA", LocalDate.of(2026, 1, 5));

        // Then
        assertEquals(49, key.length());
        assertEquals("20260105", key.substring(0, 8), "Fecha AAAAMMDD");
        assertEquals("0000000000001", key.substring(8, 21), "RUC con ceros a la izquierda");
        assertEquals("000000001", key.substring(29, 38), "Secuencial 9 dígitos");
    }
}
