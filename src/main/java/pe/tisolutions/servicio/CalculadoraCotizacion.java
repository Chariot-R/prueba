package pe.tisolutions.servicio;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;

/**
 * Cálculos de la cotización. Todos los precios INCLUYEN IGV.
 *
 *   precio_unitario = ROUND(costo_unitario × (100 + margen) / 100, 2)
 *   importe         = ROUND(cantidad × precio_unitario, 2)
 *   total           = Σ importe                      (precio de venta, con IGV)
 *   subtotal        = ROUND(total / (1 + tasa_igv), 2)  (valor de venta, sin IGV)
 *   igv             = total − subtotal               (así subtotal + igv = total, sin descuadres de centavos)
 *
 * Se usa BigDecimal con HALF_UP, que para montos positivos redondea igual que ROUND() de SQL Server
 * (el script de BD valida importe = ROUND(cantidad * precio_unitario, 2)).
 * Clase sin dependencias de Spring para poder probarla de forma aislada.
 */
public final class CalculadoraCotizacion {

    private static final BigDecimal CIEN = BigDecimal.valueOf(100);

    private CalculadoraCotizacion() {
    }

    public static BigDecimal precioVenta(BigDecimal costoUnitario, BigDecimal margenPct) {
        return costoUnitario.multiply(CIEN.add(margenPct))
                .divide(CIEN, 2, RoundingMode.HALF_UP);
    }

    public static BigDecimal importe(BigDecimal cantidad, BigDecimal precioUnitario) {
        return cantidad.multiply(precioUnitario).setScale(2, RoundingMode.HALF_UP);
    }

    public static Totales totales(Collection<BigDecimal> importes, BigDecimal tasaIgv) {
        BigDecimal total = importes.stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal subtotal = total.divide(BigDecimal.ONE.add(tasaIgv), 2, RoundingMode.HALF_UP);
        BigDecimal igv = total.subtract(subtotal);
        return new Totales(subtotal, igv, total);
    }

    public record Totales(BigDecimal subtotal, BigDecimal igv, BigDecimal total) {
    }
}
