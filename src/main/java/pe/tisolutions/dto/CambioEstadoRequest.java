package pe.tisolutions.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import pe.tisolutions.dominio.EstadoCotizacion;

public record CambioEstadoRequest(
        @NotNull(message = "Indique el nuevo estado")
        EstadoCotizacion estado,

        @Size(max = 300, message = "Máximo 300 caracteres")
        String motivo,

        @NotNull(message = "Falta la versión de la cotización; recargue la página")
        Integer filaVersion
) {
}
