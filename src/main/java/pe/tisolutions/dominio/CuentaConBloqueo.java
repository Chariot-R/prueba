package pe.tisolutions.dominio;

import java.time.LocalDateTime;

/** Lo común a las cuentas del personal y de clientes para el bloqueo por intentos fallidos. */
public interface CuentaConBloqueo {

    String getEmail();
    int getIntentosFallidos();
    void setIntentosFallidos(int intentos);
    LocalDateTime getBloqueadoHasta();
    void setBloqueadoHasta(LocalDateTime hasta);
    void setUltimoAcceso(LocalDateTime fecha);

    default boolean estaBloqueadaTemporalmente(LocalDateTime ahora) {
        return getBloqueadoHasta() != null && getBloqueadoHasta().isAfter(ahora);
    }

    default void desbloquear() {
        setIntentosFallidos(0);
        setBloqueadoHasta(null);
    }
}
