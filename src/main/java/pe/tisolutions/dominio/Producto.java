package pe.tisolutions.dominio;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Catálogo de productos del proveedor. El precio es referencial, con IGV incluido, y puede cambiar. */
@Entity
@Table(name = "producto")
public class Producto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "proveedor_id", nullable = false)
    private Proveedor proveedor;

    @Column(nullable = false, length = 150)
    private String nombre;

    @Column(nullable = false, length = 10)
    private String unidad;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal precio;

    @Column(nullable = false)
    private Integer stock;

    @Column(name = "fecha_actualizacion", nullable = false)
    private LocalDateTime fechaActualizacion;

    @Column(nullable = false)
    private Boolean activo;

    public boolean esUnidadEntera() {
        return "UND".equalsIgnoreCase(unidad);
    }

    public Integer getId() { return id; }
    public Proveedor getProveedor() { return proveedor; }
    public String getNombre() { return nombre; }
    public String getUnidad() { return unidad; }
    public BigDecimal getPrecio() { return precio; }
    public Integer getStock() { return stock; }
    public LocalDateTime getFechaActualizacion() { return fechaActualizacion; }
    public Boolean getActivo() { return activo; }
}
