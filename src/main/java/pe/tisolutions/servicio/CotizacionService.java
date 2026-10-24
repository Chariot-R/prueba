package pe.tisolutions.servicio;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.tisolutions.dominio.*;
import pe.tisolutions.dto.*;
import pe.tisolutions.excepcion.*;
import pe.tisolutions.repositorio.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Capa de negocio del módulo de cotizaciones.
 *
 * Cada método público que escribe es UNA transacción (@Transactional): el maestro y todas sus
 * líneas se guardan juntos o no se guarda nada. Cualquier excepción (de validación, de regla de
 * negocio o de la base de datos) provoca ROLLBACK automático.
 */
@Service
public class CotizacionService {

    private final CotizacionRepository cotizaciones;
    private final ClienteRepository clientes;
    private final SolicitudRepository solicitudes;
    private final ProductoRepository productos;
    private final UsuarioRepository usuarios;
    private final ServicioTecnicoRepository servicios;
    private final EntityManager em;
    private final BigDecimal tasaIgv;
    private final Integer usuarioActualId;

    public CotizacionService(CotizacionRepository cotizaciones, ClienteRepository clientes,
                             SolicitudRepository solicitudes, ProductoRepository productos,
                             UsuarioRepository usuarios, ServicioTecnicoRepository servicios,
                             EntityManager em,
                             @Value("${app.igv.tasa}") BigDecimal tasaIgv,
                             @Value("${app.usuario-actual-id}") Integer usuarioActualId) {
        this.cotizaciones = cotizaciones;
        this.clientes = clientes;
        this.solicitudes = solicitudes;
        this.productos = productos;
        this.usuarios = usuarios;
        this.servicios = servicios;
        this.em = em;
        this.tasaIgv = tasaIgv;
        this.usuarioActualId = usuarioActualId;
    }

    // =====================================================================
    //  CONSULTAR
    // =====================================================================

    @Transactional(readOnly = true)
    public Page<CotizacionResumen> listar(EstadoCotizacion estado, String texto, LocalDate desde,
                                          LocalDate hasta, boolean incluirCerradas, Pageable pageable) {
        String filtro = texto == null || texto.isBlank() ? null : texto.trim();
        return cotizaciones.buscar(estado, filtro,
                        desde == null ? null : desde.atStartOfDay(),
                        hasta == null ? null : hasta.plusDays(1).atStartOfDay(),
                        incluirCerradas, pageable)
                .map(c -> new CotizacionResumen(c.getId(), c.getCodigoCompleto(),
                        c.getCliente().getNombreRazonSocial(), c.getFecha(), c.getEstado().name(),
                        c.getEstado().getEtiqueta(), c.getMontoTotal()));
    }

    @Transactional(readOnly = true)
    public CotizacionResponse obtener(Integer id) {
        return aRespuesta(cargarConDetalles(id));
    }

    // =====================================================================
    //  REGISTRAR
    // =====================================================================

    @Transactional
    public CotizacionResponse crear(CotizacionRequest req) {
        List<ErrorCampo> errores = new ArrayList<>();
        Cliente cliente = validarCliente(req.clienteId(), errores);
        Solicitud solicitud = validarSolicitud(req.solicitudId(), req.clienteId(), errores);
        Map<Integer, Producto> catalogo = cargarProductos(req.lineas());
        validarLineas(req.lineas(), catalogo, Map.of(), errores);
        if (!errores.isEmpty()) {
            throw new ValidacionException(errores);          // nada se escribió todavía
        }

        Cotizacion c = new Cotizacion();
        c.setCodigo(String.format("C-%05d", cotizaciones.siguienteNumero()));
        c.setNumeroVersion(1);
        c.setUsuario(usuarios.getReferenceById(usuarioActualId));
        c.setEstado(EstadoCotizacion.BORRADOR);
        c.setTasaIgv(tasaIgv);
        aplicarCabecera(c, req, cliente, solicitud);
        sincronizarLineas(c, req.lineas(), catalogo);
        recalcularTotales(c);

        cotizaciones.saveAndFlush(c);   // flush: si la BD rechaza algo, falla aquí y se hace rollback
        return aRespuesta(c);
    }

