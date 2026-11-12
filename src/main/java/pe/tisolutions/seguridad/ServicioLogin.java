package pe.tisolutions.seguridad;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.stereotype.Service;
import pe.tisolutions.excepcion.CredencialesInvalidasException;

/** Inicio de sesión por una de las dos puertas (personal o clientes). */
@Service
public class ServicioLogin {

    private final ControlAccesos control;
    private final CsrfTokenRepository csrf;

    public ServicioLogin(ControlAccesos control, CsrfTokenRepository csrf) {
        this.control = control;
        this.csrf = csrf;
    }

    public CuentaAutenticada iniciar(Puerta puerta, String email, String password,
                                     HttpServletRequest request, HttpServletResponse response) {
        String correo = Correo.normalizar(email);
        String ip = request.getRemoteAddr();
        try {
            Authentication auth = puerta.gestor().authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(correo, password == null ? "" : password));

            // Nuevo id de sesión al entrar: evita la "fijación de sesión"
            if (request.getSession(false) != null) {
                request.changeSessionId();
            }
            SecurityContext contexto = SecurityContextHolder.createEmptyContext();
            contexto.setAuthentication(auth);
            SecurityContextHolder.setContext(contexto);
            puerta.repositorio().saveContext(contexto, request, response);

            // Token CSRF nuevo tras iniciar sesión
            csrf.saveToken(null, request, response);
            csrf.saveToken(csrf.generateToken(request), request, response);

            control.exito(puerta.tipo(), correo, ip);
            return (CuentaAutenticada) auth.getPrincipal();
        } catch (LockedException e) {
            control.fallo(puerta.tipo(), correo, "BLOQUEADA", ip);
            throw new CredencialesInvalidasException(mensajeBloqueo());
        } catch (DisabledException e) {
            control.fallo(puerta.tipo(), correo, "DESHABILITADA", ip);
            throw new CredencialesInvalidasException("Tu cuenta no está activa. Comunícate con TI Solutions.");
        } catch (AuthenticationException e) {
            // Mismo mensaje si el correo no existe o si la contraseña está mal:
            // así nadie puede averiguar qué correos están registrados.
            boolean seBloqueo = control.fallo(puerta.tipo(), correo, "CREDENCIALES", ip);
            throw new CredencialesInvalidasException(seBloqueo ? mensajeBloqueo() : "Correo o contraseña incorrectos.");
        }
    }

    private String mensajeBloqueo() {
        return "Por seguridad bloqueamos la cuenta por " + control.getMinutosBloqueo()
                + " minutos tras varios intentos fallidos. Intenta más tarde.";
    }
}
