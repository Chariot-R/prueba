package pe.tisolutions.dto;

import pe.tisolutions.seguridad.CuentaAutenticada;

import java.time.LocalDateTime;

/** Objetos JSON del módulo de acceso (login, sesión, contraseñas, usuarios). */
public final class AccesoDtos {

    private AccesoDtos() {
    }

    public record LoginRequest(String email, String password) {
    }

    /** Quién está en sesión. Nunca incluye el hash de la contraseña. */
    public record SesionDto(String tipo, Integer id, String nombre, String email, String rol,
                            String rolEtiqueta, boolean debeCambiarPassword) {
        public static SesionDto de(CuentaAutenticada c) {
            return new SesionDto(c.getTipo().name(), c.getId(), c.getNombre(), c.getEmail(), c.getRol(),
                    c.getRolEtiqueta(), c.isDebeCambiarPassword());
        }
    }

    public record CambioPasswordRequest(String actual, String nueva, String confirmar) {
    }

    /** Contraseña temporal generada: se muestra una sola vez al administrador. */
    public record ClaveTemporalDto(String email, String temporal) {
    }

    public record CrearUsuarioRequest(String nombre, String email, Integer rolId) {
    }

    public record EditarUsuarioRequest(String nombre, Integer rolId) {
    }

    public record EstadoUsuarioRequest(String estado) {
    }

    public record RolDto(Integer id, String nombre, String etiqueta) {
    }

    public record UsuarioFilaDto(Integer id, String nombre, String email, Integer rolId, String rol, String rolEtiqueta,
                                 String estado, boolean bloqueoTemporal, LocalDateTime bloqueadoHasta,
                                 boolean debeCambiarPassword, LocalDateTime ultimoAcceso, boolean esUsted) {
    }

    public record AccesoDto(LocalDateTime fecha, String tipoCuenta, String email, boolean exito, String motivo, String ip) {
    }

    public record ResumenAdminDto(long personalActivo, long clientesWeb, long cotizaciones, long fallidos24h,
                                  java.util.List<AccesoDto> accesos) {
    }
}
