package pe.tisolutions.excepcion;

import java.util.List;

/** Datos inválidos (HTTP 422). Lleva TODOS los errores encontrados, no solo el primero. */
public class ValidacionException extends RuntimeException {

    private final List<ErrorCampo> errores;

    public ValidacionException(List<ErrorCampo> errores) {
        super("Hay datos por corregir");
        this.errores = List.copyOf(errores);
    }

    public List<ErrorCampo> getErrores() {
        return errores;
    }
}
