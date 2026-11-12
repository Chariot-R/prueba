package pe.tisolutions.seguridad;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Asegura que la cookie XSRF-TOKEN exista desde la primera petición, para que Angular
 * pueda enviarla en la cabecera X-XSRF-TOKEN (incluido el primer inicio de sesión).
 */
class FiltroCookieCsrf extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        CsrfToken token = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
        if (token != null) {
            token.getToken();   // al leerlo se genera y se escribe la cookie si aún no existe
        }
        chain.doFilter(request, response);
    }
}
