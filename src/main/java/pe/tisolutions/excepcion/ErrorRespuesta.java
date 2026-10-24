package pe.tisolutions.excepcion;

import java.time.LocalDateTime;
import java.util.List;

/** Formato único de error que recibe la interfaz. */
public record ErrorRespuesta(
        int status,
        String tipo,
        String mensaje,
        List<ErrorCampo> errores,
        String referencia,
        LocalDateTime fecha
) {
    public static ErrorRespuesta de(int status, String tipo, String mensaje) {
        return new ErrorRespuesta(status, tipo, mensaje, List.of(), null, LocalDateTime.now());
    }
}
