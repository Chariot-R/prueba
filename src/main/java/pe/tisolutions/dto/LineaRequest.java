package pe.tisolutions.dto;

import jakarta.validation.constraints.*;
import pe.tisolutions.dominio.TipoItem;

import java.math.BigDecimal;

/**
 * Una línea del detalle.
 *  - id == null  -> línea nueva
 *  - id != null  -> línea existente que se modifica
 *  - las líneas existentes que no se envían se eliminan
 * Las reglas que dependen del tipo (producto obligatorio, etc.) se validan en el servicio.
 */
public record LineaRequest(
        Integer id,

        @NotNull(message = "Indique el tipo de línea")
        TipoItem tipoItem,

        Integer productoId,

        @Size(max = 200, message = "Máximo 200 caracteres")
        String descripcion,

        @Size(max = 10, message = "Máximo 10 caracteres")
        String unidad,

        @NotNull(message = "Ingrese la cantidad")
        @DecimalMin(value = "0.01", message = "La cantidad debe ser mayor a 0")
        @Digits(integer = 10, fraction = 2, message = "Máximo 2 decimales")
        BigDecimal cantidad,

        @DecimalMin(value = "0", message = "El costo no puede ser negativo")
        @Digits(integer = 10, fraction = 2, message = "Máximo 2 decimales")
        BigDecimal costoUnitario,

        @DecimalMin(value = "0", message = "El margen no puede ser negativo")
        @DecimalMax(value = "100", message = "El margen no puede superar 100 %")
        @Digits(integer = 3, fraction = 2, message = "Máximo 2 decimales")
        BigDecimal margenPct,

        @DecimalMin(value = "0", message = "El precio no puede ser negativo")
        @Digits(integer = 10, fraction = 2, message = "Máximo 2 decimales")
        BigDecimal precioUnitario
) {
}
