package pe.tisolutions.excepcion;

/** Inicio de sesión rechazado (HTTP 401). El mensaje ya es apto para mostrar al usuario. */
public class CredencialesInvalidasException extends RuntimeException {

    public CredencialesInvalidasException(String mensaje) {
        super(mensaje);
    }
}
