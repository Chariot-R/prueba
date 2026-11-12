package pe.tisolutions.seguridad;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import pe.tisolutions.dominio.Rol;
import pe.tisolutions.dominio.Usuario;
import pe.tisolutions.repositorio.RolRepository;
import pe.tisolutions.repositorio.UsuarioRepository;

/**
 * Crea el PRIMER administrador al arrancar, usando las variables de entorno
 * ADMIN_EMAIL y ADMIN_PASSWORD (nunca escritas en el código).
 * Esa contraseña es temporal: al entrar por primera vez se pide cambiarla.
 */
@Component
public class InicializadorAdmin implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(InicializadorAdmin.class);
    /** Hash de relleno que trae 02_datos_prueba.sql (no corresponde a ninguna contraseña). */
    private static final String HASH_DE_PRUEBA = "$2a$10$reemplazarPorHashReal";

    private final UsuarioRepository usuarios;
    private final RolRepository roles;
    private final PasswordEncoder encoder;
    private final String email;
    private final String password;

    public InicializadorAdmin(UsuarioRepository usuarios, RolRepository roles, PasswordEncoder encoder,
                              @Value("${app.admin.email:}") String email,
                              @Value("${app.admin.password:}") String password) {
        this.usuarios = usuarios;
        this.roles = roles;
        this.encoder = encoder;
        this.email = Correo.normalizar(email);
        this.password = password;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (email.isEmpty() || password == null || password.isEmpty()) {
            log.info("ADMIN_EMAIL / ADMIN_PASSWORD no definidas: no se crea administrador inicial.");
            return;
        }
        if (password.length() < PoliticaPassword.MINIMO_PERSONAL) {
            log.warn("ADMIN_PASSWORD debe tener al menos {} caracteres: no se crea administrador inicial.",
                    PoliticaPassword.MINIMO_PERSONAL);
            return;
        }
        var existente = usuarios.findByEmailIgnoreCase(email);
        if (existente.isPresent()) {
            Usuario u = existente.get();
            if (u.getPasswordHash().startsWith(HASH_DE_PRUEBA) && Rol.ADMINISTRADOR.equals(u.getRol().getNombre())) {
                u.asignarPasswordTemporal(encoder.encode(password));
                log.info("Administrador inicial {} activado con la contraseña de ADMIN_PASSWORD.", email);
            }
            return;   // si ya tiene contraseña real, no se toca
        }
        Rol admin = roles.findByNombre(Rol.ADMINISTRADOR)
                .orElseThrow(() -> new IllegalStateException("Falta el rol ADMINISTRADOR en la base de datos"));
        usuarios.save(new Usuario(admin, "Administrador", email, encoder.encode(password)));
        log.info("Administrador inicial {} creado.", email);
    }
}
