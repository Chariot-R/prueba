package pe.tisolutions.dto;

import java.math.BigDecimal;
import java.util.List;

/** Datos que necesita el formulario de cotización al abrirse. */
public record ConfigCotizacionDto(BigDecimal margenDefecto, BigDecimal tasaIgv, List<Opcion> estados,
                                  List<ClienteOpcion> clientes) {

    public record Opcion(String valor, String etiqueta) {
    }

    public record ClienteOpcion(Integer id, String nombre, String documento) {
    }
}
