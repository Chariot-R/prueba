package pe.tisolutions.seguridad;

import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import pe.tisolutions.repositorio.CuentaClienteRepository;
import pe.tisolutions.repositorio.UsuarioRepository;

/**
 * Cómo se busca una cuenta al iniciar sesión. Cada puerta busca SOLO en su propia tabla:
 * con un correo de cliente no se puede entrar a la intranet, y viceversa.
 */
final class CuentasDetalles {

    private CuentasDetalles() {
    }

    static UserDetailsService personal(UsuarioRepository usuarios) {
        return email -> usuarios.findByEmailIgnoreCase(Correo.normalizar(email))
                .map(CuentaAutenticada::dePersonal)
                .orElseThrow(() -> new UsernameNotFoundException("No existe"));
    }

    static UserDetailsService clientes(CuentaClienteRepository cuentas) {
        return email -> cuentas.findByEmailIgnoreCase(Correo.normalizar(email))
                .map(CuentaAutenticada::deCliente)
                .orElseThrow(() -> new UsernameNotFoundException("No existe"));
    }
}
