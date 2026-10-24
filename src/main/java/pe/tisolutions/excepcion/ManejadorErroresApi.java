package pe.tisolutions.excepcion;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Traduce cualquier error de la API a un ErrorRespuesta entendible.
 * El usuario nunca ve SQL ni trazas; los errores inesperados se registran en el log con un código de referencia.
 */
@RestControllerAdvice(basePackages = "pe.tisolutions.web.api")
public class ManejadorErroresApi {

    private static final Logger log = LoggerFactory.getLogger(ManejadorErroresApi.class);

    /** Bean Validation sobre el JSON recibido (@Valid). */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorRespuesta> validacionDto(MethodArgumentNotValidException ex) {
        List<ErrorCampo> errores = ex.getBindingResult().getAllErrors().stream()
                .map(e -> new ErrorCampo(e instanceof FieldError fe ? fe.getField() : e.getObjectName(),
                        e.getDefaultMessage()))
                .toList();
        return validacion(errores);
    }

    /** Validaciones de la capa de negocio. */
    @ExceptionHandler(ValidacionException.class)
    public ResponseEntity<ErrorRespuesta> validacionNegocio(ValidacionException ex) {
        return validacion(ex.getErrores());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorRespuesta> jsonInvalido(HttpMessageNotReadableException ex) {
        return responder(HttpStatus.BAD_REQUEST, "FORMATO",
                "Los datos enviados no tienen el formato correcto (revise números y opciones seleccionadas).");
    }

    @ExceptionHandler(ReglaNegocioException.class)
    public ResponseEntity<ErrorRespuesta> reglaNegocio(ReglaNegocioException ex) {
        return responder(HttpStatus.CONFLICT, "REGLA_NEGOCIO", ex.getMessage());
    }

    @ExceptionHandler({ConflictoConcurrenciaException.class, ObjectOptimisticLockingFailureException.class})
    public ResponseEntity<ErrorRespuesta> concurrencia(RuntimeException ex) {
        return responder(HttpStatus.CONFLICT, "CONCURRENCIA", new ConflictoConcurrenciaException().getMessage());
    }

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<ErrorRespuesta> noEncontrado(RecursoNoEncontradoException ex) {
        return responder(HttpStatus.NOT_FOUND, "NO_ENCONTRADO", ex.getMessage());
    }

    /**
     * La base de datos rechazó la operación (última barrera). Si llega aquí es porque una regla
     * no se validó antes; se traduce el código de SQL Server a un mensaje entendible.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorRespuesta> integridad(DataIntegrityViolationException ex) {
        SQLException sql = buscarSqlException(ex);
        int codigo = sql != null ? sql.getErrorCode() : 0;
        String detalle = sql != null && sql.getMessage() != null ? sql.getMessage() : "";
        String referencia = UUID.randomUUID().toString().substring(0, 8);
        log.warn("[{}] Violación de integridad (SQL {}): {}", referencia, codigo, detalle);

        String mensaje;
        if (codigo == 547 && detalle.contains("REFERENCE")) {
            mensaje = "No se puede completar la operación porque existen documentos relacionados.";
        } else if (codigo == 547) {
            mensaje = "Algún dato no cumple las reglas de la cotización (cantidades, precios o estado). Revise y vuelva a intentar.";
        } else if (codigo == 2627 || codigo == 2601) {
            mensaje = "Ya existe un registro con esos datos.";
        } else {
            mensaje = "La base de datos rechazó la operación.";
        }
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorRespuesta(
                409, "INTEGRIDAD", mensaje, List.of(), referencia, LocalDateTime.now()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorRespuesta> inesperado(Exception ex) {
        String referencia = UUID.randomUUID().toString().substring(0, 8);
        log.error("[{}] Error inesperado", referencia, ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ErrorRespuesta(
                500, "INTERNO",
                "Ocurrió un error inesperado. No se guardó ningún cambio. Código de referencia: " + referencia,
                List.of(), referencia, LocalDateTime.now()));
    }

    // ------------------------------------------------------------------

    private ResponseEntity<ErrorRespuesta> validacion(List<ErrorCampo> errores) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(new ErrorRespuesta(
                422, "VALIDACION", "Revise los datos marcados antes de guardar.", errores, null, LocalDateTime.now()));
    }

    private ResponseEntity<ErrorRespuesta> responder(HttpStatus status, String tipo, String mensaje) {
        return ResponseEntity.status(status).body(ErrorRespuesta.de(status.value(), tipo, mensaje));
    }

    private static SQLException buscarSqlException(Throwable t) {
        while (t != null) {
            if (t instanceof SQLException s) {
                return s;
            }
            t = t.getCause();
        }
        return null;
    }
}
