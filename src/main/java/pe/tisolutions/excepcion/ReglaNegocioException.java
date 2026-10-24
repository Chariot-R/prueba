package pe.tisolutions.excepcion;

/** La operación no está permitida por una regla del negocio (HTTP 409). */
public class ReglaNegocioException extends RuntimeException {

    public ReglaNegocioException(String mensaje) {
        super(mensaje);
    }
}
