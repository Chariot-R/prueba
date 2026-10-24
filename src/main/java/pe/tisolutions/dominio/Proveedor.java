package pe.tisolutions.dominio;

import jakarta.persistence.*;

@Entity
@Table(name = "proveedor")
public class Proveedor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, length = 100)
    private String nombre;

    private String contacto;

    public Integer getId() { return id; }
    public String getNombre() { return nombre; }
    public String getContacto() { return contacto; }
}
