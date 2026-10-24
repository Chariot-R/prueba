package pe.tisolutions.dominio;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static pe.tisolutions.dominio.EstadoCotizacion.*;

class EstadoCotizacionTest {

    @Test
    void soloElBorradorEsEditable() {
        for (EstadoCotizacion e : values()) {
            assertEquals(e == BORRADOR, e.esEditable(), e.name());
        }
    }

    @Test
    void transicionesPermitidas() {
        assertTrue(BORRADOR.puedeCambiarA(EMITIDA));
        assertFalse(BORRADOR.puedeCambiarA(ACEPTADA));
        assertTrue(EMITIDA.puedeCambiarA(EN_NEGOCIACION));
        assertTrue(SIN_RESPUESTA.puedeCambiarA(ACEPTADA));
        assertTrue(ACEPTADA.puedeCambiarA(ANULADA));
        assertFalse(ACEPTADA.puedeCambiarA(RECHAZADA));
        assertTrue(RECHAZADA.siguientes().isEmpty());
        assertTrue(ANULADA.siguientes().isEmpty());
        // REEMPLAZADA solo la asigna el sistema al emitir una nueva versión
        for (EstadoCotizacion e : values()) {
            assertFalse(e.puedeCambiarA(REEMPLAZADA));
            assertFalse(e.puedeCambiarA(BORRADOR));
        }
    }

    @Test
    void motivoObligatorio() {
        assertTrue(RECHAZADA.requiereMotivo());
        assertTrue(SIN_RESPUESTA.requiereMotivo());
        assertTrue(ANULADA.requiereMotivo());
        assertFalse(ACEPTADA.requiereMotivo());
    }
}