    // =====================================================================
    //  EDITAR (cabecera + agregar / modificar / quitar líneas)
    // =====================================================================

    @Transactional
    public CotizacionResponse actualizar(Integer id, CotizacionRequest req) {
        Cotizacion c = cargarConDetalles(id);
        if (!c.getEstado().esEditable()) {
            throw new ReglaNegocioException("La cotización " + c.getCodigoCompleto() + " está "
                    + c.getEstado().getEtiqueta().toLowerCase() + " y sus precios ya no se pueden cambiar. "
                    + "Si necesita modificarla, cree una nueva versión.");
        }
        verificarVersion(c, req.filaVersion());

        Map<Integer, DetalleCotizacion> actuales = c.getDetalles().stream()
                .collect(Collectors.toMap(DetalleCotizacion::getId, Function.identity()));

        List<ErrorCampo> errores = new ArrayList<>();
        Cliente cliente = validarCliente(req.clienteId(), errores);
        Solicitud solicitud = validarSolicitud(req.solicitudId(), req.clienteId(), errores);
        Map<Integer, Producto> catalogo = cargarProductos(req.lineas());
        validarLineas(req.lineas(), catalogo, actuales, errores);
        if (!errores.isEmpty()) {
            throw new ValidacionException(errores);
        }

        // Bloquea la fila e incrementa fila_version YA (aunque solo cambien líneas), así una
        // edición simultánea de otro usuario se detecta y las dos no se mezclan.
        em.lock(c, LockModeType.PESSIMISTIC_FORCE_INCREMENT);

        aplicarCabecera(c, req, cliente, solicitud);
        sincronizarLineas(c, req.lineas(), catalogo);
        recalcularTotales(c);

        cotizaciones.saveAndFlush(c);
        return aRespuesta(c);
    }

    // =====================================================================
    //  CAMBIOS DE ESTADO (emitir, negociar, aceptar, rechazar, anular...)
    // =====================================================================

    @Transactional
    public CotizacionResponse cambiarEstado(Integer id, CambioEstadoRequest req) {
        Cotizacion c = cargarConDetalles(id);
        verificarVersion(c, req.filaVersion());

        EstadoCotizacion actual = c.getEstado();
        EstadoCotizacion destino = req.estado();
        if (!actual.puedeCambiarA(destino)) {
            throw new ReglaNegocioException("No se puede pasar una cotización de \"" + actual.getEtiqueta()
                    + "\" a \"" + destino.getEtiqueta() + "\".");
        }
        String motivo = req.motivo() == null ? null : req.motivo().trim();
        if (destino.requiereMotivo() && (motivo == null || motivo.isEmpty())) {
            throw new ValidacionException(List.of(new ErrorCampo("motivo", "Indique el motivo")));
        }

        LocalDateTime ahora = LocalDateTime.now();
        switch (destino) {
            case EMITIDA -> emitir(c, ahora);
            case RECHAZADA, SIN_RESPUESTA -> c.setMotivoNoCierre(motivo);
            case ANULADA -> {
                if (servicios.existsByCotizacionIdAndEstadoNot(c.getId(), "ANULADO")) {
                    throw new ReglaNegocioException("No se puede anular: la cotización tiene servicios técnicos "
                            + "programados. Anule primero los servicios.");
                }
                c.setMotivoAnulacion(motivo);
                c.setFechaAnulacion(ahora);
            }
            default -> { /* EN_NEGOCIACION, ACEPTADA: sin datos adicionales */ }
        }
        c.setEstado(destino);
        cotizaciones.saveAndFlush(c);
        return aRespuesta(c);
    }

