package pe.tisolutions.web;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

import java.io.IOException;

/**
 * Sirve el frontend Angular ya compilado (carpeta static/, la copia Maven desde frontend/dist).
 * Si la ruta no es un archivo (por ejemplo /intranet/cotizaciones/5), responde index.html y
 * Angular decide qué pantalla mostrar. Las rutas /api/** nunca llegan aquí como archivos.
 *
 * En desarrollo no hace falta: se usa "ng serve" en el puerto 4200.
 */
@Configuration
public class ConfiguracionSpa implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/")
                .resourceChain(true)
                .addResolver(new PathResourceResolver() {
                    @Override
                    protected Resource getResource(String ruta, Resource base) throws IOException {
                        Resource pedido = base.createRelative(ruta);
                        if (pedido.exists() && pedido.isReadable()) {
                            return pedido;
                        }
                        if (ruta.startsWith("api/")) {
                            return null;
                        }
                        Resource indice = new ClassPathResource("/static/index.html");
                        return indice.exists() ? indice : null;
                    }
                });
    }
}
