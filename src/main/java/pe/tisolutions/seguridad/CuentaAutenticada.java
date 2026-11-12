package pe.tisolutions.seguridad;

import org.springframework.security.core.CredentialsContainer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import pe.tisolutions.dominio.CuentaCliente;
import pe.tisolutions.dominio.Rol;
import pe.tisolutions.dominio.Usuario;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * Quién inició sesión. Se guarda en la sesión del servidor (nunca en el navegador).
 * El hash de la contraseña se borra apenas termina el login (eraseCredentials).
 */
public class CuentaAutenticada implements UserDetails, CredentialsContainer {

    public static final String ROL_CLIENTE = "CLIENTE";

    private final TipoCuenta tipo;
    private final Integer id;
    private final Integer clienteId;
    private final String email;
    private final String nombre;
    private final String rol;
    private final String rolEtiqueta;
    private final boolean activa;
    private final boolean noBloqueada;
    private String passwordHash;
    private volatile boolean debeCambiarPassword;

    private CuentaAutenticada(TipoCuenta tipo, Integer id, Integer clienteId, String email, String nombre,
                              String rol, String rolEtiqueta, boolean activa, boolean noBloqueada,
                              String passwordHash, boolean debeCambiarPassword) {
        this.tipo = tipo;
        this.id = id;
        this.clienteId = clienteId;
        this.email = email;
        this.nombre = nombre;
        this.rol = rol;
        this.rolEtiqueta = rolEtiqueta;
        this.activa = activa;
        this.noBloqueada = noBloqueada;
        this.passwordHash = passwordHash;
        this.debeCambiarPassword = debeCambiarPassword;
    }

    static CuentaAutenticada dePersonal(Usuario u) {
        Rol r = u.getRol();
        return new CuentaAutenticada(TipoCuenta.PERSONAL, u.getId(), null, u.getEmail(), u.getNombre(),
                r.getNombre(), r.getEtiqueta(), u.isActivo(), !u.estaBloqueadaTemporalmente(LocalDateTime.now()),
                u.getPasswordHash(), u.isDebeCambiarPassword());
    }

    static CuentaAutenticada deCliente(CuentaCliente c) {
        boolean activa = c.isActiva() && c.getCliente().isActivo();
        String etiqueta = c.getCliente().isEmpresa() ? "Empresa" : "Persona natural";
        return new CuentaAutenticada(TipoCuenta.CLIENTE, c.getId(), c.getCliente().getId(), c.getEmail(),
                c.getNombreContacto(), ROL_CLIENTE, etiqueta, activa,
                !c.estaBloqueadaTemporalmente(LocalDateTime.now()), c.getPasswordHash(), false);
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + rol));
    }

    @Override public String getPassword() { return passwordHash; }
    @Override public String getUsername() { return email; }
    @Override public boolean isEnabled() { return activa; }
    @Override public boolean isAccountNonLocked() { return noBloqueada; }
    @Override public void eraseCredentials() { this.passwordHash = null; }

    public TipoCuenta getTipo() { return tipo; }
    public Integer getId() { return id; }
    /** Solo para clientes: id del registro en la tabla cliente. */
    public Integer getClienteId() { return clienteId; }
    public String getEmail() { return email; }
    public String getNombre() { return nombre; }
    public String getInicial() { return nombre == null || nombre.isBlank() ? "?" : nombre.substring(0, 1).toUpperCase(); }
    public String getRol() { return rol; }
    public String getRolEtiqueta() { return rolEtiqueta; }
    public boolean isDebeCambiarPassword() { return debeCambiarPassword; }
    void passwordCambiada() { this.debeCambiarPassword = false; }

    @Override
    public boolean equals(Object o) {
        return o instanceof CuentaAutenticada otra && tipo == otra.tipo && Objects.equals(id, otra.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(tipo, id);
    }

    @Override
    public String toString() {
        return tipo + ":" + id;   // nunca imprimir el hash
    }
}
