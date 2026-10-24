/*
 * Cálculo de la cotización en el navegador (vista previa).
 * Replica CalculadoraCotizacion.java trabajando en CÉNTIMOS ENTEROS para evitar
 * errores de punto flotante (0.1 + 0.2). El servidor recalcula siempre al guardar.
 *
 *   precio  = redondear(costo × (100 + margen) / 100)
 *   importe = redondear(cantidad × precio)
 *   total   = Σ importes (con IGV) ; subtotal = redondear(total / (1 + igv)) ; igv = total − subtotal
 */
(function (global) {
    'use strict';

    /** "12.5" -> 1250 (céntimos o centésimas). Devuelve null si no es un número válido. */
    function aCentesimas(valor) {
        if (valor === null || valor === undefined) return null;
        var texto = String(valor).trim().replace(',', '.');
        if (!/^\d+(\.\d{0,2})?$/.test(texto)) return null;
        var partes = texto.split('.');
        var decimales = ((partes[1] || '') + '00').slice(0, 2);
        return parseInt(partes[0], 10) * 100 + parseInt(decimales, 10);
    }

    /** Redondeo HALF_UP de a / b para enteros positivos (igual que BigDecimal HALF_UP). */
    function dividirRedondeando(a, b) {
        return Math.floor((2 * a + b) / (2 * b));
    }

    function precioVenta(costoCent, margenCent) {
        return dividirRedondeando(costoCent * (10000 + margenCent), 10000);
    }

    function importe(cantidadCent, precioCent) {
        return dividirRedondeando(cantidadCent * precioCent, 100);
    }

    /** tasaIgvBp: tasa en puntos básicos (0.18 -> 1800). */
    function totales(importesCent, tasaIgvBp) {
        var total = importesCent.reduce(function (s, x) { return s + x; }, 0);
        var subtotal = dividirRedondeando(total * 10000, 10000 + tasaIgvBp);
        return { subtotal: subtotal, igv: total - subtotal, total: total };
    }

    function soles(centimos) {
        if (centimos === null || centimos === undefined || isNaN(centimos)) return '—';
        var negativo = centimos < 0;
        var abs = Math.abs(centimos);
        var enteros = Math.floor(abs / 100).toString().replace(/\B(?=(\d{3})+(?!\d))/g, ',');
        var dec = String(abs % 100).padStart(2, '0');
        return (negativo ? '-' : '') + 'S/ ' + enteros + '.' + dec;
    }

    function aTexto(centesimas) {
        if (centesimas === null || centesimas === undefined) return '';
        return (centesimas / 100).toFixed(2);
    }

    var api = {
        aCentesimas: aCentesimas, precioVenta: precioVenta, importe: importe,
        totales: totales, soles: soles, aTexto: aTexto
    };
    if (typeof module !== 'undefined' && module.exports) {
        module.exports = api;
    } else {
        global.Calculo = api;
    }
})(this);
