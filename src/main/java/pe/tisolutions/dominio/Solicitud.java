package pe.tisolutions.dominio;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "solicitud")
public class Solicitud {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    @Column(nullable = false, length = 20)
    private String canal;

    @Column(length = 300)
    private String descripcion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private EstadoSolicitud estado;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion;

    protected Solicitud() {
    }

    /** Solicitud de servicio que el cliente registra desde su cuenta web. */
    public static Solicitud desdeWeb(Cliente cliente, String descripcion) {
        Solicitud s = new Solicitud();
        s.cliente = cliente;
        s.canal = "WEB";
        s.descripcion = descripcion;
        s.estado = EstadoSolicitud.NUEVA;
        s.fechaCreacion = LocalDateTime.now();
        return s;
    }

    public Integer getId() { return id; }
    public Cliente getCliente() { return cliente; }
    public String getCanal() { return canal; }
    public String getDescripcion() { return descripcion; }
    public EstadoSolicitud getEstado() { return estado; }
    public void setEstado(EstadoSolicitud estado) { this.estado = estado; }
    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
}
