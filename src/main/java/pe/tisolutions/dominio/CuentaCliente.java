package pe.tisolutions.dominio;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/** Cuenta web de un CLIENTE (persona natural o empresa). Tabla separada del personal a propósito. */
@Entity
@Table(name = "cuenta_cliente")
public class CuentaCliente implements CuentaConBloqueo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @OneToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "cliente_id", nullable = false, updatable = false)
    private Cliente cliente;

    @Column(nullable = false, length = 120)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "nombre_contacto", nullable = false, length = 100)
    private String nombreContacto;

    @Column(nullable = false, length = 15)
    private String estado = "ACTIVO";

    @Column(name = "intentos_fallidos", nullable = false)
    private int intentosFallidos;

    @Column(name = "bloqueado_hasta")
    private LocalDateTime bloqueadoHasta;

    @Column(name = "acepto_terminos", nullable = false, updatable = false)
    private LocalDateTime aceptoTerminos;

    @Column(name = "fecha_registro", nullable = false, updatable = false)
    private LocalDateTime fechaRegistro;

    @Column(name = "ultimo_acceso")
    private LocalDateTime ultimoAcceso;

    protected CuentaCliente() {
    }

    public CuentaCliente(Cliente cliente, String email, String passwordHash, String nombreContacto) {
        LocalDateTime ahora = LocalDateTime.now();
        this.cliente = cliente;
        this.email = email;
        this.passwordHash = passwordHash;
        this.nombreContacto = nombreContacto;
        this.aceptoTerminos = ahora;
        this.fechaRegistro = ahora;
    }

    public boolean isActiva() {
        return "ACTIVO".equals(estado);
    }

    public Integer getId() { return id; }
    public Cliente getCliente() { return cliente; }
    @Override public String getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }
    public String getNombreContacto() { return nombreContacto; }
    public String getEstado() { return estado; }
    @Override public int getIntentosFallidos() { return intentosFallidos; }
    @Override public void setIntentosFallidos(int intentosFallidos) { this.intentosFallidos = intentosFallidos; }
    @Override public LocalDateTime getBloqueadoHasta() { return bloqueadoHasta; }
    @Override public void setBloqueadoHasta(LocalDateTime bloqueadoHasta) { this.bloqueadoHasta = bloqueadoHasta; }
    public LocalDateTime getFechaRegistro() { return fechaRegistro; }
    public LocalDateTime getUltimoAcceso() { return ultimoAcceso; }
    @Override public void setUltimoAcceso(LocalDateTime ultimoAcceso) { this.ultimoAcceso = ultimoAcceso; }
}
