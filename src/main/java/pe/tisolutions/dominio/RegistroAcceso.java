package pe.tisolutions.dominio;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/** Bitácora de intentos de inicio de sesión (exitosos y fallidos), para auditoría. */
@Entity
@Table(name = "registro_acceso")
public class RegistroAcceso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tipo_cuenta", nullable = false, length = 10)
    private String tipoCuenta;

    @Column(nullable = false, length = 120)
    private String email;

    @Column(nullable = false)
    private boolean exito;

    @Column(length = 40)
    private String motivo;

    @Column(length = 45)
    private String ip;

    @Column(nullable = false)
    private LocalDateTime fecha;

    protected RegistroAcceso() {
    }

    public RegistroAcceso(String tipoCuenta, String email, boolean exito, String motivo, String ip) {
        this.tipoCuenta = tipoCuenta;
        this.email = email.length() > 120 ? email.substring(0, 120) : email;
        this.exito = exito;
        this.motivo = motivo;
        this.ip = ip;
        this.fecha = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public String getTipoCuenta() { return tipoCuenta; }
    public String getEmail() { return email; }
    public boolean isExito() { return exito; }
    public String getMotivo() { return motivo; }
    public String getIp() { return ip; }
    public LocalDateTime getFecha() { return fecha; }
}
