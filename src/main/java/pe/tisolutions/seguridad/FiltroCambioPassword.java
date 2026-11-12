package pe.tisolutions.seguridad;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;

/**
 * Si un trabajador entró con una contraseña temporal (puesta por el administrador), la API
 * rechaza todo (403 CAMBIO_PASSWORD) hasta que la cambie.
 * No es @Component a propósito: solo se usa dentro de la cadena de la intranet.
 */
class FiltroCambioPassword extends OncePerRequestFilter {

    private static final Set<String> PERMITIDAS = Set.of(
            "/api/intranet/auth/yo", "/api/intranet/auth/cambiar-password", "/api/intranet/auth/logout");

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof CuentaAutenticada c && c.isDebeCambiarPassword()) {
            String ruta = request.getRequestURI().substring(request.getContextPath().length());
            if (!PERMITIDAS.contains(ruta)) {
                RespuestasJson.escribir(response, 403, "CAMBIO_PASSWORD",
                        "Debes cambiar tu contraseña temporal antes de continuar.");
                return;
            }
        }
        chain.doFilter(request, response);
    }
}
