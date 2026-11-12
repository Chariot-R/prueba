package pe.tisolutions.dominio;

import jakarta.persistence.*;

@Entity
@Table(name = "cliente")
public class Cliente {

    public static final String NATURAL = "N";
    public static final String JURIDICA = "J";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "tipo_cliente", nullable = false, length = 1)
    private String tipoCliente;

    @Column(name = "nombre_razon_social", nullable = false, length = 150)
    private String nombreRazonSocial;

    @Column(nullable = false, length = 11)
    private String documento;

    private String telefono;
    private String email;

    @Column(name = "medio_contacto", length = 20)
    private String medioContacto;

    @Column(nullable = false, length = 10)
    private String estado;

    protected Cliente() {
    }

    /** Cliente que se registra solo desde la web. */
    public static Cliente registradoPorWeb(String tipoCliente, String nombreRazonSocial, String documento,
                                           String telefono, String email) {
        Cliente c = new Cliente();
        c.tipoCliente = tipoCliente;
        c.nombreRazonSocial = nombreRazonSocial;
        c.documento = documento;
        c.telefono = telefono;
        c.email = email;
        c.medioContacto = "WEB";
        c.estado = "ACTIVO";
        return c;
    }

    public boolean isActivo() {
        return "ACTIVO".equals(estado);
    }

    public boolean isEmpresa() {
        return JURIDICA.equals(tipoCliente);
    }

    public Integer getId() { return id; }
    public String getTipoCliente() { return tipoCliente; }
    public String getNombreRazonSocial() { return nombreRazonSocial; }
    public String getDocumento() { return documento; }
    public String getTelefono() { return telefono; }
    public String getEmail() { return email; }
    public String getMedioContacto() { return medioContacto; }
    public String getEstado() { return estado; }
}
