package pe.tisolutions.dto;

import jakarta.validation.constraints.*;

/** Datos del formulario "Registrarse" (persona natural o empresa). */
public class RegistroClienteForm {

    @NotBlank(message = "Elige si eres persona natural o empresa.")
    @Pattern(regexp = "[NJ]", message = "Tipo de cliente no válido.")
    private String tipo = "N";

    // Persona natural
    private String nombres;
    private String apellidos;

    // Empresa
    private String razonSocial;
    private String nombreContacto;

    @NotBlank(message = "Ingresa tu DNI o RUC.")
    private String documento;

    @NotBlank(message = "Ingresa tu correo.")
    @Email(message = "Ingresa un correo válido.")
    @Size(max = 120, message = "El correo es demasiado largo.")
    private String email;

    @NotBlank(message = "Ingresa tu celular.")
    @Pattern(regexp = "9\\d{8}", message = "Ingresa un celular de 9 dígitos que empiece con 9.")
    private String telefono;

    @NotBlank(message = "Ingresa una contraseña.")
    private String password;

    @NotBlank(message = "Confirma tu contraseña.")
    private String confirmar;

    @AssertTrue(message = "Debes aceptar los términos y condiciones.")
    private boolean terminos;

    /** Campo trampa invisible: las personas no lo ven; si viene lleno, lo envió un robot. */
    private String sitioWeb;

    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }
    public String getNombres() { return nombres; }
    public void setNombres(String nombres) { this.nombres = nombres; }
    public String getApellidos() { return apellidos; }
    public void setApellidos(String apellidos) { this.apellidos = apellidos; }
    public String getRazonSocial() { return razonSocial; }
    public void setRazonSocial(String razonSocial) { this.razonSocial = razonSocial; }
    public String getNombreContacto() { return nombreContacto; }
    public void setNombreContacto(String nombreContacto) { this.nombreContacto = nombreContacto; }
    public String getDocumento() { return documento; }
    public void setDocumento(String documento) { this.documento = documento; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getConfirmar() { return confirmar; }
    public void setConfirmar(String confirmar) { this.confirmar = confirmar; }
    public boolean isTerminos() { return terminos; }
    public void setTerminos(boolean terminos) { this.terminos = terminos; }
    public String getSitioWeb() { return sitioWeb; }
    public void setSitioWeb(String sitioWeb) { this.sitioWeb = sitioWeb; }

    /** Nunca se devuelve la contraseña al navegador si el formulario tiene errores. */
    public void limpiarPasswords() {
        this.password = null;
        this.confirmar = null;
    }
}
