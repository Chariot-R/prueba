/* Llamadas a la API y presentación uniforme de errores. */
(function (global) {
    'use strict';

    var base = (document.getElementById('app') && document.getElementById('app').dataset.base) || '/';

    function url(ruta) {
        return base.replace(/\/$/, '') + ruta;
    }

    /**
     * fetch JSON. Si la respuesta no es 2xx lanza un objeto con el ErrorRespuesta del servidor:
     * { status, tipo, mensaje, errores: [{campo, mensaje}] }
     */
    function llamar(metodo, ruta, cuerpo) {
        var opciones = { method: metodo, headers: { 'Accept': 'application/json' } };
        if (cuerpo !== undefined) {
            opciones.headers['Content-Type'] = 'application/json';
            opciones.body = JSON.stringify(cuerpo);
        }
        return fetch(url(ruta), opciones).then(function (resp) {
            if (resp.status === 204) return null;
            return resp.json().catch(function () { return null; }).then(function (datos) {
                if (resp.ok) return datos;
                throw datos || { status: resp.status, tipo: 'INTERNO', mensaje: 'Error de comunicación con el servidor.', errores: [] };
            });
        }, function () {
            throw { status: 0, tipo: 'RED', mensaje: 'No se pudo conectar con el servidor. Verifique su conexión; los datos del formulario se conservan.', errores: [] };
        });
    }

    function escapar(texto) {
        return String(texto == null ? '' : texto)
            .replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');
    }

    /** Muestra un mensaje en el contenedor #alertas. */
    function alerta(tipo, html) {
        var cont = document.getElementById('alertas');
        if (!cont) return;
        cont.innerHTML = html ? '<div class="alerta alerta-' + tipo + '">' + html + '</div>' : '';
        if (html) cont.scrollIntoView({ behavior: 'smooth', block: 'start' });
    }

    function alertaError(err) {
        var html = escapar(err.mensaje || 'Ocurrió un error.');
        if (err.tipo === 'CONCURRENCIA') {
            html += ' <a href="javascript:location.reload()">Recargar ahora</a>';
        }
        alerta('error', html);
    }

    global.Api = { llamar: llamar, url: url, escapar: escapar, alerta: alerta, alertaError: alertaError };
})(this);
