package pe.tisolutions.excepcion;

/** Error asociado a un campo. campo usa la ruta del JSON, p. ej. "clienteId" o "lineas[2].cantidad". */
public record ErrorCampo(String campo, String mensaje) {
}
