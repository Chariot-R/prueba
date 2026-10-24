package pe.tisolutions.dto;

import java.math.BigDecimal;

public record LineaResponse(
        Integer id,
        Integer nroLinea,
        String tipoItem,
        String tipoEtiqueta,
        Integer productoId,
        String proveedorNombre,
        String descripcion,
        String unidad,
        BigDecimal cantidad,
        BigDecimal costoUnitario,
        BigDecimal margenPct,
        BigDecimal precioUnitario,
        BigDecimal importe,
        /** Precio actual del proveedor; si difiere de costoUnitario, la UI lo informa (la línea NO cambia). */
        BigDecimal costoActualProveedor
) {
}
