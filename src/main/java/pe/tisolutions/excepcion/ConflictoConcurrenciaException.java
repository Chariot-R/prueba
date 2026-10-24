package pe.tisolutions.excepcion;

/** Otro usuario modificó el registro después de que este usuario lo abrió (HTTP 409). */
public class ConflictoConcurrenciaException extends RuntimeException {

    public ConflictoConcurrenciaException() {
        super("Otro usuario modificó esta cotización mientras la editaba. Recargue la página para ver los cambios.");
    }
}
