package pe.tisolutions.web.api;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import pe.tisolutions.dto.AccesoDtos.CambioPasswordRequest;
import pe.tisolutions.dto.AccesoDtos.LoginRequest;
import pe.tisolutions.dto.AccesoDtos.SesionDto;
import pe.tisolutions.excepcion.ReglaNegocioException;
import pe.tisolutions.seguridad.CuentaAutenticada;
import pe.tisolutions.seguridad.Puerta;
import pe.tisolutions.seguridad.ServicioLogin;
import pe.tisolutions.seguridad.UsuarioActual;
import pe.tisolutions.servicio.GestionUsuariosService;

/** Sesión del PERSONAL. El cierre de sesión (POST /api/intranet/auth/logout) lo atiende Spring Security. */
@RestController
@RequestMapping("/api/intranet/auth")
public class AuthIntranetController {

    private final Puerta puerta;
    private final ServicioLogin login;
    private final UsuarioActual usuarioActual;
    private final GestionUsuariosService gestion;

    public AuthIntranetController(@Qualifier("puertaPersonal") Puerta puerta, ServicioLogin login,
                                  UsuarioActual usuarioActual, GestionUsuariosService gestion) {
        this.puerta = puerta;
        this.login = login;
        this.usuarioActual = usuarioActual;
        this.gestion = gestion;
    }

    @PostMapping("/login")
    public SesionDto login(@RequestBody LoginRequest req, HttpServletRequest request, HttpServletResponse response) {
        return SesionDto.de(login.iniciar(puerta, req.email(), req.password(), request, response));
    }

    /** 200 con la sesión actual, o 204 si nadie del personal inició sesión en este navegador. */
    @GetMapping("/yo")
    public ResponseEntity<SesionDto> yo(Authentication auth) {
        if (auth != null && auth.getPrincipal() instanceof CuentaAutenticada c) {
            return ResponseEntity.ok(SesionDto.de(c));
        }
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/cambiar-password")
    public SesionDto cambiarPassword(@RequestBody CambioPasswordRequest req) {
        String problema = gestion.cambiarPasswordPropia(usuarioActual.idPersonal(), req.actual(), req.nueva(), req.confirmar());
        if (problema != null) {
            throw new ReglaNegocioException(problema);
        }
        usuarioActual.marcarPasswordCambiada();
        return SesionDto.de(usuarioActual.cuenta());
    }
}
