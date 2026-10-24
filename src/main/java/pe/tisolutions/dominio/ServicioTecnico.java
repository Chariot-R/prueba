package pe.tisolutions.dominio;

import jakarta.persistence.*;

/** Documento relacionado: mientras exista un servicio no anulado, la cotización no se puede borrar ni anular. */
@Entity
@Table(name = "servicio_tecnico")
public class ServicioTecnico {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cotizacion_id", nullable = false)
    private Cotizacion cotizacion;

    @Column(nullable = false, length = 15)
    private String estado;

    public Integer getId() { return id; }
    public Cotizacion getCotizacion() { return cotizacion; }
    public String getEstado() { return estado; }
}
