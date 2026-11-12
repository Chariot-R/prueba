package pe.tisolutions.seguridad;

/** Las dos "puertas" del sistema. Cada una tiene su tabla, su página de login y su sesión. */
public enum TipoCuenta {
    /** Administrador, Ventas, Técnico: tabla usuario, entra por /intranet/login. */
    PERSONAL,
    /** Persona natural o empresa: tabla cuenta_cliente, entra por /login. */
    CLIENTE
}
