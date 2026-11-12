package pe.tisolutions.web.api;

import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import pe.tisolutions.dominio.Cliente;
import pe.tisolutions.dominio.EstadoCotizacion;
import pe.tisolutions.dominio.Solicitud;
import pe.tisolutions.dto.PortalDtos.*;
import pe.tisolutions.excepcion.ErrorCampo;
import pe.tisolutions.excepcion.ValidacionException;
import pe.tisolutions.repositorio.ClienteRepository;
import pe.tisolutions.repositorio.CotizacionRepository;
import pe.tisolutions.repositorio.SolicitudRepository;
import pe.tisolutions.seguridad.CuentaAutenticada;
import pe.tisolutions.seguridad.UsuarioActual;

import java.util.EnumSet;
import java.util.List;

/**
 * "Mi cuenta" del CLIENTE. Todo se filtra por el cliente de la sesión:
 * un cliente nunca puede ver solicitudes ni cotizaciones de otro.
 */
@RestController
@RequestMapping("/api/portal")
public class PortalController {

    /** Lo que el cliente puede ver: nunca borradores internos ni versiones reemplazadas. */
    private static final EnumSet<EstadoCotizacion> VISIBLES = EnumSet.of(
            EstadoCotizacion.EMITIDA, EstadoCotizacion.EN_NEGOCIACION, EstadoCotizacion.ACEPTADA,
            EstadoCotizacion.RECHAZADA, EstadoCotizacion.SIN_RESPUESTA);

    private final UsuarioActual usuarioActual;
    private final ClienteRepository clientes;
    private final SolicitudRepository solicitudes;
    private final CotizacionRepository cotizaciones;

    public PortalController(UsuarioActual usuarioActual, ClienteRepository clientes,
                            SolicitudRepository solicitudes, CotizacionRepository cotizaciones) {
        this.usuarioActual = usuarioActual;
        this.clientes = clientes;
        this.solicitudes = solicitudes;
        this.cotizaciones = cotizaciones;
    }

    @GetMapping("/mi-cuenta")
    @Transactional(readOnly = true)
    public MiCuentaDto miCuenta() {
        CuentaAutenticada cuenta = usuarioActual.cuenta();
        Integer clienteId = usuarioActual.idCliente();
        Cliente c = clientes.findById(clienteId).orElseThrow();
        return new MiCuentaDto(c.getNombreRazonSocial(), cuenta.getNombre(), c.isEmpresa(), c.getDocumento(),
                cuenta.getEmail(), c.getTelefono(),
                solicitudes.findByClienteIdOrderByFechaCreacionDesc(clienteId).stream()
                        .map(s -> new SolicitudDto(s.getId(), s.getFechaCreacion(), s.getDescripcion(), s.getEstado().name()))
                        .toList(),
                cotizaciones.findByClienteIdAndEstadoInOrderByFechaDesc(clienteId, VISIBLES).stream()
                        .map(k -> new CotizacionClienteDto(k.getCodigoCompleto(), k.getFecha(), k.getFechaVencimiento(),
                                k.getMontoTotal(), k.getEstado().name(), k.getEstado().getEtiqueta()))
                        .toList());
    }

    @PostMapping("/solicitudes")
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public void nuevaSolicitud(@RequestBody NuevaSolicitudRequest req) {
        String d = req.descripcion() == null ? "" : req.descripcion().trim();
        if (d.length() < 10 || d.length() > 300) {
            throw new ValidacionException(List.of(
                    new ErrorCampo("descripcion", "Describe lo que necesitas (entre 10 y 300 caracteres).")));
        }
        solicitudes.save(Solicitud.desdeWeb(clientes.getReferenceById(usuarioActual.idCliente()), d));
    }
}
