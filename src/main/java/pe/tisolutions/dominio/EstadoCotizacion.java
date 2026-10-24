package pe.tisolutions.dominio;

import java.util.EnumSet;
import java.util.Set;

/**
 * Estados de una cotización y transiciones permitidas.
 *
 * BORRADOR --> EMITIDA --> EN_NEGOCIACION --> ACEPTADA / RECHAZADA / SIN_RESPUESTA
 * Solo BORRADOR es editable. Una vez emitida, los precios quedan congelados;
 * para cambiarlos se crea una nueva versión (que al emitirse deja a la original como REEMPLAZADA).
 */
public enum EstadoCotizacion {

    BORRADOR("Borrador"),
    EMITIDA("Emitida"),
    EN_NEGOCIACION("En negociación"),
    ACEPTADA("Aceptada"),
    RECHAZADA("Rechazada"),
    SIN_RESPUESTA("Sin respuesta"),
    ANULADA("Anulada"),
    REEMPLAZADA("Reemplazada");

    private final String etiqueta;

    EstadoCotizacion(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String getEtiqueta() {
        return etiqueta;
    }

    /** Estados a los que el usuario puede pasar manualmente desde el estado actual. */
    public Set<EstadoCotizacion> siguientes() {
        return switch (this) {
            case BORRADOR -> EnumSet.of(EMITIDA);
            case EMITIDA -> EnumSet.of(EN_NEGOCIACION, ACEPTADA, RECHAZADA, SIN_RESPUESTA, ANULADA);
            case EN_NEGOCIACION -> EnumSet.of(ACEPTADA, RECHAZADA, SIN_RESPUESTA, ANULADA);
            case SIN_RESPUESTA -> EnumSet.of(EN_NEGOCIACION, ACEPTADA, RECHAZADA, ANULADA);
            case ACEPTADA -> EnumSet.of(ANULADA);
            case RECHAZADA, ANULADA, REEMPLAZADA -> EnumSet.noneOf(EstadoCotizacion.class);
        };
    }

    public boolean puedeCambiarA(EstadoCotizacion destino) {
        return siguientes().contains(destino);
    }

    public boolean esEditable() {
        return this == BORRADOR;
    }

    /** Se puede crear una nueva versión mientras la cotización siga abierta con el cliente. */
    public boolean permiteNuevaVersion() {
        return this == EMITIDA || this == EN_NEGOCIACION || this == SIN_RESPUESTA;
    }

    public boolean requiereMotivo() {
        return this == RECHAZADA || this == SIN_RESPUESTA || this == ANULADA;
    }
}
