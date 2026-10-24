package pe.tisolutions.servicio;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CalculadoraCotizacionTest {

    private static BigDecimal bd(String v) {
        return new BigDecimal(v);
    }

    @Test
    void precioVentaAplicaMargenYRedondeaADosDecimales() {
        assertEquals(bd("156.00"), CalculadoraCotizacion.precioVenta(bd("120.00"), bd("30")));
        assertEquals(bd("3.25"), CalculadoraCotizacion.precioVenta(bd("2.50"), bd("30")));
        // 10.05 * 1.155 = 11.60775 -> 11.61 (HALF_UP)
        assertEquals(bd("11.61"), CalculadoraCotizacion.precioVenta(bd("10.05"), bd("15.5")));
        assertEquals(bd("99.90"), CalculadoraCotizacion.precioVenta(bd("99.90"), bd("0")));
    }

    @Test
    void importeEsCantidadPorPrecio() {
        assertEquals(bd("162.50"), CalculadoraCotizacion.importe(bd("50"), bd("3.25")));
        assertEquals(bd("12.35"), CalculadoraCotizacion.importe(bd("2.5"), bd("4.94")));
    }

    @Test
    void totalesConIgvIncluido() {
        var t = CalculadoraCotizacion.totales(
                List.of(bd("624.00"), bd("455.00"), bd("162.50"), bd("400.00")), bd("0.18"));
        assertEquals(bd("1641.50"), t.total());
        assertEquals(bd("1391.10"), t.subtotal());
        assertEquals(bd("250.40"), t.igv());
        assertEquals(t.total(), t.subtotal().add(t.igv()));   // nunca descuadra
    }

    @Test
    void sinLineasTodoEsCero() {
        var t = CalculadoraCotizacion.totales(List.of(), bd("0.18"));
        assertEquals(bd("0.00"), t.total());
        assertEquals(bd("0.00"), t.subtotal());
        assertEquals(bd("0.00"), t.igv());
    }
}
