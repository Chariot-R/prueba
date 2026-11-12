package pe.tisolutions.servicio;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ValidadorDocumentoTest {

    @Test
    void dniDeOchoDigitos() {
        assertTrue(ValidadorDocumento.dniValido("45678912"));
        assertFalse(ValidadorDocumento.dniValido("4567891"));
        assertFalse(ValidadorDocumento.dniValido("4567891A"));
        assertFalse(ValidadorDocumento.dniValido(null));
    }

    @Test
    void rucConDigitoVerificadorCorrecto() {
        assertTrue(ValidadorDocumento.rucValido("20131312955"));   // SUNAT
        assertTrue(ValidadorDocumento.rucValido("20512345671"));
    }

    @Test
    void rucInvalido() {
        assertFalse(ValidadorDocumento.rucValido("20131312954"));  // dígito verificador mal
        assertFalse(ValidadorDocumento.rucValido("30131312955"));  // prefijo no existe
        assertFalse(ValidadorDocumento.rucValido("2013131295"));   // 10 dígitos
        assertFalse(ValidadorDocumento.rucValido(null));
    }
}
