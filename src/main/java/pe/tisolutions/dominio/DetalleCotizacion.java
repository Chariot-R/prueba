package pe.tisolutions.dominio;

import jakarta.persistence.*;
import java.math.BigDecimal;

/**
 * DETALLE. Cada línea guarda una COPIA de la descripción y del costo del proveedor
 * al momento de cotizar: si luego cambia producto.precio, esta línea no cambia.
 */
@Entity
@Table(name = "detalle_cotizacion")
public class DetalleCotizacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cotizacion_id", nullable = false, updatable = false)
    private Cotizacion cotizacion;

    @Column(name = "nro_linea", nullable = false)
    private Integer nroLinea;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_item", nullable = false, length = 10)
    private TipoItem tipoItem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "producto_id")
    private Producto producto;

    @Column(nullable = false, length = 200)
    private String descripcion;

    @Column(nullable = false, length = 10)
    private String unidad;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal cantidad;

    @Column(name = "costo_unitario", precision = 12, scale = 2)
    private BigDecimal costoUnitario;

    @Column(name = "margen_pct", precision = 5, scale = 2)
    private BigDecimal margenPct;

    @Column(name = "precio_unitario", nullable = false, precision = 12, scale = 2)
    private BigDecimal precioUnitario;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal importe;

    /** Copia de otra línea (para nuevas versiones): conserva los precios congelados. */
    public DetalleCotizacion copiar() {
        DetalleCotizacion d = new DetalleCotizacion();
        d.nroLinea = nroLinea;
        d.tipoItem = tipoItem;
        d.producto = producto;
        d.descripcion = descripcion;
        d.unidad = unidad;
        d.cantidad = cantidad;
        d.costoUnitario = costoUnitario;
        d.margenPct = margenPct;
        d.precioUnitario = precioUnitario;
        d.importe = importe;
        return d;
    }

    public Integer getId() { return id; }
    public Cotizacion getCotizacion() { return cotizacion; }
    void setCotizacion(Cotizacion cotizacion) { this.cotizacion = cotizacion; }
    public Integer getNroLinea() { return nroLinea; }
    public void setNroLinea(Integer nroLinea) { this.nroLinea = nroLinea; }
    public TipoItem getTipoItem() { return tipoItem; }
    public void setTipoItem(TipoItem tipoItem) { this.tipoItem = tipoItem; }
    public Producto getProducto() { return producto; }
    public void setProducto(Producto producto) { this.producto = producto; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public String getUnidad() { return unidad; }
    public void setUnidad(String unidad) { this.unidad = unidad; }
    public BigDecimal getCantidad() { return cantidad; }
    public void setCantidad(BigDecimal cantidad) { this.cantidad = cantidad; }
    public BigDecimal getCostoUnitario() { return costoUnitario; }
    public void setCostoUnitario(BigDecimal costoUnitario) { this.costoUnitario = costoUnitario; }
    public BigDecimal getMargenPct() { return margenPct; }
    public void setMargenPct(BigDecimal margenPct) { this.margenPct = margenPct; }
    public BigDecimal getPrecioUnitario() { return precioUnitario; }
    public void setPrecioUnitario(BigDecimal precioUnitario) { this.precioUnitario = precioUnitario; }
    public BigDecimal getImporte() { return importe; }
    public void setImporte(BigDecimal importe) { this.importe = importe; }
}
