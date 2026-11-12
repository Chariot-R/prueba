import { Pipe, PipeTransform } from '@angular/core';

/** Formatos de la interfaz: soles, números y fechas (siempre con el mismo estilo). */

export function soles(valor: number | string | null | undefined): string {
  if (valor === null || valor === undefined || valor === '') return '—';
  const n = Number(valor);
  if (isNaN(n)) return '—';
  return 'S/ ' + n.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
}

export function numero(valor: number | string | null | undefined): string {
  if (valor === null || valor === undefined || valor === '') return '—';
  return String(Number(valor));
}

/** "2026-11-12T17:20:00" -> "12/11/2026" (sin conversión de zona horaria). */
export function fecha(iso: string | null | undefined): string {
  if (!iso) return '—';
  const [d] = iso.split('T');
  const [a, m, dia] = d.split('-');
  return `${dia}/${m}/${a}`;
}

export function fechaHora(iso: string | null | undefined): string {
  if (!iso) return '—';
  const hora = (iso.split('T')[1] ?? '').slice(0, 5);
  return `${fecha(iso)} ${hora}`;
}

/** Clase CSS del indicador de estado: EN_NEGOCIACION -> "estado estado-en-negociacion". */
export function claseEstado(estado: string | null | undefined): string {
  return 'estado estado-' + (estado ?? '').toLowerCase().replace(/_/g, '-');
}

@Pipe({ name: 'soles' })
export class SolesPipe implements PipeTransform {
  transform = soles;
}

@Pipe({ name: 'numero' })
export class NumeroPipe implements PipeTransform {
  transform = numero;
}

@Pipe({ name: 'fecha' })
export class FechaPipe implements PipeTransform {
  transform = fecha;
}

@Pipe({ name: 'fechaHora' })
export class FechaHoraPipe implements PipeTransform {
  transform = fechaHora;
}

@Pipe({ name: 'claseEstado' })
export class ClaseEstadoPipe implements PipeTransform {
  transform = claseEstado;
}
