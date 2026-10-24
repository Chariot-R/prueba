package pe.tisolutions.dominio;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** MAESTRO. Agrega y controla sus líneas de detalle (se guardan y borran junto con ella). */
@Entity
@Table(name = "cotizacion")
public class Cotizacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, length = 12, updatable = false)
    private String codigo;

    /** Número de versión del documento (1, 2, 3...). No confundir con filaVersion. */
    @Column(name = "version", nullable = false, updatable = false)
    private Integer numeroVersion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cotizacion_origen_id", updatable = false)
    private Cotizacion cotizacionOrigen;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "solicitud_id")
    private Solicitud solicitud;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false, updatable = false)
    private Usuario usuario;

    @Column(nullable = false, updatable = false)
    private LocalDateTime fecha;

    @Column(name = "fecha_emision")
    private LocalDateTime fechaEmision;

    @Column(name = "dias_validez", nullable = false)
    private Integer diasValidez;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private EstadoCotizacion estado;

    @Column(name = "margen_aplicado", nullable = false, precision = 5, scale = 2)
    private BigDecimal margenAplicado;

    @Column(name = "tasa_igv", nullable = false, precision = 5, scale = 4, updatable = false)
    private BigDecimal tasaIgv;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotal = BigDecimal.ZERO;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal igv = BigDecimal.ZERO;

    @Column(name = "monto_total", nullable = false, precision = 12, scale = 2)
    private BigDecimal montoTotal = BigDecimal.ZERO;

    @Column(length = 500)
    private String observaciones;

    @Column(name = "motivo_no_cierre", length = 300)
    private String motivoNoCierre;

    @Column(name = "motivo_anulacion", length = 300)
    private String motivoAnulacion;

    @Column(name = "fecha_anulacion")
    private LocalDateTime fechaAnulacion;

    /** Bloqueo optimista: si otro usuario guardó antes, el UPDATE no encuentra la fila y se lanza un conflicto. */
    @Version
    @Column(name = "fila_version", nullable = false)
    private Integer filaVersion;

    @OneToMany(mappedBy = "cotizacion", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("nroLinea ASC")
    private List<DetalleCotizacion> detalles = new ArrayList<>();

    @PrePersist
    void antesDeInsertar() {
        if (fecha == null) {
            fecha = LocalDateTime.now();
        }
    }

    public void agregarDetalle(DetalleCotizacion detalle) {
        detalle.setCotizacion(this);
        detalles.add(detalle);
    }

    public String getCodigoCompleto() {
        return numeroVersion != null && numeroVersion > 1 ? codigo + " v" + numeroVersion : codigo;
    }

    public LocalDateTime getFechaVencimiento() {
        return fechaEmision == null ? null : fechaEmision.plusDays(diasValidez);
    }

    // --- getters / setters ---
    public Integer getId() { return id; }
    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }
    public Integer getNumeroVersion() { return numeroVersion; }
    public void setNumeroVersion(Integer numeroVersion) { this.numeroVersion = numeroVersion; }
    public Cotizacion getCotizacionOrigen() { return cotizacionOrigen; }
    public void setCotizacionOrigen(Cotizacion cotizacionOrigen) { this.cotizacionOrigen = cotizacionOrigen; }
    public Cliente getCliente() { return cliente; }
    public void setCliente(Cliente cliente) { this.cliente = cliente; }
    public Solicitud getSolicitud() { return solicitud; }
    public void setSolicitud(Solicitud solicitud) { this.solicitud = solicitud; }
    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }
    public LocalDateTime getFecha() { return fecha; }
    public LocalDateTime getFechaEmision() { return fechaEmision; }
    public void setFechaEmision(LocalDateTime fechaEmision) { this.fechaEmision = fechaEmision; }
    public Integer getDiasValidez() { return diasValidez; }
    public void setDiasValidez(Integer diasValidez) { this.diasValidez = diasValidez; }
    public EstadoCotizacion getEstado() { return estado; }
    public void setEstado(EstadoCotizacion estado) { this.estado = estado; }
    public BigDecimal getMargenAplicado() { return margenAplicado; }
    public void setMargenAplicado(BigDecimal margenAplicado) { this.margenAplicado = margenAplicado; }
    public BigDecimal getTasaIgv() { return tasaIgv; }
    public void setTasaIgv(BigDecimal tasaIgv) { this.tasaIgv = tasaIgv; }
    public BigDecimal getSubtotal() { return subtotal; }
    public void setSubtotal(BigDecimal subtotal) { this.subtotal = subtotal; }
    public BigDecimal getIgv() { return igv; }
    public void setIgv(BigDecimal igv) { this.igv = igv; }
    public BigDecimal getMontoTotal() { return montoTotal; }
    public void setMontoTotal(BigDecimal montoTotal) { this.montoTotal = montoTotal; }
    public String getObservaciones() { return observaciones; }
    public void setObservaciones(String observaciones) { this.observaciones = observaciones; }
    public String getMotivoNoCierre() { return motivoNoCierre; }
    public void setMotivoNoCierre(String motivoNoCierre) { this.motivoNoCierre = motivoNoCierre; }
    public String getMotivoAnulacion() { return motivoAnulacion; }
    public void setMotivoAnulacion(String motivoAnulacion) { this.motivoAnulacion = motivoAnulacion; }
    public LocalDateTime getFechaAnulacion() { return fechaAnulacion; }
    public void setFechaAnulacion(LocalDateTime fechaAnulacion) { this.fechaAnulacion = fechaAnulacion; }
    public Integer getFilaVersion() { return filaVersion; }
    public List<DetalleCotizacion> getDetalles() { return detalles; }
}
