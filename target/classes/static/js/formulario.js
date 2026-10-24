/*
 * Formulario maestro-detalle de cotización.
 * - Mantiene las líneas en memoria, recalcula importes y totales en vivo.
 * - Valida en el navegador (ayuda al usuario) y envía TODO en una sola petición.
 * - Muestra los errores del servidor sobre el campo / la fila exacta.
 */
(function () {
    'use strict';

    var C = window.Calculo;
    var app = document.getElementById('app');
    var modo = app.dataset.modo;
    var cotizacionId = app.dataset.id || null;
    var margenDefecto = app.dataset.margenDefecto || '30';

    var estado = {
        filaVersion: null,
        tasaIgvBp: Math.round(parseFloat(app.dataset.tasaIgv || '0.18') * 10000),
        lineas: []
    };
    var sucio = false;
    var contadorUid = 0;

    var $ = function (sel, ctx) { return (ctx || document).querySelector(sel); };
    var tbody = $('#lineas');
    var esc = Api.escapar;

    var ETIQUETA_TIPO = { PRODUCTO: 'Producto', MATERIAL: 'Material', MANO_OBRA: 'Mano de obra' };

    // ------------------------------------------------------------------
    // Modelo de línea
    // ------------------------------------------------------------------

    function nuevaLinea(tipo) {
        return {
            uid: ++contadorUid, id: null, tipoItem: tipo, productoId: null, proveedor: null, stock: null,
            descripcion: '', unidad: tipo === 'MANO_OBRA' ? 'SERV' : '', cantidad: '1',
            costoUnitario: '', margenPct: tipo === 'MANO_OBRA' ? '' : $('#margenAplicado').value,
            precioUnitario: '', margenManual: false, costoActual: null, unidadEntera: false
        };
    }

    function desdeServidor(l, margenCabecera) {
        return {
            uid: ++contadorUid, id: l.id, tipoItem: l.tipoItem, productoId: l.productoId,
            proveedor: l.proveedorNombre, stock: null,
            descripcion: l.descripcion, unidad: l.unidad,
            cantidad: numeroTexto(l.cantidad),
            costoUnitario: l.costoUnitario == null ? '' : Number(l.costoUnitario).toFixed(2),
            margenPct: l.margenPct == null ? '' : numeroTexto(l.margenPct),
            precioUnitario: l.precioUnitario == null ? '' : Number(l.precioUnitario).toFixed(2),
            margenManual: l.margenPct != null && Number(l.margenPct) !== Number(margenCabecera),
            costoActual: l.costoActualProveedor == null ? null : C.aCentesimas(Number(l.costoActualProveedor).toFixed(2)),
            unidadEntera: (l.unidad || '').toUpperCase() === 'UND' && l.productoId != null
        };
    }

    function numeroTexto(n) {
        return n == null ? '' : String(Number(n));
    }

    function tieneProducto(l) { return l.productoId != null; }
    function usaCatalogo(l) { return l.tipoItem === 'PRODUCTO' || l.tipoItem === 'MATERIAL'; }

    function calcular(l) {
        var cant = C.aCentesimas(l.cantidad);
        var precio;
        if (l.tipoItem === 'MANO_OBRA') {
            precio = C.aCentesimas(l.precioUnitario);
        } else {
            var costo = C.aCentesimas(l.costoUnitario);
            var margen = C.aCentesimas(l.margenPct);
            precio = costo != null && margen != null ? C.precioVenta(costo, margen) : null;
        }
        return { precio: precio, importe: cant != null && precio != null ? C.importe(cant, precio) : null };
    }

    // ------------------------------------------------------------------
    // Render
    // ------------------------------------------------------------------

    function render() {
        if (estado.lineas.length === 0) {
            tbody.innerHTML = '<tr><td colspan="10" class="vacio">Agregue productos, materiales o mano de obra con los botones de arriba.</td></tr>';
        } else {
            tbody.innerHTML = estado.lineas.map(filaHtml).join('');
            estado.lineas.forEach(function (l) { actualizarCalculos(l); });
        }
        actualizarTotales();
    }

    function renderFila(l, enfocar) {
        var tr = tbody.querySelector('tr[data-uid="' + l.uid + '"]');
        if (!tr) return render();
        tr.outerHTML = filaHtml(l, estado.lineas.indexOf(l));
        actualizarCalculos(l);
        actualizarTotales();
        if (enfocar) {
            var input = tbody.querySelector('tr[data-uid="' + l.uid + '"] [data-f="' + enfocar + '"]');
            if (input) { input.focus(); var v = input.value; input.value = ''; input.value = v; }
        }
    }

    function filaHtml(l, i) {
        if (i === undefined) i = estado.lineas.indexOf(l);
        var catalogo = usaCatalogo(l);
        var conProducto = tieneProducto(l);
        var esMO = l.tipoItem === 'MANO_OBRA';

        var desc;
        if (catalogo) {
            var placeholder = l.tipoItem === 'PRODUCTO' ? 'Buscar producto o proveedor…' : 'Buscar en catálogo o escribir material…';
            desc = '<div class="buscador"><input data-f="descripcion" autocomplete="off" value="' + esc(l.descripcion) +
                '" placeholder="' + placeholder + '"><div class="sugerencias" hidden></div></div>' +
                '<div class="meta-linea" data-meta></div>';
        } else {
            desc = '<input data-f="descripcion" value="' + esc(l.descripcion) + '" maxlength="200" placeholder="Ej.: Instalación y configuración">';
        }

        // Producto: unidad y costo SOLO vienen del catálogo. Material: editables si es fuera de catálogo.
        var bloqueadoUnidad = l.tipoItem === 'PRODUCTO' || (catalogo && conProducto);
        var bloqueadoCosto = l.tipoItem !== 'MATERIAL' || conProducto;

        return '<tr data-uid="' + l.uid + '">' +
            '<td>' + (i + 1) + '</td>' +
            '<td><span class="tipo-chip tipo-' + l.tipoItem + '">' + ETIQUETA_TIPO[l.tipoItem] + '</span></td>' +
            '<td>' + desc + '</td>' +
            '<td><input data-f="unidad" maxlength="10" value="' + esc(l.unidad) + '"' + (bloqueadoUnidad ? ' readonly' : '') + '></td>' +
            '<td><input class="num" data-f="cantidad" inputmode="decimal" value="' + esc(l.cantidad) + '"></td>' +
            '<td>' + (esMO ? '<input class="num" value="—" disabled>' :
                '<input class="num" data-f="costoUnitario" inputmode="decimal" value="' + esc(l.costoUnitario) + '"' +
                (bloqueadoCosto ? ' readonly title="Precio del proveedor al momento de cotizar"' : '') + '>') + '</td>' +
            '<td>' + (esMO ? '<input class="num" value="—" disabled>' :
                '<input class="num" data-f="margenPct" inputmode="decimal" value="' + esc(l.margenPct) + '">') + '</td>' +
            '<td>' + (esMO ? '<input class="num" data-f="precioUnitario" inputmode="decimal" value="' + esc(l.precioUnitario) + '">' :
                '<input class="num" data-calc="precio" readonly tabindex="-1">') + '</td>' +
            '<td class="num"><strong data-calc="importe">—</strong></td>' +
            '<td><button type="button" class="btn-icono" data-quitar title="Quitar línea">✕</button></td>' +
            '</tr>';
    }

    function actualizarCalculos(l) {
        var tr = tbody.querySelector('tr[data-uid="' + l.uid + '"]');
        if (!tr) return;
        var r = calcular(l);
        var precioEl = tr.querySelector('[data-calc="precio"]');
        if (precioEl) precioEl.value = r.precio == null ? '' : C.aTexto(r.precio);
        tr.querySelector('[data-calc="importe"]').textContent = r.importe == null ? '—' : C.soles(r.importe);

        var meta = tr.querySelector('[data-meta]');
        if (!meta) return;
        var partes = [];
        var aviso = false;
        if (tieneProducto(l)) {
            if (l.proveedor) partes.push('Proveedor: ' + esc(l.proveedor));
            var cant = Number(l.cantidad);
            if (l.stock != null) {
                partes.push('Stock: ' + l.stock);
                if (cant > l.stock) { partes.push('cantidad mayor al stock del proveedor'); aviso = true; }
            }
            var costo = C.aCentesimas(l.costoUnitario);
            if (l.costoActual != null && costo != null && l.costoActual !== costo) {
                partes.push('precio actual del proveedor ' + C.soles(l.costoActual) + ' (se mantiene el original)');
                aviso = true;
            }
        } else if (l.tipoItem === 'MATERIAL' && l.descripcion) {
            partes.push('Material fuera de catálogo: ingrese su costo');
        }
        meta.innerHTML = partes.join(' · ');
        meta.className = 'meta-linea' + (aviso ? ' aviso' : '');
    }

    function actualizarTotales() {
        var importes = estado.lineas.map(function (l) { return calcular(l).importe; })
            .filter(function (x) { return x != null; });
        var t = C.totales(importes, estado.tasaIgvBp);
        $('#tSubtotal').textContent = C.soles(t.subtotal);
        $('#tIgv').textContent = C.soles(t.igv);
        $('#tTotal').textContent = C.soles(t.total);
        $('#tIgvEtiqueta').textContent = 'IGV (' + (estado.tasaIgvBp / 100) + ' %)';
    }

    // ------------------------------------------------------------------
    // Eventos del detalle
    // ------------------------------------------------------------------

    function lineaDe(elemento) {
        var tr = elemento.closest('tr[data-uid]');
        if (!tr) return null;
        var uid = Number(tr.dataset.uid);
        return estado.lineas.find(function (l) { return l.uid === uid; });
    }

    document.querySelectorAll('[data-agregar]').forEach(function (btn) {
        btn.addEventListener('click', function () {
            var l = nuevaLinea(btn.dataset.agregar);
            estado.lineas.push(l);
            sucio = true;
            $('[data-error-de="lineas"]').textContent = '';
            Api.alerta('error', '');
            render();
            var input = tbody.querySelector('tr[data-uid="' + l.uid + '"] [data-f="descripcion"]');
            if (input) input.focus();
        });
    });

    tbody.addEventListener('click', function (e) {
        if (e.target.closest('[data-quitar]')) {
            var l = lineaDe(e.target);
            estado.lineas.splice(estado.lineas.indexOf(l), 1);
            sucio = true;
            render();
        }
    });

    tbody.addEventListener('input', function (e) {
        var campo = e.target.dataset.f;
        var l = lineaDe(e.target);
        if (!campo || !l) return;
        sucio = true;
        limpiarErrorElemento(e.target);
        l[campo] = e.target.value;
        if (campo === 'margenPct') l.margenManual = true;

        if (campo === 'descripcion' && usaCatalogo(l)) {
            if (tieneProducto(l)) {
                // Cambió el texto de un producto ya elegido: se desvincula del catálogo
                l.productoId = null; l.proveedor = null; l.stock = null; l.costoActual = null; l.unidadEntera = false;
                l.costoUnitario = ''; l.unidad = '';
                renderFila(l, 'descripcion');
            }
            buscarProductos(l, e.target.value);
            return;
        }
        actualizarCalculos(l);
        actualizarTotales();
    });

    // ------------------------------------------------------------------
    // Autocompletado de productos
    // ------------------------------------------------------------------

    var temporizador = null;
    var sugerenciasActuales = [];

    function cajaSugerencias(l) {
        return tbody.querySelector('tr[data-uid="' + l.uid + '"] .sugerencias');
    }

    function buscarProductos(l, texto) {
        clearTimeout(temporizador);
        var caja = cajaSugerencias(l);
        if (!texto || texto.trim().length < 2) { if (caja) caja.hidden = true; return; }
        temporizador = setTimeout(function () {
            Api.llamar('GET', '/api/productos?q=' + encodeURIComponent(texto.trim())).then(function (lista) {
                sugerenciasActuales = lista;
                var c = cajaSugerencias(l);
                var input = c && c.parentElement.querySelector('[data-f="descripcion"]');
                if (!c || document.activeElement !== input) return;   // el usuario ya salió del campo
                if (lista.length === 0) {
                    c.innerHTML = '<div><small>' + (l.tipoItem === 'MATERIAL'
                        ? 'Sin coincidencias: se registrará como material fuera de catálogo.'
                        : 'No hay productos activos con ese nombre.') + '</small></div>';
                } else {
                    c.innerHTML = lista.map(function (p, i) {
                        return '<div data-sug="' + i + '"><strong>' + esc(p.nombre) + '</strong>' +
                            '<small>' + esc(p.proveedor) + ' · ' + C.soles(C.aCentesimas(Number(p.precio).toFixed(2))) +
                            ' / ' + esc(p.unidad) + ' · <span class="' + (p.stock > 0 ? '' : 'sin-stock') + '">stock ' + p.stock + '</span></small></div>';
                    }).join('');
                }
                c.hidden = false;
            }).catch(function () { /* la búsqueda es auxiliar; si falla no se interrumpe al usuario */ });
        }, 250);
    }

    tbody.addEventListener('mousedown', function (e) {
        var item = e.target.closest('[data-sug]');
        if (!item) return;
        e.preventDefault();
        elegirProducto(lineaDe(item), sugerenciasActuales[Number(item.dataset.sug)]);
    });

    tbody.addEventListener('keydown', function (e) {
        if (e.target.dataset.f !== 'descripcion') return;
        var l = lineaDe(e.target);
        var caja = l && cajaSugerencias(l);
        if (!caja || caja.hidden) return;
        var items = Array.prototype.slice.call(caja.querySelectorAll('[data-sug]'));
        if (!items.length) return;
        var actual = items.findIndex(function (x) { return x.classList.contains('marcada'); });
        if (e.key === 'ArrowDown' || e.key === 'ArrowUp') {
            e.preventDefault();
            var sig = e.key === 'ArrowDown' ? Math.min(actual + 1, items.length - 1) : Math.max(actual - 1, 0);
            items.forEach(function (x) { x.classList.remove('marcada'); });
            items[sig].classList.add('marcada');
        } else if (e.key === 'Enter' && actual >= 0) {
            e.preventDefault();
            elegirProducto(l, sugerenciasActuales[Number(items[actual].dataset.sug)]);
        } else if (e.key === 'Escape') {
            caja.hidden = true;
        }
    });

    tbody.addEventListener('focusout', function (e) {
        if (e.target.dataset.f === 'descripcion') {
            var l = lineaDe(e.target);
            setTimeout(function () { var c = l && cajaSugerencias(l); if (c) c.hidden = true; }, 150);
        }
    });

    function elegirProducto(l, p) {
        if (!l || !p) return;
        l.productoId = p.id;
        l.descripcion = p.nombre;
        l.proveedor = p.proveedor;
        l.stock = p.stock;
        l.unidad = p.unidad;
        l.unidadEntera = (p.unidad || '').toUpperCase() === 'UND';
        l.costoUnitario = Number(p.precio).toFixed(2);   // foto del precio actual del proveedor
        l.costoActual = null;
        sucio = true;
        renderFila(l, 'cantidad');
    }

    // ------------------------------------------------------------------
    // Cabecera
    // ------------------------------------------------------------------

    $('#margenAplicado').addEventListener('input', function (e) {
        sucio = true;
        limpiarErrorElemento(e.target);
        estado.lineas.forEach(function (l) {
            if (l.tipoItem !== 'MANO_OBRA' && !l.margenManual) {
                l.margenPct = e.target.value;
                var input = tbody.querySelector('tr[data-uid="' + l.uid + '"] [data-f="margenPct"]');
                if (input) input.value = e.target.value;
                actualizarCalculos(l);
            }
        });
        actualizarTotales();
    });

    $('#clienteId').addEventListener('change', function (e) {
        sucio = true;
        limpiarErrorElemento(e.target);
        cargarSolicitudes(e.target.value, null);
    });

    ['#diasValidez', '#observaciones', '#solicitudId'].forEach(function (sel) {
        $(sel).addEventListener('input', function (e) { sucio = true; limpiarErrorElemento(e.target); });
    });

    function cargarSolicitudes(clienteId, seleccionada) {
        var sel = $('#solicitudId');
        sel.innerHTML = '<option value="">Sin solicitud</option>';
        sel.disabled = !clienteId;
        if (!clienteId) return Promise.resolve();
        return Api.llamar('GET', '/api/clientes/' + clienteId + '/solicitudes').then(function (lista) {
            lista.forEach(function (s) {
                var o = document.createElement('option');
                o.value = s.id;
                o.textContent = s.etiqueta;
                sel.appendChild(o);
            });
            if (seleccionada) sel.value = String(seleccionada);
        }).catch(function () {
            Api.alerta('aviso', 'No se pudieron cargar las solicitudes del cliente.');
        });
    }

    // ------------------------------------------------------------------
    // Validación en el navegador (mismas reglas que el servidor)
    // ------------------------------------------------------------------

    function validar() {
        var errores = [];
        var add = function (campo, mensaje) { errores.push({ campo: campo, mensaje: mensaje }); };
        var margen = C.aCentesimas($('#margenAplicado').value);
        var dias = Number($('#diasValidez').value);

        if (!$('#clienteId').value) add('clienteId', 'Seleccione un cliente');
        if (margen == null || margen > 10000) add('margenAplicado', 'Ingrese un margen entre 0 y 100 (máx. 2 decimales)');
        if (!Number.isInteger(dias) || dias < 1 || dias > 90) add('diasValidez', 'Ingrese entre 1 y 90 días');
        if (estado.lineas.length === 0) add('lineas', 'Agregue al menos una línea a la cotización');

        estado.lineas.forEach(function (l, i) {
            var p = 'lineas[' + i + '].';
            var cant = C.aCentesimas(l.cantidad);
            if (cant == null || cant <= 0) add(p + 'cantidad', 'Cantidad mayor a 0 (máx. 2 decimales)');
            else if (l.unidadEntera && cant % 100 !== 0) add(p + 'cantidad', 'Este producto se vende por unidades enteras');

            if (l.tipoItem === 'MANO_OBRA') {
                if (!l.descripcion.trim()) add(p + 'descripcion', 'Describa el trabajo');
                if (C.aCentesimas(l.precioUnitario) == null) add(p + 'precioUnitario', 'Ingrese un precio válido');
                return;
            }
            var m = C.aCentesimas(l.margenPct);
            if (m == null || m > 10000) add(p + 'margenPct', 'Margen entre 0 y 100');
            if (l.tipoItem === 'PRODUCTO' && !tieneProducto(l)) {
                add(p + 'productoId', 'Seleccione un producto de la lista');
            }
            if (l.tipoItem === 'MATERIAL' && !tieneProducto(l)) {
                if (!l.descripcion.trim()) add(p + 'descripcion', 'Describa el material');
                if (!l.unidad.trim()) add(p + 'unidad', 'Indique la unidad');
                if (C.aCentesimas(l.costoUnitario) == null) add(p + 'costoUnitario', 'Ingrese un costo válido');
            }
        });
        return errores;
    }

    // ------------------------------------------------------------------
    // Errores (del navegador o del servidor, mismo formato)
    // ------------------------------------------------------------------

    var CAMPO_VISIBLE = { productoId: 'descripcion', id: 'descripcion', tipoItem: 'descripcion' };

    function limpiarErrores() {
        document.querySelectorAll('.invalido').forEach(function (x) { x.classList.remove('invalido'); x.removeAttribute('title'); });
        document.querySelectorAll('.msg-error[data-generado]').forEach(function (x) { x.remove(); });
        document.querySelectorAll('.fila-error').forEach(function (x) { x.classList.remove('fila-error'); });
        $('[data-error-de="lineas"]').textContent = '';
        Api.alerta('error', '');
    }

    function limpiarErrorElemento(el) {
        el.classList.remove('invalido');
        el.removeAttribute('title');
        var sig = el.parentElement && el.parentElement.querySelector('.msg-error[data-generado]');
        if (sig) sig.remove();
    }

    function mostrarErrores(errores, mensajeGeneral) {
        limpiarErrores();
        var resumen = [];
        errores.forEach(function (err) {
            var m = /^lineas\[(\d+)\]\.?(\w*)$/.exec(err.campo || '');
            if (m) {
                var i = Number(m[1]);
                var l = estado.lineas[i];
                resumen.push('Línea ' + (i + 1) + ': ' + err.mensaje);
                if (!l) return;
                var tr = tbody.querySelector('tr[data-uid="' + l.uid + '"]');
                if (!tr) return;
                tr.classList.add('fila-error');
                var input = tr.querySelector('[data-f="' + (CAMPO_VISIBLE[m[2]] || m[2]) + '"]');
                if (input) { input.classList.add('invalido'); input.title = err.mensaje; }
            } else if (err.campo === 'lineas') {
                $('[data-error-de="lineas"]').textContent = err.mensaje;
                resumen.push(err.mensaje);
            } else {
                var el = document.querySelector('[data-campo="' + err.campo + '"]');
                if (el) {
                    el.classList.add('invalido');
                    var div = document.createElement('div');
                    div.className = 'msg-error';
                    div.setAttribute('data-generado', '');
                    div.textContent = err.mensaje;
                    el.parentElement.appendChild(div);
                } else {
                    resumen.push(err.mensaje);
                }
            }
        });
        var html = esc(mensajeGeneral || 'Revise los datos marcados antes de guardar.');
        if (resumen.length) {
            html += '<ul>' + resumen.slice(0, 8).map(function (r) { return '<li>' + esc(r) + '</li>'; }).join('') + '</ul>';
        }
        Api.alerta('error', html);
    }

    // ------------------------------------------------------------------
    // Guardar: cabecera + todas las líneas en UNA petición (una transacción en el servidor)
    // ------------------------------------------------------------------

    function numeroONulo(texto) {
        var c = C.aCentesimas(texto);
        return c == null ? null : c / 100;
    }

    function construirPeticion() {
        return {
            clienteId: Number($('#clienteId').value) || null,
            solicitudId: Number($('#solicitudId').value) || null,
            margenAplicado: numeroONulo($('#margenAplicado').value),
            diasValidez: Number($('#diasValidez').value) || null,
            observaciones: $('#observaciones').value.trim() || null,
            filaVersion: estado.filaVersion,
            // Importes y totales NO se envían: el servidor los calcula
            lineas: estado.lineas.map(function (l) {
                return {
                    id: l.id,
                    tipoItem: l.tipoItem,
                    productoId: l.productoId,
                    descripcion: l.descripcion.trim() || null,
                    unidad: l.unidad.trim() || null,
                    cantidad: numeroONulo(l.cantidad),
                    costoUnitario: l.tipoItem === 'MANO_OBRA' ? null : numeroONulo(l.costoUnitario),
                    margenPct: l.tipoItem === 'MANO_OBRA' ? null : numeroONulo(l.margenPct),
                    precioUnitario: l.tipoItem === 'MANO_OBRA' ? numeroONulo(l.precioUnitario) : null
                };
            })
        };
    }

    var btnGuardar = $('#btnGuardar');
    btnGuardar.addEventListener('click', function () {
        var errores = validar();
        if (errores.length) { mostrarErrores(errores); return; }

        limpiarErrores();
        btnGuardar.disabled = true;
        btnGuardar.textContent = 'Guardando…';

        var peticion = modo === 'nueva'
            ? Api.llamar('POST', '/api/cotizaciones', construirPeticion())
            : Api.llamar('PUT', '/api/cotizaciones/' + cotizacionId, construirPeticion());

        peticion.then(function (r) {
            sucio = false;
            location.href = Api.url('/cotizaciones/' + r.id + '?guardado');
        }).catch(function (err) {
            btnGuardar.disabled = false;
            btnGuardar.textContent = 'Guardar borrador';
            if (err.tipo === 'VALIDACION') mostrarErrores(err.errores || [], err.mensaje);
            else Api.alertaError(err);
        });
    });

    window.addEventListener('beforeunload', function (e) {
        if (sucio) { e.preventDefault(); e.returnValue = ''; }
    });

    // ------------------------------------------------------------------
    // Inicio
    // ------------------------------------------------------------------

    if (modo === 'editar') {
        Api.llamar('GET', '/api/cotizaciones/' + cotizacionId).then(function (c) {
            estado.filaVersion = c.filaVersion;
            estado.tasaIgvBp = Math.round(Number(c.tasaIgv) * 10000);
            $('#clienteId').value = String(c.clienteId);
            $('#margenAplicado').value = numeroTexto(c.margenAplicado);
            $('#diasValidez').value = c.diasValidez;
            $('#observaciones').value = c.observaciones || '';
            estado.lineas = c.lineas.map(function (l) { return desdeServidor(l, c.margenAplicado); });
            render();
            return cargarSolicitudes(c.clienteId, c.solicitudId);
        }).catch(function (err) { Api.alertaError(err); });
    } else {
        $('#margenAplicado').value = margenDefecto;
        render();
    }
})();
