package pe.tisolutions.seguridad;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.tisolutions.dominio.CuentaConBloqueo;
import pe.tisolutions.dominio.RegistroAcceso;
import pe.tisolutions.repositorio.CuentaClienteRepository;
import pe.tisolutions.repositorio.RegistroAccesoRepository;
import pe.tisolutions.repositorio.UsuarioRepository;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Bloqueo contra "fuerza bruta" (probar contraseñas una tras otra) y bitácora de accesos.
 * Tras N intentos fallidos seguidos la cuenta queda bloqueada M minutos.
 */
@Service
public class ControlAccesos {

    private final UsuarioRepository usuarios;
    private final CuentaClienteRepository cuentasCliente;
    private final RegistroAccesoRepository registros;
    private final int maxIntentos;
    private final int minutosBloqueo;

    public ControlAccesos(UsuarioRepository usuarios, CuentaClienteRepository cuentasCliente,
                          RegistroAccesoRepository registros,
                          @Value("${app.seguridad.max-intentos}") int maxIntentos,
                          @Value("${app.seguridad.minutos-bloqueo}") int minutosBloqueo) {
        this.usuarios = usuarios;
        this.cuentasCliente = cuentasCliente;
        this.registros = registros;
        this.maxIntentos = maxIntentos;
        this.minutosBloqueo = minutosBloqueo;
    }

    @Transactional
    public void exito(TipoCuenta tipo, String email, String ip) {
        buscar(tipo, email).ifPresent(c -> {
            c.desbloquear();
            c.setUltimoAcceso(LocalDateTime.now());
        });
        registros.save(new RegistroAcceso(tipo.name(), email, true, null, ip));
    }

    /**
     * Registra un intento fallido.
     * @return true si con este intento la cuenta quedó bloqueada.
     */
    @Transactional
    public boolean fallo(TipoCuenta tipo, String email, String motivo, String ip) {
        boolean seBloqueo = false;
        if ("CREDENCIALES".equals(motivo)) {
            Optional<? extends CuentaConBloqueo> cuenta = buscar(tipo, email);
            if (cuenta.isPresent()) {
                CuentaConBloqueo c = cuenta.get();
                int intentos = c.getIntentosFallidos() + 1;
                if (intentos >= maxIntentos) {
                    c.setIntentosFallidos(0);
                    c.setBloqueadoHasta(LocalDateTime.now().plusMinutes(minutosBloqueo));
                    seBloqueo = true;
                    motivo = "SE_BLOQUEO";
                } else {
                    c.setIntentosFallidos(intentos);
                }
            }
        }
        registros.save(new RegistroAcceso(tipo.name(), email.isEmpty() ? "(vacío)" : email, false, motivo, ip));
        return seBloqueo;
    }

    public int getMinutosBloqueo() {
        return minutosBloqueo;
    }

    private Optional<? extends CuentaConBloqueo> buscar(TipoCuenta tipo, String email) {
        return tipo == TipoCuenta.PERSONAL
                ? usuarios.findByEmailIgnoreCase(email)
                : cuentasCliente.findByEmailIgnoreCase(email);
    }
}
