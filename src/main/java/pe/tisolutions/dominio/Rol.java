package pe.tisolutions.dominio;

import jakarta.persistence.*;

/** Rol del personal: ADMINISTRADOR, VENTAS o TECNICO. */
@Entity
@Table(name = "rol")
public class Rol {

    public static final String ADMINISTRADOR = "ADMINISTRADOR";
    public static final String VENTAS = "VENTAS";
    public static final String TECNICO = "TECNICO";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, length = 50)
    private String nombre;

    protected Rol() {
    }

    public Integer getId() { return id; }
    public String getNombre() { return nombre; }

    /** Nombre para mostrar: "Administrador", "Ventas", "Técnico". */
    public String getEtiqueta() {
        return switch (nombre) {
            case ADMINISTRADOR -> "Administrador";
            case VENTAS -> "Ventas";
            case TECNICO -> "Técnico";
            default -> nombre;
        };
    }
}
