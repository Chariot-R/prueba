package pe.tisolutions.web.api;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.*;
import pe.tisolutions.dto.AccesoDtos.LoginRequest;
import pe.tisolutions.dto.AccesoDtos.SesionDto;
import pe.tisolutions.dto.RegistroClienteForm;
import pe.tisolutions.excepcion.ErrorCampo;
import pe.tisolutions.excepcion.ValidacionException;
import pe.tisolutions.seguridad.CuentaAutenticada;
import pe.tisolutions.seguridad.Puerta;
import pe.tisolutions.seguridad.ServicioLogin;
import pe.tisolutions.servicio.RegistroClienteService;

import java.util.List;

/** Sesión y registro de CLIENTES (persona natural o empresa). */
@RestController
@RequestMapping("/api/portal/auth")
public class AuthPortalController {

    private final Puerta puerta;
    private final ServicioLogin login;
    private final RegistroClienteService registro;

    public AuthPortalController(@Qualifier("puertaCliente") Puerta puerta, ServicioLogin login,
                                RegistroClienteService registro) {
        this.puerta = puerta;
        this.login = login;
        this.registro = registro;
    }

    @PostMapping("/login")
    public SesionDto login(@RequestBody LoginRequest req, HttpServletRequest request, HttpServletResponse response) {
        return SesionDto.de(login.iniciar(puerta, req.email(), req.password(), request, response));
    }

    @GetMapping("/yo")
    public ResponseEntity<SesionDto> yo(Authentication auth) {
        if (auth != null && auth.getPrincipal() instanceof CuentaAutenticada c) {
            return ResponseEntity.ok(SesionDto.de(c));
        }
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/registro")
    @ResponseStatus(HttpStatus.CREATED)
    public void registrar(@Valid @RequestBody RegistroClienteForm form, BindingResult errores) {
        if (form.getSitioWeb() != null && !form.getSitioWeb().isEmpty()) {
            return;                         // campo trampa lleno: robot. No se guarda nada ni se dan pistas.
        }
        registro.validar(form, errores);
        if (errores.hasErrors()) {
            List<ErrorCampo> lista = errores.getAllErrors().stream()
                    .map(e -> new ErrorCampo(e instanceof FieldError fe ? fe.getField() : "", e.getDefaultMessage()))
                    .toList();
            throw new ValidacionException(lista);
        }
        registro.registrar(form);
    }
}
