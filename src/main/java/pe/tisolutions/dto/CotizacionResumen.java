package pe.tisolutions.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Fila del listado de cotizaciones. */
public record CotizacionResumen(
        Integer id,
        String codigoCompleto,
        String clienteNombre,
        LocalDateTime fecha,
        String estado,
        String estadoEtiqueta,
        BigDecimal montoTotal
) {
}
