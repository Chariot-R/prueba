package pe.tisolutions.dominio;

import jakarta.persistence.*;

@Entity
@Table(name = "cliente")
public class Cliente {

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

    @Column(nullable = false, length = 10)
    private String estado;

    public boolean isActivo() {
        return "ACTIVO".equals(estado);
    }

    public Integer getId() { return id; }
    public String getTipoCliente() { return tipoCliente; }
    public String getNombreRazonSocial() { return nombreRazonSocial; }
    public String getDocumento() { return documento; }
    public String getTelefono() { return telefono; }
    public String getEmail() { return email; }
    public String getEstado() { return estado; }
}
