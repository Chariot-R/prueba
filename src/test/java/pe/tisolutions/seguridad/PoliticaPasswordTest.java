package pe.tisolutions.seguridad;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PoliticaPasswordTest {

    @Test
    void clienteNecesitaOchoCaracteresConLetrasYNumeros() {
        assertNull(PoliticaPassword.problemaCliente("casa2024x", "juan@gmail.com"));
        assertNotNull(PoliticaPassword.problemaCliente("abc12", "juan@gmail.com"));       // corta
        assertNotNull(PoliticaPassword.problemaCliente("soloLetras", "juan@gmail.com"));  // sin números
        assertNotNull(PoliticaPassword.problemaCliente("12345678", "juan@gmail.com"));    // común
        assertNotNull(PoliticaPassword.problemaCliente("juanperez1", "juanperez@gmail.com")); // contiene el correo
    }

    @Test
    void personalNecesitaDiezConMayusculasMinusculasYNumeros() {
        assertNull(PoliticaPassword.problemaPersonal("Ventas2026Lima", "mlopez@tisolutions.pe"));
        assertNotNull(PoliticaPassword.problemaPersonal("ventas2026lima", "mlopez@tisolutions.pe"));
        assertNotNull(PoliticaPassword.problemaPersonal("Ventas26", "mlopez@tisolutions.pe"));
    }

    @Test
    void temporalCumpleLaPoliticaYEsDistintaCadaVez() {
        String a = PoliticaPassword.generarTemporal();
        String b = PoliticaPassword.generarTemporal();
        assertNull(PoliticaPassword.problemaPersonal(a, ""));
        assertNotEquals(a, b);
    }
}