    private void emitir(Cotizacion c, LocalDateTime ahora) {
        if (c.getDetalles().isEmpty()) {
            throw new ReglaNegocioException("No se puede emitir una cotización sin líneas.");
        }
        if (c.getMontoTotal().signum() <= 0) {
            throw new ReglaNegocioException("No se puede emitir una cotización con total S/ 0.00.");
        }
        if (!c.getCliente().isActivo()) {
            throw new ReglaNegocioException("El cliente está archivado; reactívelo antes de emitir.");
        }
        c.setFechaEmision(ahora);

        // La solicitud de origen pasa a "cotizada" en la MISMA transacción
        Solicitud s = c.getSolicitud();
        if (s != null && (s.getEstado() == EstadoSolicitud.NUEVA || s.getEstado() == EstadoSolicitud.EN_ATENCION)) {
            s.setEstado(EstadoSolicitud.COTIZADA);
        }

        // Si es una nueva versión, la versión anterior queda reemplazada
        Cotizacion origen = c.getCotizacionOrigen();
        if (origen != null) {
            if (!origen.getEstado().permiteNuevaVersion()) {
                throw new ReglaNegocioException("La versión anterior (" + origen.getCodigoCompleto() + ") ya está "
                        + origen.getEstado().getEtiqueta().toLowerCase() + "; esta versión ya no puede emitirse.");
            }
            origen.setEstado(EstadoCotizacion.REEMPLAZADA);
        }
    }

    // =====================================================================
    //  NUEVA VERSIÓN (la forma de "editar" una cotización ya emitida)
    // =====================================================================

    @Transactional
    public CotizacionResponse nuevaVersion(Integer id) {
        Cotizacion origen = cargarConDetalles(id);
        if (!origen.getEstado().permiteNuevaVersion()) {
            throw new ReglaNegocioException("Solo se crean versiones de cotizaciones emitidas, en negociación "
                    + "o sin respuesta.");
        }
        if (cotizaciones.existsByCotizacionOrigenIdAndEstado(origen.getId(), EstadoCotizacion.BORRADOR)) {
            throw new ReglaNegocioException("Ya existe una nueva versión en borrador de esta cotización.");
        }

        Cotizacion nueva = new Cotizacion();
        nueva.setCodigo(origen.getCodigo());
        nueva.setNumeroVersion(cotizaciones.ultimaVersion(origen.getCodigo()) + 1);
        nueva.setCotizacionOrigen(origen);
        nueva.setUsuario(usuarios.getReferenceById(usuarioActualId));
        nueva.setEstado(EstadoCotizacion.BORRADOR);
        nueva.setTasaIgv(tasaIgv);
        nueva.setCliente(origen.getCliente());
        nueva.setSolicitud(origen.getSolicitud());
        nueva.setMargenAplicado(origen.getMargenAplicado());
        nueva.setDiasValidez(origen.getDiasValidez());
        nueva.setObservaciones(origen.getObservaciones());
        // Se copian las líneas con sus precios congelados; el usuario decide qué cambiar
        origen.getDetalles().forEach(d -> nueva.agregarDetalle(d.copiar()));
        recalcularTotales(nueva);

        cotizaciones.saveAndFlush(nueva);
        return aRespuesta(nueva);
    }

    // =====================================================================
    //  ELIMINAR (solo borradores; lo demás se anula = eliminación lógica)
    // =====================================================================

