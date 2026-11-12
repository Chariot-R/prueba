import { HttpErrorResponse } from '@angular/common/http';
import { ErrorApi } from './modelos';

/** Convierte cualquier error HTTP al formato único de la API ({ status, tipo, mensaje, errores }). */
export function errorApi(e: unknown): ErrorApi {
  if (e instanceof HttpErrorResponse) {
    if (e.status === 0) {
      return {
        status: 0,
        tipo: 'RED',
        mensaje: 'No se pudo conectar con el servidor. Revisa que el backend esté encendido.',
        errores: [],
      };
    }
    const cuerpo = e.error;
    if (cuerpo && typeof cuerpo === 'object' && 'mensaje' in cuerpo) {
      return { errores: [], ...cuerpo } as ErrorApi;
    }
    return { status: e.status, tipo: 'INTERNO', mensaje: 'Ocurrió un error inesperado.', errores: [] };
  }
  return { status: 0, tipo: 'INTERNO', mensaje: 'Ocurrió un error inesperado.', errores: [] };
}

/** Errores por campo: { documento: 'El RUC no es válido...', ... } */
export function erroresPorCampo(err: ErrorApi): Record<string, string> {
  const r: Record<string, string> = {};
  for (const e of err.errores ?? []) {
    if (e.campo && !r[e.campo]) r[e.campo] = e.mensaje;
  }
  return r;
}
