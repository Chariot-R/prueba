package pe.tisolutions.web.api;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pe.tisolutions.dto.CambioEstadoRequest;
import pe.tisolutions.dto.CotizacionRequest;
import pe.tisolutions.dto.CotizacionResponse;
import pe.tisolutions.servicio.CotizacionService;

import java.net.URI;

/** API JSON que usa el formulario maestro-detalle. Los errores los formatea ManejadorErroresApi. */
@RestController
@RequestMapping("/api/cotizaciones")
public class CotizacionApiController {

    private final CotizacionService servicio;

    public CotizacionApiController(CotizacionService servicio) {
        this.servicio = servicio;
    }

    @GetMapping("/{id}")
    public CotizacionResponse obtener(@PathVariable Integer id) {
        return servicio.obtener(id);
    }

    @PostMapping
    public ResponseEntity<CotizacionResponse> crear(@Valid @RequestBody CotizacionRequest req) {
        CotizacionResponse r = servicio.crear(req);
        return ResponseEntity.created(URI.create("/api/cotizaciones/" + r.id())).body(r);
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
