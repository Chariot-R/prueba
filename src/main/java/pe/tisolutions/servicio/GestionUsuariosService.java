package pe.tisolutions.servicio;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.tisolutions.dominio.Rol;
import pe.tisolutions.dominio.Usuario;
import pe.tisolutions.excepcion.RecursoNoEncontradoException;
import pe.tisolutions.excepcion.ReglaNegocioException;
import pe.tisolutions.repositorio.RolRepository;
import pe.tisolutions.repositorio.UsuarioRepository;
import pe.tisolutions.seguridad.Correo;
import pe.tisolutions.seguridad.PoliticaPassword;

import java.util.Set;

/** Cuentas del personal: solo el administrador las crea, bloquea o restablece. */
@Service
public class GestionUsuariosService {

    private static final Set<String> ESTADOS = Set.of("ACTIVO", "BLOQUEADO", "BAJA");

    private final UsuarioRepository usuarios;
    private final RolRepository roles;
    private final PasswordEncoder encoder;

    public GestionUsuariosService(UsuarioRepository usuarios, RolRepository roles, PasswordEncoder encoder) {
        this.usuarios = usuarios;
        this.roles = roles;
        this.encoder = encoder;
    }

    /** Crea la cuenta y devuelve la contraseña temporal (se muestra UNA sola vez al administrador). */
    @Transactional
    public String crear(String nombre, String email, Integer rolId) {
        String n = nombre == null ? "" : nombre.trim().replaceAll("\\s+", " ");
        String e = Correo.normalizar(email);
        if (n.isEmpty() || n.length() > 100) throw new ReglaNegocioException("Ingresa un nombre (máximo 100 caracteres).");
        if (!e.matches("[^@\\s]+@[^@\\s]+\\.[^@\\s]+")) throw new ReglaNegocioException("Ingresa un correo válido.");
        if (usuarios.existsByEmailIgnoreCase(e)) throw new ReglaNegocioException("Ya existe un trabajador con ese correo.");
        Rol rol = roles.findById(rolId == null ? -1 : rolId)
                .orElseThrow(() -> new ReglaNegocioException("Elige un rol."));
        String temporal = PoliticaPassword.generarTemporal();
        usuarios.save(new Usuario(rol, n, e, encoder.encode(temporal)));
        return temporal;
    }

    /** Cambia nombre y rol. El administrador no puede quitarse a sí mismo el rol de administrador. */
    @Transactional
    public void editar(Integer id, String nombre, Integer rolId, Integer idAdminActual) {
        String n = nombre == null ? "" : nombre.trim().replaceAll("\\s+", " ");
        if (n.isEmpty() || n.length() > 100) throw new ReglaNegocioException("Ingresa un nombre (máximo 100 caracteres).");
        Usuario u = buscar(id);
        Rol rol = roles.findById(rolId == null ? -1 : rolId)
                .orElseThrow(() -> new ReglaNegocioException("Elige un rol."));
        if (id.equals(idAdminActual) && !Rol.ADMINISTRADOR.equals(rol.getNombre())) {
            throw new ReglaNegocioException("No puedes quitarte el rol de administrador a ti mismo.");
        }
        u.setNombre(n);
        u.setRol(rol);
    }

    /** Pone una contraseña temporal nueva y quita el bloqueo por intentos. Devuelve la temporal. */
    @Transactional
    public String restablecerPassword(Integer id) {
        Usuario u = buscar(id);
        String temporal = PoliticaPassword.generarTemporal();
        u.asignarPasswordTemporal(encoder.encode(temporal));
        return temporal;
    }

    @Transactional
    public void cambiarEstado(Integer id, String estado, Integer idAdminActual) {
        if (!ESTADOS.contains(estado)) throw new ReglaNegocioException("Estado no válido.");
        if (id.equals(idAdminActual)) throw new ReglaNegocioException("No puedes cambiar el estado de tu propia cuenta.");
        Usuario u = buscar(id);
        u.setEstado(estado);
        if ("ACTIVO".equals(estado)) u.desbloquear();
    }

    /** El propio trabajador cambia su contraseña. Devuelve el problema o null si se cambió. */
    @Transactional
    public String cambiarPasswordPropia(Integer id, String actual, String nueva, String confirmar) {
        Usuario u = buscar(id);
        if (actual == null || !encoder.matches(actual, u.getPasswordHash())) {
            return "La contraseña actual no es correcta.";
        }
        String problema = PoliticaPassword.problemaPersonal(nueva, u.getEmail());
        if (problema != null) return problema;
        if (!nueva.equals(confirmar)) return "Las contraseñas nuevas no coinciden.";
        if (encoder.matches(nueva, u.getPasswordHash())) return "La nueva contraseña debe ser distinta de la actual.";
        u.cambiarPassword(encoder.encode(nueva));
        return null;
    }

    private Usuario buscar(Integer id) {
        return usuarios.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("El usuario no existe."));
    }
}
