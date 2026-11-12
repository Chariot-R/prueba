package pe.tisolutions.web.api;

import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import pe.tisolutions.dominio.Usuario;
import pe.tisolutions.dto.AccesoDtos.*;
import pe.tisolutions.repositorio.*;
import pe.tisolutions.seguridad.Correo;
import pe.tisolutions.seguridad.UsuarioActual;
import pe.tisolutions.servicio.GestionUsuariosService;

import java.time.LocalDateTime;
import java.util.List;

/** Panel del ADMINISTRADOR: resumen, bitácora de accesos y cuentas del personal (módulo 4.4). */
@RestController
@RequestMapping("/api/intranet/admin")
public class AdminApiController {

    private final UsuarioRepository usuarios;
    private final RolRepository roles;
    private final CuentaClienteRepository cuentasCliente;
    private final CotizacionRepository cotizaciones;
    private final RegistroAccesoRepository accesos;
    private final GestionUsuariosService gestion;
    private final UsuarioActual usuarioActual;

    public AdminApiController(UsuarioRepository usuarios, RolRepository roles, CuentaClienteRepository cuentasCliente,
                              CotizacionRepository cotizaciones, RegistroAccesoRepository accesos,
                              GestionUsuariosService gestion, UsuarioActual usuarioActual) {
        this.usuarios = usuarios;
        this.roles = roles;
        this.cuentasCliente = cuentasCliente;
        this.cotizaciones = cotizaciones;
        this.accesos = accesos;
        this.gestion = gestion;
        this.usuarioActual = usuarioActual;
    }

    @GetMapping("/resumen")
    @Transactional(readOnly = true)
    public ResumenAdminDto resumen() {
        return new ResumenAdminDto(usuarios.countByEstado("ACTIVO"), cuentasCliente.count(), cotizaciones.count(),
                accesos.countByExitoFalseAndFechaAfter(LocalDateTime.now().minusHours(24)),
                accesos.findTop20ByOrderByFechaDescIdDesc().stream()
                        .map(a -> new AccesoDto(a.getFecha(), a.getTipoCuenta(), a.getEmail(), a.isExito(), a.getMotivo(), a.getIp()))
                        .toList());
    }

    @GetMapping("/roles")
    @Transactional(readOnly = true)
    public List<RolDto> roles() {
        return roles.findAllByOrderByIdAsc().stream().map(r -> new RolDto(r.getId(), r.getNombre(), r.getEtiqueta())).toList();
    }

    @GetMapping("/usuarios")
    @Transactional(readOnly = true)
    public List<UsuarioFilaDto> usuarios() {
        Integer yo = usuarioActual.idPersonal();
        LocalDateTime ahora = LocalDateTime.now();
        return usuarios.findAllByOrderByNombreAsc().stream().map(u -> fila(u, yo, ahora)).toList();
    }

    @PostMapping("/usuarios")
    @ResponseStatus(HttpStatus.CREATED)
    public ClaveTemporalDto crear(@RequestBody CrearUsuarioRequest req) {
        String temporal = gestion.crear(req.nombre(), req.email(), req.rolId());
        return new ClaveTemporalDto(Correo.normalizar(req.email()), temporal);
    }

    @PutMapping("/usuarios/{id}")
    public void editar(@PathVariable Integer id, @RequestBody EditarUsuarioRequest req) {
        gestion.editar(id, req.nombre(), req.rolId(), usuarioActual.idPersonal());
    }

    @PostMapping("/usuarios/{id}/restablecer")
    public ClaveTemporalDto restablecer(@PathVariable Integer id) {
        String temporal = gestion.restablecerPassword(id);
        return new ClaveTemporalDto(usuarios.findById(id).map(Usuario::getEmail).orElse(""), temporal);
    }

    @PutMapping("/usuarios/{id}/estado")
    public void cambiarEstado(@PathVariable Integer id, @RequestBody EstadoUsuarioRequest req) {
        gestion.cambiarEstado(id, req.estado(), usuarioActual.idPersonal());
    }

    private static UsuarioFilaDto fila(Usuario u, Integer yo, LocalDateTime ahora) {
        return new UsuarioFilaDto(u.getId(), u.getNombre(), u.getEmail(), u.getRol().getId(), u.getRol().getNombre(),
                u.getRol().getEtiqueta(), u.getEstado(), u.estaBloqueadaTemporalmente(ahora), u.getBloqueadoHasta(),
                u.isDebeCambiarPassword(), u.getUltimoAcceso(), u.getId().equals(yo));
    }
}
