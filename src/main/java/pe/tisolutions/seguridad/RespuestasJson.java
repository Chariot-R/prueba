package pe.tisolutions.seguridad;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.csrf.CsrfException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/** Respuestas de seguridad en el mismo formato JSON que el resto de errores de la API. */
final class RespuestasJson {

    private RespuestasJson() {
    }

    static void noAutenticado(HttpServletRequest req, HttpServletResponse res, AuthenticationException e)
            throws IOException {
        escribir(res, 401, "NO_AUTENTICADO", "Tu sesión terminó o no has iniciado sesión.");
    }

    static void denegado(HttpServletRequest req, HttpServletResponse res, AccessDeniedException e)
            throws IOException {
        if (e instanceof CsrfException) {
            // Falta o venció el token anti-falsificación (p. ej. justo después de cerrar sesión).
            // El frontend pide uno nuevo y reintenta solo.
            escribir(res, 403, "CSRF", "La página estaba desactualizada. Vuelve a intentarlo.");
            return;
        }
        escribir(res, 403, "DENEGADO", "No tienes permiso para realizar esta acción.");
    }

    static void escribir(HttpServletResponse res, int status, String tipo, String mensaje) throws IOException {
        res.setStatus(status);
        res.setContentType("application/json");
        res.setCharacterEncoding(StandardCharsets.UTF_8.name());
        res.getWriter().write("{\"status\":" + status + ",\"tipo\":\"" + tipo + "\",\"mensaje\":\""
                + mensaje + "\",\"errores\":[]}");
    }
}
