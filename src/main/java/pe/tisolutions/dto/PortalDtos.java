package pe.tisolutions.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** Objetos JSON del portal de clientes. */
public final class PortalDtos {

    private PortalDtos() {
    }

    public record MiCuentaDto(String nombre, String contacto, boolean empresa, String documento, String email,
                              String telefono, List<SolicitudDto> solicitudes, List<CotizacionClienteDto> cotizaciones) {
    }

    public record SolicitudDto(Integer id, LocalDateTime fecha, String descripcion, String estado) {
    }

    public record CotizacionClienteDto(String codigo, LocalDateTime fecha, LocalDateTime vence, BigDecimal total,
                                       String estado, String estadoEtiqueta) {
    }

    public record NuevaSolicitudRequest(String descripcion) {
    }
}
