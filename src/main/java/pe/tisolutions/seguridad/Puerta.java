package pe.tisolutions.seguridad;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.web.context.SecurityContextRepository;

/**
 * Una de las dos entradas al sistema (personal o clientes).
 * Cada una valida contra su propia tabla y guarda la sesión bajo su propia clave.
 */
public record Puerta(TipoCuenta tipo, AuthenticationManager gestor, SecurityContextRepository repositorio) {
}
