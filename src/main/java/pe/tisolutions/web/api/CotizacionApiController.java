package pe.tisolutions.web.api;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import pe.tisolutions.dominio.EstadoCotizacion;
import pe.tisolutions.dto.*;
import pe.tisolutions.repositorio.ClienteRepository;
import pe.tisolutions.servicio.CotizacionService;

import java.math.BigDecimal;
import java.net.URI;
import java.time.LocalDate;
import java.util.Arrays;

/** API de cotizaciones (Ventas y Administrador). Los errores los formatea ManejadorErroresApi. */
@RestController
@RequestMapping("/api/intranet/cotizaciones")
public class CotizacionApiController {

    private final CotizacionService servicio;
    private final ClienteRepository clientes;
    private final BigDecimal margenDefecto;
    private final BigDecimal tasaIgv;

    public CotizacionApiController(CotizacionService servicio, ClienteRepository clientes,
                                   @Value("${app.margen.defecto}") BigDecimal margenDefecto,
                                   @Value("${app.igv.tasa}") BigDecimal tasaIgv) {
        this.servicio = servicio;
        this.clientes = clientes;
        this.margenDefecto = margenDefecto;
        this.tasaIgv = tasaIgv;
    }

    @GetMapping
    public PaginaDto<CotizacionResumen> listar(@RequestParam(required = false) EstadoCotizacion estado,
                                               @RequestParam(required = false) String texto,
                                               @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                                               @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
                                               @RequestParam(defaultValue = "false") boolean incluirCerradas,
                                               @RequestParam(defaultValue = "0") int pagina) {
        return PaginaDto.de(servicio.listar(estado, texto, desde, hasta, incluirCerradas,
                PageRequest.of(Math.max(pagina, 0), 15, Sort.by(Sort.Direction.DESC, "fecha", "id"))));
    }

    /** Margen por defecto, IGV, estados y clientes activos para el formulario y los filtros. */
    @GetMapping("/config")
    @Transactional(readOnly = true)
    public ConfigCotizacionDto config() {
        return new ConfigCotizacionDto(margenDefecto, tasaIgv,
                Arrays.stream(EstadoCotizacion.values())
                        .map(e -> new ConfigCotizacionDto.Opcion(e.name(), e.getEtiqueta())).toList(),
                clientes.findByEstadoOrderByNombreRazonSocialAsc("ACTIVO").stream()
                        .map(c -> new ConfigCotizacionDto.ClienteOpcion(c.getId(), c.getNombreRazonSocial(), c.getDocumento()))
                        .toList());
    }

    @GetMapping("/{id}")
    public CotizacionResponse obtener(@PathVariable Integer id) {
        return servicio.obtener(id);
    }

    @PostMapping
    public ResponseEntity<CotizacionResponse> crear(@Valid @RequestBody CotizacionRequest req) {
        CotizacionResponse r = servicio.crear(req);
        return ResponseEntity.created(URI.create("/api/intranet/cotizaciones/" + r.id())).body(r);
    }

    @PutMapping("/{id}")
    public CotizacionResponse actualizar(@PathVariable Integer id, @Valid @RequestBody CotizacionRequest req) {
        return servicio.actualizar(id, req);
    }

    @PostMapping("/{id}/estado")
    public CotizacionResponse cambiarEstado(@PathVariable Integer id, @Valid @RequestBody CambioEstadoRequest req) {
        return servicio.cambiarEstado(id, req);
    }

    @PostMapping("/{id}/nueva-version")
    @ResponseStatus(HttpStatus.CREATED)
    public CotizacionResponse nuevaVersion(@PathVariable Integer id) {
        return servicio.nuevaVersion(id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable Integer id) {
        servicio.eliminar(id);
    }
}
