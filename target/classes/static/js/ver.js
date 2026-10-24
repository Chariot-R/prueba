/* Acciones de la vista de detalle: cambios de estado, nueva versión y eliminación. */
(function () {
    'use strict';

    var app = document.getElementById('app');
    var id = app.dataset.id;
    var filaVersion = Number(app.dataset.filaVersion);

    var dlg = document.getElementById('dlgMotivo');
    var txt = document.getElementById('txtMotivo');
    var err = document.getElementById('errMotivo');
    var pendiente = null;

    function recargar() {
        location.href = Api.url('/cotizaciones/' + id);
    }

    function cambiarEstado(estado, motivo) {
        return Api.llamar('POST', '/api/cotizaciones/' + id + '/estado',
            { estado: estado, motivo: motivo || null, filaVersion: filaVersion });
    }

    function deshabilitarBotones(valor) {
        document.querySelectorAll('.acciones button').forEach(function (b) { b.disabled = valor; });
    }

    function manejarError(e) {
        deshabilitarBotones(false);
        if (e.tipo === 'VALIDACION' && e.errores && e.errores.length) {
            Api.alerta('error', Api.escapar(e.errores.map(function (x) { return x.mensaje; }).join('. ')));
        } else {
            Api.alertaError(e);
        }
    }

    document.querySelectorAll('[data-estado]').forEach(function (btn) {
        btn.addEventListener('click', function () {
            var estado = btn.dataset.estado;
            if (btn.dataset.motivo === 'true') {
                pendiente = estado;
                document.getElementById('dlgTitulo').textContent = 'Marcar como "' + btn.dataset.etiqueta + '"';
                txt.value = '';
                err.textContent = '';
                dlg.showModal();
                txt.focus();
                return;
            }
            if (estado === 'EMITIDA' && !confirmarInline(btn,
                    '¿Emitir? Los precios quedarán fijos y ya no podrá editar el borrador.')) {
                return;
            }
            deshabilitarBotones(true);
            cambiarEstado(estado).then(recargar).catch(manejarError);
        });
    });

    document.getElementById('dlgCancelar').addEventListener('click', function () { dlg.close(); });
    document.getElementById('dlgAceptar').addEventListener('click', function () {
        var motivo = txt.value.trim();
        if (!motivo) { err.textContent = 'Indique el motivo'; txt.classList.add('invalido'); return; }
        dlg.close();
        deshabilitarBotones(true);
        cambiarEstado(pendiente, motivo).then(recargar).catch(manejarError);
    });

    var btnVersion = document.querySelector('[data-nueva-version]');
    if (btnVersion) {
        btnVersion.addEventListener('click', function () {
            if (!confirmarInline(btnVersion, 'Se creará un borrador copiando esta cotización. ¿Continuar?')) return;
            deshabilitarBotones(true);
            Api.llamar('POST', '/api/cotizaciones/' + id + '/nueva-version').then(function (r) {
                location.href = Api.url('/cotizaciones/' + r.id + '/editar');
            }).catch(manejarError);
        });
    }

    var btnEliminar = document.querySelector('[data-eliminar]');
    if (btnEliminar) {
        btnEliminar.addEventListener('click', function () {
            if (!confirmarInline(btnEliminar, 'Se eliminará el borrador y todas sus líneas. ¿Continuar?')) return;
            deshabilitarBotones(true);
            Api.llamar('DELETE', '/api/cotizaciones/' + id).then(function () {
                location.href = Api.url('/cotizaciones');
            }).catch(manejarError);
        });
    }

    /**
     * Confirmación en dos clics sin ventanas nativas: el primer clic cambia el texto del botón
     * y muestra el aviso; el segundo (dentro de 5 s) confirma.
     */
    function confirmarInline(btn, aviso) {
        if (btn.dataset.armado === '1') {
            btn.dataset.armado = '';
            return true;
        }
        var original = btn.textContent;
        btn.dataset.armado = '1';
        btn.textContent = '¿Confirmar?';
        Api.alerta('aviso', Api.escapar(aviso) + ' Pulse de nuevo el botón para confirmar.');
        setTimeout(function () {
            btn.dataset.armado = '';
            btn.textContent = original;
        }, 5000);
        return false;
    }
})();
