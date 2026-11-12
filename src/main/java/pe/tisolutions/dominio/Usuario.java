package pe.tisolutions.dominio;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/** Cuenta del PERSONAL (Administrador, Ventas, Técnico). Las crea el administrador; nadie se registra solo. */
@Entity
@Table(name = "usuario")
public class Usuario implements CuentaConBloqueo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "rol_id", nullable = false)
    private Rol rol;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(nullable = false, length = 120)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(nullable = false, length = 15)
    private String estado = "ACTIVO";

    @Column(name = "intentos_fallidos", nullable = false)
    private int intentosFallidos;

    @Column(name = "bloqueado_hasta")
    private LocalDateTime bloqueadoHasta;

    @Column(name = "debe_cambiar_password", nullable = false)
    private boolean debeCambiarPassword = true;

    @Column(name = "ultimo_acceso")
    private LocalDateTime ultimoAcceso;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion = LocalDateTime.now();

    protected Usuario() {
    }

    /** Usuario nuevo con contraseña temporal: deberá cambiarla en su primer ingreso. */
    public Usuario(Rol rol, String nombre, String email, String passwordHash) {
        this.rol = rol;
        this.nombre = nombre;
        this.email = email;
        this.passwordHash = passwordHash;
    }

    public boolean isActivo() {
        return "ACTIVO".equals(estado);
    }

    /** Contraseña temporal puesta por el administrador (alta o restablecimiento). */
    public void asignarPasswordTemporal(String hash) {
        this.passwordHash = hash;
        this.debeCambiarPassword = true;
        desbloquear();
    }

    /** Contraseña elegida por el propio usuario. */
    public void cambiarPassword(String hash) {
        this.passwordHash = hash;
        this.debeCambiarPassword = false;
    }

    public Integer getId() { return id; }
    public Rol getRol() { return rol; }
    public void setRol(Rol rol) { this.rol = rol; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    @Override public String getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    @Override public int getIntentosFallidos() { return intentosFallidos; }
    @Override public void setIntentosFallidos(int intentosFallidos) { this.intentosFallidos = intentosFallidos; }
    @Override public LocalDateTime getBloqueadoHasta() { return bloqueadoHasta; }
    @Override public void setBloqueadoHasta(LocalDateTime bloqueadoHasta) { this.bloqueadoHasta = bloqueadoHasta; }
    public boolean isDebeCambiarPassword() { return debeCambiarPassword; }
    public LocalDateTime getUltimoAcceso() { return ultimoAcceso; }
    @Override public void setUltimoAcceso(LocalDateTime ultimoAcceso) { this.ultimoAcceso = ultimoAcceso; }
    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
}
