package pe.tisolutions.seguridad;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.security.web.authentication.logout.HttpStatusReturningLogoutSuccessHandler;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy;
import org.springframework.security.web.savedrequest.NullRequestCache;
import pe.tisolutions.dominio.Rol;
import pe.tisolutions.repositorio.CuentaClienteRepository;
import pe.tisolutions.repositorio.UsuarioRepository;

/**
 * Seguridad de la API REST que consume el frontend Angular.
 *
 * Hay dos "puertas" independientes:
 *  1) /api/intranet/**  -> personal (Administrador, Ventas, Técnico). Valida contra la tabla usuario.
 *  2) /api/portal/**    -> clientes (persona natural o empresa). Valida contra la tabla cuenta_cliente.
 *
 * Cada puerta guarda su sesión bajo una clave distinta: una sesión de cliente "no existe" para la
 * intranet, así que aunque alguien robe una cuenta de cliente no puede ver nada interno.
 *
 * La sesión vive en el servidor; el navegador solo tiene una cookie HttpOnly (JavaScript no puede
 * leerla). Contra CSRF se usa la cookie XSRF-TOKEN, que Angular reenvía sola en la cabecera X-XSRF-TOKEN.
 */
@Configuration
@EnableWebSecurity
public class ConfiguracionSeguridad {

    static final String CLAVE_SESION_PERSONAL = "TIS_SESION_PERSONAL";
    static final String CLAVE_SESION_CLIENTE = "TIS_SESION_CLIENTE";

    /** BCrypt con costo 12: adivinar una contraseña robada es lento a propósito. */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public CookieCsrfTokenRepository csrfRepositorio() {
        CookieCsrfTokenRepository repo = CookieCsrfTokenRepository.withHttpOnlyFalse();
        repo.setCookieCustomizer(c -> c.sameSite("Lax"));
        return repo;
    }

    @Bean
    public Puerta puertaPersonal(UsuarioRepository usuarios, PasswordEncoder encoder) {
        return new Puerta(TipoCuenta.PERSONAL, gestor(CuentasDetalles.personal(usuarios), encoder),
                repositorioSesion(CLAVE_SESION_PERSONAL));
    }

    @Bean
    public Puerta puertaCliente(CuentaClienteRepository cuentas, PasswordEncoder encoder) {
        return new Puerta(TipoCuenta.CLIENTE, gestor(CuentasDetalles.clientes(cuentas), encoder),
                repositorioSesion(CLAVE_SESION_CLIENTE));
    }

    @Bean
    @Order(1)
    public SecurityFilterChain intranet(HttpSecurity http, @Qualifier("puertaPersonal") Puerta puerta,
                                        CookieCsrfTokenRepository csrf) throws Exception {
        comun(http, puerta, csrf, "/api/intranet/auth/logout");
        http.securityMatcher("/api/intranet/**")
            .authorizeHttpRequests(a -> a
                .requestMatchers("/api/intranet/auth/login", "/api/intranet/auth/yo").permitAll()
                .requestMatchers("/api/intranet/admin/**").hasRole(Rol.ADMINISTRADOR)
                .requestMatchers("/api/intranet/tecnico/**").hasRole(Rol.TECNICO)
                .requestMatchers("/api/intranet/cotizaciones/**", "/api/intranet/catalogo/**")
                    .hasAnyRole(Rol.VENTAS, Rol.ADMINISTRADOR)
                .anyRequest().authenticated())
            .addFilterAfter(new FiltroCambioPassword(), AuthorizationFilter.class);
        return http.build();
    }

    @Bean
    @Order(2)
    public SecurityFilterChain portal(HttpSecurity http, @Qualifier("puertaCliente") Puerta puerta,
                                      CookieCsrfTokenRepository csrf) throws Exception {
        comun(http, puerta, csrf, "/api/portal/auth/logout");
        http.securityMatcher("/api/portal/**")
            .authorizeHttpRequests(a -> a
                .requestMatchers("/api/portal/auth/login", "/api/portal/auth/yo", "/api/portal/auth/registro").permitAll()
                .anyRequest().hasRole(CuentaAutenticada.ROL_CLIENTE));
        return http.build();
    }

    /** Todo lo demás: ninguna otra ruta de la API existe. */
    @Bean
    @Order(3)
    public SecurityFilterChain resto(HttpSecurity http, CookieCsrfTokenRepository csrf) throws Exception {
        http.csrf(c -> c.csrfTokenRepository(csrf).csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler()))
            .authorizeHttpRequests(a -> a
                .requestMatchers("/error").permitAll()
                .requestMatchers("/api/**").denyAll()
                .anyRequest().permitAll())
            .exceptionHandling(e -> e
                .authenticationEntryPoint(RespuestasJson::noAutenticado)
                .accessDeniedHandler(RespuestasJson::denegado));
        return http.build();
    }

    private static void comun(HttpSecurity http, Puerta puerta, CookieCsrfTokenRepository csrf,
                              String rutaLogout) throws Exception {
        http.securityContext(c -> c.securityContextRepository(puerta.repositorio()))
            .csrf(c -> c.csrfTokenRepository(csrf).csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler()))
            .addFilterAfter(new FiltroCookieCsrf(), BasicAuthenticationFilter.class)
            .requestCache(c -> c.requestCache(new NullRequestCache()))
            .logout(l -> l
                .logoutUrl(rutaLogout)
                // Solo se borra la sesión de ESTA puerta; la otra (si existe) sigue igual
                .invalidateHttpSession(false)
                .logoutSuccessHandler(new HttpStatusReturningLogoutSuccessHandler(HttpStatus.NO_CONTENT)))
            .exceptionHandling(e -> e
                .authenticationEntryPoint(RespuestasJson::noAutenticado)
                .accessDeniedHandler(RespuestasJson::denegado))
            .headers(h -> h.referrerPolicy(r -> r.policy(ReferrerPolicy.SAME_ORIGIN)));
    }

    private static AuthenticationManager gestor(UserDetailsService detalles, PasswordEncoder encoder) {
        DaoAuthenticationProvider proveedor = new DaoAuthenticationProvider();
        proveedor.setUserDetailsService(detalles);
        proveedor.setPasswordEncoder(encoder);
        return new ProviderManager(proveedor);
    }

    private static HttpSessionSecurityContextRepository repositorioSesion(String clave) {
        HttpSessionSecurityContextRepository repo = new HttpSessionSecurityContextRepository();
        repo.setSpringSecurityContextKey(clave);
        return repo;
    }
}