    @Transactional
    public void eliminar(Integer id) {
        Cotizacion c = cotizaciones.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("La cotización no existe."));
        if (c.getEstado() != EstadoCotizacion.BORRADOR) {
            throw new ReglaNegocioException("Solo se pueden eliminar borradores. Una cotización emitida se anula "
                    + "(queda registrada con su motivo).");
        }
        if (servicios.existsByCotizacionId(id)) {
            throw new ReglaNegocioException("No se puede eliminar: la cotización tiene servicios técnicos asociados.");
        }
        cotizaciones.delete(c);   // las líneas se eliminan en cascada
        cotizaciones.flush();
    }

    // =====================================================================
    //  VALIDACIONES DE NEGOCIO
    // =====================================================================

    private Cliente validarCliente(Integer clienteId, List<ErrorCampo> errores) {
        if (clienteId == null) {
            errores.add(new ErrorCampo("clienteId", "Seleccione un cliente"));
            return null;
        }
        Optional<Cliente> cliente = clientes.findById(clienteId);
        if (cliente.isEmpty()) {
            errores.add(new ErrorCampo("clienteId", "El cliente no existe"));
            return null;
        }
        if (!cliente.get().isActivo()) {
            errores.add(new ErrorCampo("clienteId", "El cliente está archivado"));
        }
        return cliente.get();
    }

    private Solicitud validarSolicitud(Integer solicitudId, Integer clienteId, List<ErrorCampo> errores) {
        if (solicitudId == null) {
            return null;                                   // la solicitud es opcional
        }
        Optional<Solicitud> s = solicitudes.findById(solicitudId);
        if (s.isEmpty()) {
            errores.add(new ErrorCampo("solicitudId", "La solicitud no existe"));
            return null;
        }
        if (!s.get().getCliente().getId().equals(clienteId)) {
            errores.add(new ErrorCampo("solicitudId", "La solicitud pertenece a otro cliente"));
        }
        if (s.get().getEstado() == EstadoSolicitud.CERRADA) {
            errores.add(new ErrorCampo("solicitudId", "La solicitud está cerrada"));
        }
        return s.get();
    }

    private void validarLineas(List<LineaRequest> lineas, Map<Integer, Producto> catalogo,
                               Map<Integer, DetalleCotizacion> actuales, List<ErrorCampo> errores) {
        if (lineas == null || lineas.isEmpty()) {
            errores.add(new ErrorCampo("lineas", "Agregue al menos una línea a la cotización"));
            return;
        }
        Set<Integer> idsVistos = new HashSet<>();
        for (int i = 0; i < lineas.size(); i++) {
            LineaRequest l = lineas.get(i);
            String p = "lineas[" + i + "].";
            if (l == null) {
                errores.add(new ErrorCampo("lineas[" + i + "]", "Línea vacía"));
                continue;
            }
            if (l.id() != null && (!actuales.containsKey(l.id()) || !idsVistos.add(l.id()))) {
                errores.add(new ErrorCampo(p + "id", "La línea no pertenece a esta cotización"));
                continue;
            }
            if (l.tipoItem() == null) {
                errores.add(new ErrorCampo(p + "tipoItem", "Indique el tipo de línea"));
                continue;
            }
            if (l.cantidad() == null || l.cantidad().signum() <= 0) {
                errores.add(new ErrorCampo(p + "cantidad", "La cantidad debe ser mayor a 0"));
            }

            DetalleCotizacion existente = l.id() == null ? null : actuales.get(l.id());
            switch (l.tipoItem()) {
                case PRODUCTO -> {
                    if (l.productoId() == null) {
                        errores.add(new ErrorCampo(p + "productoId", "Seleccione un producto"));
                    } else {
                        validarProducto(l, catalogo, existente, p, errores);
                    }
                }
                case MATERIAL -> {
                    if (l.productoId() != null) {
                        validarProducto(l, catalogo, existente, p, errores);
                    } else {
                        requerirTexto(l.descripcion(), p + "descripcion", "Describa el material", errores);
                        requerirTexto(l.unidad(), p + "unidad", "Indique la unidad", errores);
                        if (l.costoUnitario() == null) {
                            errores.add(new ErrorCampo(p + "costoUnitario", "Ingrese el costo del material"));
                        }
                    }
                }
                case MANO_OBRA -> {
                    if (l.productoId() != null) {
                        errores.add(new ErrorCampo(p + "productoId", "La mano de obra no lleva producto"));
                    }
                    requerirTexto(l.descripcion(), p + "descripcion", "Describa el trabajo", errores);
                    if (l.precioUnitario() == null) {
                        errores.add(new ErrorCampo(p + "precioUnitario", "Ingrese el precio de la mano de obra"));
                    }
                }
            }
        }
    }

    private void validarProducto(LineaRequest l, Map<Integer, Producto> catalogo, DetalleCotizacion existente,
                                 String p, List<ErrorCampo> errores) {
        Producto prod = catalogo.get(l.productoId());
        if (prod == null) {
            errores.add(new ErrorCampo(p + "productoId", "El producto no existe"));
            return;
        }
        boolean mismoProducto = existente != null && existente.getProducto() != null
                && existente.getProducto().getId().equals(prod.getId());
        // Un producto desactivado puede quedarse en una línea antigua, pero no agregarse de nuevo
        if (!mismoProducto && !Boolean.TRUE.equals(prod.getActivo())) {
            errores.add(new ErrorCampo(p + "productoId", "El producto \"" + prod.getNombre() + "\" está desactivado"));
        }
        if (prod.esUnidadEntera() && l.cantidad() != null
                && l.cantidad().stripTrailingZeros().scale() > 0) {
            errores.add(new ErrorCampo(p + "cantidad", "Este producto se vende por unidades enteras"));
        }
    }

    private static void requerirTexto(String valor, String campo, String mensaje, List<ErrorCampo> errores) {
        if (valor == null || valor.isBlank()) {
            errores.add(new ErrorCampo(campo, mensaje));
        }
    }

    private void verificarVersion(Cotizacion c, Integer filaVersionCliente) {
        if (filaVersionCliente == null || !filaVersionCliente.equals(c.getFilaVersion())) {
            throw new ConflictoConcurrenciaException();
        }
    }

    // =====================================================================
    //  ARMADO DEL AGREGADO Y CÁLCULOS
    // =====================================================================

    private void aplicarCabecera(Cotizacion c, CotizacionRequest req, Cliente cliente, Solicitud solicitud) {
        c.setCliente(cliente);
        c.setSolicitud(solicitud);
        c.setMargenAplicado(escala2(req.margenAplicado()));
        c.setDiasValidez(req.diasValidez());
        c.setObservaciones(textoONulo(req.observaciones()));
    }

    /**
     * Compara las líneas recibidas con las guardadas:
     * con id -> se actualiza, sin id -> se inserta, ids que faltan -> se eliminan (orphanRemoval).
     */
    private void sincronizarLineas(Cotizacion c, List<LineaRequest> lineas, Map<Integer, Producto> catalogo) {
        Map<Integer, DetalleCotizacion> actuales = c.getDetalles().stream()
                .filter(d -> d.getId() != null)
                .collect(Collectors.toMap(DetalleCotizacion::getId, Function.identity()));
        Set<Integer> conservadas = lineas.stream().map(LineaRequest::id)
                .filter(Objects::nonNull).collect(Collectors.toSet());

        c.getDetalles().removeIf(d -> d.getId() != null && !conservadas.contains(d.getId()));

        int nro = 1;
        for (LineaRequest l : lineas) {
            DetalleCotizacion d = l.id() == null ? null : actuales.get(l.id());
            boolean nueva = d == null;
            if (nueva) {
                d = new DetalleCotizacion();
            }
            llenarLinea(d, l, catalogo, c.getMargenAplicado());
            d.setNroLinea(nro++);
            if (nueva) {
                c.agregarDetalle(d);
            }
        }
        c.getDetalles().sort(Comparator.comparing(DetalleCotizacion::getNroLinea));
    }

    private void llenarLinea(DetalleCotizacion d, LineaRequest l, Map<Integer, Producto> catalogo,
                             BigDecimal margenCabecera) {
        d.setTipoItem(l.tipoItem());
        d.setCantidad(escala2(l.cantidad()));

        if (l.tipoItem() == TipoItem.MANO_OBRA) {
            d.setProducto(null);
            d.setCostoUnitario(null);
            d.setMargenPct(null);
            d.setDescripcion(l.descripcion().trim());
            d.setUnidad(textoONulo(l.unidad()) == null ? "SERV" : l.unidad().trim().toUpperCase());
            d.setPrecioUnitario(escala2(l.precioUnitario()));
        } else {
            if (l.productoId() != null) {
                Producto prod = catalogo.get(l.productoId());
                boolean mismoProducto = d.getProducto() != null && d.getProducto().getId().equals(prod.getId());
                if (!mismoProducto) {
                    // FOTO del precio del proveedor en este momento. Si la línea ya tenía este
                    // producto, se conserva el costo original aunque el proveedor lo haya cambiado.
                    d.setProducto(prod);
                    d.setCostoUnitario(prod.getPrecio());
                    d.setDescripcion(prod.getNombre());
                    d.setUnidad(prod.getUnidad());
                }
            } else {
                d.setProducto(null);
                d.setCostoUnitario(escala2(l.costoUnitario()));
                d.setDescripcion(l.descripcion().trim());
                d.setUnidad(l.unidad().trim().toUpperCase());
            }
            d.setMargenPct(escala2(l.margenPct() != null ? l.margenPct() : margenCabecera));
            d.setPrecioUnitario(CalculadoraCotizacion.precioVenta(d.getCostoUnitario(), d.getMargenPct()));
        }
        d.setImporte(CalculadoraCotizacion.importe(d.getCantidad(), d.getPrecioUnitario()));
    }

    private void recalcularTotales(Cotizacion c) {
        CalculadoraCotizacion.Totales t = CalculadoraCotizacion.totales(
                c.getDetalles().stream().map(DetalleCotizacion::getImporte).toList(), c.getTasaIgv());
        c.setSubtotal(t.subtotal());
        c.setIgv(t.igv());
        c.setMontoTotal(t.total());
    }

    private Map<Integer, Producto> cargarProductos(List<LineaRequest> lineas) {
        if (lineas == null) {
            return Map.of();
        }
        Set<Integer> ids = lineas.stream().filter(Objects::nonNull).map(LineaRequest::productoId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        if (ids.isEmpty()) {
            return Map.of();
        }
        return productos.buscarPorIds(ids).stream()
                .collect(Collectors.toMap(Producto::getId, Function.identity()));
    }

    private Cotizacion cargarConDetalles(Integer id) {
        return cotizaciones.buscarConDetalles(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("La cotización no existe."));
    }

    private static BigDecimal escala2(BigDecimal v) {
        return v == null ? null : v.setScale(2, RoundingMode.HALF_UP);
    }

    private static String textoONulo(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    // =====================================================================
    //  MAPEO A DTO
    // =====================================================================

    private CotizacionResponse aRespuesta(Cotizacion c) {
        EstadoCotizacion e = c.getEstado();
        List<CotizacionResponse.OpcionEstado> siguientes = e.siguientes().stream()
                .map(s -> new CotizacionResponse.OpcionEstado(s.name(), s.getEtiqueta(), s.requiereMotivo()))
                .toList();
        List<LineaResponse> lineas = c.getDetalles().stream().map(d -> new LineaResponse(
                d.getId(), d.getNroLinea(), d.getTipoItem().name(), d.getTipoItem().getEtiqueta(),
                d.getProducto() == null ? null : d.getProducto().getId(),
                d.getProducto() == null ? null : d.getProducto().getProveedor().getNombre(),
                d.getDescripcion(), d.getUnidad(), d.getCantidad(), d.getCostoUnitario(), d.getMargenPct(),
                d.getPrecioUnitario(), d.getImporte(),
                d.getProducto() == null ? null : d.getProducto().getPrecio())).toList();
        Cotizacion origen = c.getCotizacionOrigen();
        Solicitud s = c.getSolicitud();

        return new CotizacionResponse(
                c.getId(), c.getCodigo(), c.getCodigoCompleto(), c.getNumeroVersion(),
                origen == null ? null : origen.getCodigoCompleto(), origen == null ? null : origen.getId(),
                e.name(), e.getEtiqueta(),
                c.getCliente().getId(), c.getCliente().getNombreRazonSocial(), c.getCliente().getDocumento(),
                s == null ? null : s.getId(),
                s == null ? null : "S-" + String.format("%03d", s.getId()) + " · " + Objects.toString(s.getDescripcion(), ""),
                c.getUsuario().getNombre(),
                c.getFecha(), c.getFechaEmision(), c.getFechaVencimiento(), c.getDiasValidez(),
                c.getMargenAplicado(), c.getTasaIgv(), c.getSubtotal(), c.getIgv(), c.getMontoTotal(),
                c.getObservaciones(), c.getMotivoNoCierre(), c.getMotivoAnulacion(), c.getFilaVersion(),
                e.esEditable(), e == EstadoCotizacion.BORRADOR, e.permiteNuevaVersion(),
                siguientes, lineas);
    }
}
