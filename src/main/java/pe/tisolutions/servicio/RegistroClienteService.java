package pe.tisolutions.servicio;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.Errors;
import pe.tisolutions.dominio.Cliente;
import pe.tisolutions.dominio.CuentaCliente;
import pe.tisolutions.dto.RegistroClienteForm;
import pe.tisolutions.repositorio.ClienteRepository;
import pe.tisolutions.repositorio.CuentaClienteRepository;
import pe.tisolutions.seguridad.Correo;
import pe.tisolutions.seguridad.PoliticaPassword;

/** Alta de clientes desde la web: crea el registro en "cliente" y su cuenta de acceso. */
@Service
public class RegistroClienteService {

    private final ClienteRepository clientes;
    private final CuentaClienteRepository cuentas;
    private final PasswordEncoder encoder;

    public RegistroClienteService(ClienteRepository clientes, CuentaClienteRepository cuentas, PasswordEncoder encoder) {
        this.clientes = clientes;
        this.cuentas = cuentas;
        this.encoder = encoder;
    }

    /** Valida lo que depende del tipo de cliente y de la base de datos. Los errores quedan en {@code errores}. */
    public void validar(RegistroClienteForm f, Errors errores) {
        boolean empresa = Cliente.JURIDICA.equals(f.getTipo());
        String documento = texto(f.getDocumento());
        String email = Correo.normalizar(f.getEmail());

        if (empresa) {
            exigir(f.getRazonSocial(), 150, "razonSocial", "Ingresa la razón social.", errores);
            exigir(f.getNombreContacto(), 100, "nombreContacto", "Ingresa el nombre de la persona de contacto.", errores);
            if (!errores.hasFieldErrors("documento") && !ValidadorDocumento.rucValido(documento)) {
                errores.rejectValue("documento", "ruc", "El RUC no es válido. Revisa los 11 dígitos.");
            }
        } else {
            exigir(f.getNombres(), 60, "nombres", "Ingresa tus nombres.", errores);
            exigir(f.getApellidos(), 60, "apellidos", "Ingresa tus apellidos.", errores);
            if (!errores.hasFieldErrors("documento") && !ValidadorDocumento.dniValido(documento)) {
                errores.rejectValue("documento", "dni", "El DNI debe tener 8 dígitos.");
            }
        }

        if (!errores.hasFieldErrors("password")) {
            String problema = PoliticaPassword.problemaCliente(f.getPassword(), email);
            if (problema != null) {
                errores.rejectValue("password", "politica", problema);
            } else if (!f.getPassword().equals(f.getConfirmar())) {
                errores.rejectValue("confirmar", "distinta", "Las contraseñas no coinciden.");
            }
        }

        if (!errores.hasFieldErrors("email") && cuentas.existsByEmailIgnoreCase(email)) {
            errores.rejectValue("email", "existe", "Este correo ya tiene una cuenta. Inicia sesión.");
        }
        if (!errores.hasFieldErrors("documento") && clientes.existsByDocumento(documento)) {
            errores.rejectValue("documento", "existe",
                    "Este documento ya está registrado en TI Solutions. Si ya eres cliente, comunícate con nosotros para activar tu cuenta.");
        }
    }

    /** Guarda cliente y cuenta juntos (o ninguno). Llamar solo si {@link #validar} no encontró errores. */
    @Transactional
    public CuentaCliente registrar(RegistroClienteForm f) {
        boolean empresa = Cliente.JURIDICA.equals(f.getTipo());
        String email = Correo.normalizar(f.getEmail());
        String nombreCliente = empresa ? texto(f.getRazonSocial()) : texto(f.getNombres()) + " " + texto(f.getApellidos());
        String contacto = empresa ? texto(f.getNombreContacto()) : texto(f.getNombres());

        Cliente cliente = clientes.save(Cliente.registradoPorWeb(
                empresa ? Cliente.JURIDICA : Cliente.NATURAL, nombreCliente, texto(f.getDocumento()),
                f.getTelefono(), email));
        return cuentas.save(new CuentaCliente(cliente, email, encoder.encode(f.getPassword()), contacto));
    }

    private static void exigir(String valor, int max, String campo, String mensaje, Errors errores) {
        String v = texto(valor);
        if (v.isEmpty()) {
            errores.rejectValue(campo, "requerido", mensaje);
        } else if (v.length() > max) {
            errores.rejectValue(campo, "largo", "Máximo " + max + " caracteres.");
        }
    }

    /** Quita espacios de más: "  Juan   Pérez " -> "Juan Pérez". */
    private static String texto(String s) {
        return s == null ? "" : s.trim().replaceAll("\\s+", " ");
    }
}
