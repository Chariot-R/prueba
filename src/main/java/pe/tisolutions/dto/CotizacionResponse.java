package pe.tisolutions.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** Cotización completa (maestro + detalle) para consultar o editar. */
public record CotizacionResponse(
        Integer id,
        String codigo,
        String codigoCompleto,
        Integer numeroVersion,
        String codigoOrigen,
        Integer origenId,
        String estado,
        String estadoEtiqueta,
        Integer clienteId,
        String clienteNombre,
        String clienteDocumento,
        Integer solicitudId,
        String solicitudDescripcion,
        String usuarioNombre,
        LocalDateTime fecha,
        LocalDateTime fechaEmision,
        LocalDateTime fechaVencimiento,
        Integer diasValidez,
        BigDecimal margenAplicado,
        BigDecimal tasaIgv,
        BigDecimal subtotal,
        BigDecimal igv,
        BigDecimal montoTotal,
        String observaciones,
        String motivoNoCierre,
        String motivoAnulacion,
        Integer filaVersion,
        boolean editable,
        boolean eliminable,
        boolean permiteNuevaVersion,
        List<OpcionEstado> estadosSiguientes,
        List<LineaResponse> lineas
) {
    public record OpcionEstado(String valor, String etiqueta, boolean requiereMotivo) {
    }
}
