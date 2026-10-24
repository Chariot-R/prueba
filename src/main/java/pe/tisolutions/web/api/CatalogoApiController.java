package pe.tisolutions.web.api;

import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import pe.tisolutions.dominio.EstadoSolicitud;
import pe.tisolutions.dto.ProductoOpcion;
import pe.tisolutions.dto.SolicitudOpcion;
import pe.tisolutions.repositorio.ProductoRepository;
import pe.tisolutions.repositorio.SolicitudRepository;

import java.util.List;
import java.util.Objects;

/** Datos de apoyo para el formulario: búsqueda de productos y solicitudes del cliente. */
@RestController
@RequestMapping("/api")
public class CatalogoApiController {

    private final ProductoRepository productos;
    private final SolicitudRepository solicitudes;

    public CatalogoApiController(ProductoRepository productos, SolicitudRepository solicitudes) {
        this.productos = productos;
        this.solicitudes = solicitudes;
    }

    @GetMapping("/productos")
    @Transactional(readOnly = true)
    public List<ProductoOpcion> buscarProductos(@RequestParam(defaultValue = "") String q) {
        return productos.buscarActivos(q.trim(), PageRequest.of(0, 15)).stream()
                .map(p -> new ProductoOpcion(p.getId(), p.getNombre(), p.getProveedor().getNombre(),
                        p.getUnidad(), p.getPrecio(), p.getStock()))
                .toList();
    }

    @GetMapping("/clientes/{clienteId}/solicitudes")
    @Transactional(readOnly = true)
    public List<SolicitudOpcion> solicitudesAbiertas(@PathVariable Integer clienteId) {
        return solicitudes.findByClienteIdAndEstadoNotOrderByFechaCreacionDesc(clienteId, EstadoSolicitud.CERRADA)
                .stream()
                .map(s -> new SolicitudOpcion(s.getId(),
                        "S-" + String.format("%03d", s.getId()) + " · " + Objects.toString(s.getDescripcion(), s.getCanal()),
                        s.getEstado().name()))
                .toList();
    }
}
