import { inject } from '@angular/core';
import { CanActivateFn, CanDeactivateFn, Router } from '@angular/router';
import { SesionService } from './sesion.service';

/**
 * Guardias de navegación. Solo deciden qué pantalla mostrar: la protección real está en el
 * backend, que revisa la sesión y el rol en CADA petición.
 */

/** Intranet: exige sesión de personal y, si se indica, uno de los roles. */
export function soloPersonal(...roles: string[]): CanActivateFn {
  return async (_ruta, estado) => {
    const sesion = inject(SesionService);
    const router = inject(Router);
    let s;
    try {
      s = await sesion.cargarPersonal();
    } catch {
      return router.createUrlTree(['/intranet/login']);
    }
    if (!s) return router.createUrlTree(['/intranet/login']);
    if (s.debeCambiarPassword && !estado.url.startsWith('/intranet/cambiar-password')) {
      return router.createUrlTree(['/intranet/cambiar-password']);
    }
    if (roles.length && !roles.includes(s.rol)) {
      return router.parseUrl(SesionService.inicioPersonal(s));
    }
    return true;
  };
}

/** Portal: exige sesión de cliente. */
export const soloCliente: CanActivateFn = async () => {
  const sesion = inject(SesionService);
  const router = inject(Router);
  try {
    return (await sesion.cargarCliente()) ? true : router.createUrlTree(['/login']);
  } catch {
    return router.createUrlTree(['/login']);
  }
};

/** Páginas de login/registro de clientes: si ya entró, va directo a "Mi cuenta". */
export const invitadoCliente: CanActivateFn = async () => {
  const sesion = inject(SesionService);
  try {
    return (await sesion.cargarCliente()) ? inject(Router).createUrlTree(['/mi-cuenta']) : true;
  } catch {
    return true;
  }
};

/** Login de la intranet: si ya entró, va a su pantalla de inicio. */
export const invitadoPersonal: CanActivateFn = async () => {
  const sesion = inject(SesionService);
  try {
    const s = await sesion.cargarPersonal();
    return s ? inject(Router).parseUrl(SesionService.inicioPersonal(s)) : true;
  } catch {
    return true;
  }
};

/** Pantallas con datos sin guardar implementan esto para avisar antes de salir. */
export interface ConCambiosPendientes {
  hayCambiosSinGuardar(): boolean;
}

export const avisarCambiosPendientes: CanDeactivateFn<ConCambiosPendientes> = (componente) =>
  !componente.hayCambiosSinGuardar() ||
  confirm('Hay cambios sin guardar. ¿Salir de todos modos? Se perderán.');
