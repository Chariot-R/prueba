/*
 * Cálculo de la cotización en el navegador (vista previa).
 * Replica CalculadoraCotizacion.java trabajando en CÉNTIMOS ENTEROS para evitar
 * errores de punto flotante (0.1 + 0.2). El servidor recalcula siempre al guardar.
 *
 *   precio  = redondear(costo × (100 + margen) / 100)
 *   importe = redondear(cantidad × precio)
 *   total   = Σ importes (con IGV) ; subtotal = redondear(total / (1 + igv)) ; igv = total − subtotal
 */

/** "12.5" -> 1250 (céntimos o centésimas). Devuelve null si no es un número válido. */
export function aCentesimas(valor: string | number | null | undefined): number | null {
  if (valor === null || valor === undefined) return null;
  const texto = String(valor).trim().replace(',', '.');
  if (!/^\d+(\.\d{0,2})?$/.test(texto)) return null;
  const [enteros, dec = ''] = texto.split('.');
  return parseInt(enteros, 10) * 100 + parseInt((dec + '00').slice(0, 2), 10);
}

/** Redondeo HALF_UP de a / b para enteros positivos (igual que BigDecimal HALF_UP). */
function dividirRedondeando(a: number, b: number): number {
  return Math.floor((2 * a + b) / (2 * b));
}

export function precioVenta(costoCent: number, margenCent: number): number {
  return dividirRedondeando(costoCent * (10000 + margenCent), 10000);
}

export function importe(cantidadCent: number, precioCent: number): number {
  return dividirRedondeando(cantidadCent * precioCent, 100);
}

/** tasaIgvBp: tasa en puntos básicos (0.18 -> 1800). */
export function totales(importesCent: number[], tasaIgvBp: number) {
  const total = importesCent.reduce((s, x) => s + x, 0);
  const subtotal = dividirRedondeando(total * 10000, 10000 + tasaIgvBp);
  return { subtotal, igv: total - subtotal, total };
}

/** Céntimos -> "S/ 1,234.50" */
export function solesCent(centimos: number | null | undefined): string {
  if (centimos === null || centimos === undefined || isNaN(centimos)) return '—';
  const abs = Math.abs(centimos);
  const enteros = Math.floor(abs / 100).toString().replace(/\B(?=(\d{3})+(?!\d))/g, ',');
  return (centimos < 0 ? '-' : '') + 'S/ ' + enteros + '.' + String(abs % 100).padStart(2, '0');
}

export function aTexto(centesimas: number | null | undefined): string {
  return centesimas === null || centesimas === undefined ? '' : (centesimas / 100).toFixed(2);
}
