package pe.tisolutions.web.pagina;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import pe.tisolutions.dominio.EstadoCotizacion;
import pe.tisolutions.dto.CotizacionResponse;
import pe.tisolutions.dto.CotizacionResumen;
import pe.tisolutions.excepcion.RecursoNoEncontradoException;
import pe.tisolutions.repositorio.ClienteRepository;
import pe.tisolutions.servicio.CotizacionService;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Páginas Thymeleaf del módulo. Las escrituras las hace el formulario vía la API JSON. */
@Controller
public class CotizacionPaginaController {

    private final CotizacionService servicio;
    private final ClienteRepository clientes;
    private final BigDecimal margenDefecto;
    private final BigDecimal tasaIgv;

    public CotizacionPaginaController(CotizacionService servicio, ClienteRepository clientes,
                                      @Value("${app.margen.defecto}") BigDecimal margenDefecto,
                                      @Value("${app.igv.tasa}") BigDecimal tasaIgv) {
        this.servicio = servicio;
        this.clientes = clientes;
        this.margenDefecto = margenDefecto;
        this.tasaIgv = tasaIgv;
    }

    @GetMapping("/")
    public String inicio() {
        return "redirect:/cotizaciones";
    }

    @GetMapping("/cotizaciones")
    public String lista(@RequestParam(required = false) EstadoCotizacion estado,
                        @RequestParam(required = false) String texto,
                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
                        @RequestParam(defaultValue = "false") boolean incluirCerradas,
                        @RequestParam(defaultValue = "0") int pagina,
                        Model model) {
        Page<CotizacionResumen> resultado = servicio.listar(estado, texto, desde, hasta, incluirCerradas,
                PageRequest.of(Math.max(pagina, 0), 15, Sort.by(Sort.Direction.DESC, "fecha", "id")));
        model.addAttribute("pagina", resultado);
        model.addAttribute("estados", EstadoCotizacion.values());
        model.addAttribute("fEstado", estado);
        model.addAttribute("fTexto", texto);
        model.addAttribute("fDesde", desde);
        model.addAttribute("fHasta", hasta);
        model.addAttribute("fIncluirCerradas", incluirCerradas);
        return "cotizaciones/lista";
    }

    @GetMapping("/cotizaciones/nueva")
    public String nueva(Model model) {
        model.addAttribute("modo", "nueva");
        model.addAttribute("tituloPagina", "Nueva cotización");
        model.addAttribute("cotizacionId", null);
        model.addAttribute("margenDefecto", margenDefecto);
        model.addAttribute("tasaIgv", tasaIgv);
        model.addAttribute("clientes", clientes.findByEstadoOrderByNombreRazonSocialAsc("ACTIVO"));
        return "cotizaciones/formulario";
    }

    @GetMapping("/cotizaciones/{id}/editar")
    public String editar(@PathVariable Integer id, Model model) {
        CotizacionResponse c = servicio.obtener(id);
        if (!c.editable()) {
            return "redirect:/cotizaciones/" + id;     // ya emitida: solo lectura
        }
        model.addAttribute("modo", "editar");
        model.addAttribute("cotizacionId", id);
        model.addAttribute("tituloPagina", "Editar cotización " + c.codigoCompleto());
        model.addAttribute("margenDefecto", margenDefecto);
        model.addAttribute("tasaIgv", tasaIgv);
        model.addAttribute("clientes", clientes.findByEstadoOrderByNombreRazonSocialAsc("ACTIVO"));
        return "cotizaciones/formulario";
    }

    @GetMapping("/cotizaciones/{id}")
    public String ver(@PathVariable Integer id, Model model) {
        model.addAttribute("c", servicio.obtener(id));
        return "cotizaciones/ver";
    }

    @ExceptionHandler(RecursoNoEncontradoException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String noEncontrado(RecursoNoEncontradoException ex, Model model) {
        model.addAttribute("mensaje", ex.getMessage());
        return "error/404";
    }
}
