package pe.tisolutions;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;

/**
 * Se excluye el usuario de demostración que Spring crea por defecto ("generated security password"):
 * el acceso usa solo las tablas usuario y cuenta_cliente (ver seguridad.ConfiguracionSeguridad).
 */
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
public class TiSolutionsApplication {

    public static void main(String[] args) {
        SpringApplication.run(TiSolutionsApplication.class, args);
    }
}
