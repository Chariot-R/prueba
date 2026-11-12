package pe.tisolutions.seguridad;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/** Acceso a quién está usando el sistema en esta petición. */
@Component
public class UsuarioActual {

    public CuentaAutenticada cuenta() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof CuentaAutenticada c) {
            return c;
        }
        throw new AccessDeniedException("No hay una sesión iniciada");
    }

    /** Id en la tabla usuario del trabajador que inició sesión. */
    public Integer idPersonal() {
        CuentaAutenticada c = cuenta();
        if (c.getTipo() != TipoCuenta.PERSONAL) {
            throw new AccessDeniedException("Solo el personal puede hacer esto");
        }
        return c.getId();
    }

    /** Id en la tabla cliente del cliente que inició sesión. */
    public Integer idCliente() {
        CuentaAutenticada c = cuenta();
        if (c.getTipo() != TipoCuenta.CLIENTE) {
            throw new AccessDeniedException("Solo un cliente puede hacer esto");
        }
        return c.getClienteId();
    }

    /** Tras cambiar la contraseña, la sesión deja de exigir el cambio. */
    public void marcarPasswordCambiada() {
        cuenta().passwordCambiada();
    }
}
