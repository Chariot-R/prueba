package pe.tisolutions.dto;

import java.math.BigDecimal;

public record ProductoOpcion(Integer id, String nombre, String proveedor, String unidad,
                             BigDecimal precio, Integer stock) {
}
