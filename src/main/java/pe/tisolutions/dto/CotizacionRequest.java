package pe.tisolutions.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * Lo que envía el formulario al guardar: la cabecera y TODAS sus líneas juntas.
 * No incluye subtotal/igv/total ni importes: el servidor siempre los recalcula.
 */
public record CotizacionRequest(

        @NotNull(message = "Seleccione un cliente")
        Integer clienteId,

        Integer solicitudId,

        @NotNull(message = "Ingrese el margen de ganancia")
        @DecimalMin(value = "0", message = "El margen no puede ser negativo")
        @DecimalMax(value = "100", message = "El margen no puede superar 100 %")
        @Digits(integer = 3, fraction = 2, message = "Máximo 2 decimales")
        BigDecimal margenAplicado,

        @NotNull(message = "Ingrese los días de validez")
        @Min(value = 1, message = "Mínimo 1 día")
        @Max(value = 90, message = "Máximo 90 días")
        Integer diasValidez,

        @Size(max = 500, message = "Máximo 500 caracteres")
        String observaciones,

        /** Obligatorio al editar: versión de la fila que tenía el usuario al abrir el formulario. */
        Integer filaVersion,

        @NotEmpty(message = "Agregue al menos una línea a la cotización")
        @Size(max = 200, message = "Máximo 200 líneas por cotización")
        List<@Valid @NotNull LineaRequest> lineas
) {
}
