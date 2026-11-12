import { HttpClient, HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, switchMap, throwError } from 'rxjs';
import { SesionService } from './sesion.service';

const REINTENTO = 'X-Reintento-Csrf';

function leerCookie(nombre: string): string | null {
  const m = document.cookie.match(new RegExp('(?:^|; )' + nombre + '=([^;]*)'));
  return m ? decodeURIComponent(m[1]) : null;
}

/**
 * Reacciona a los errores de sesión de cualquier petición:
 *  - 403 CSRF (token vencido, p. ej. tras cerrar sesión): pide un token nuevo y reintenta UNA vez.
 *  - 401 en la intranet/portal (sesión vencida): vuelve al login correspondiente.
 *  - 403 CAMBIO_PASSWORD: lleva a cambiar la contraseña temporal.
 * Los errores de login mismo (credenciales) los maneja la pantalla de login.
 */
export const erroresInterceptor: HttpInterceptorFn = (req, next) => {
  const router = inject(Router);
  const sesion = inject(SesionService);
  const http = inject(HttpClient);

  return next(req).pipe(
    catchError((e: unknown) => {
      if (!(e instanceof HttpErrorResponse)) return throwError(() => e);

      if (e.status === 403 && e.error?.tipo === 'CSRF' && !req.headers.has(REINTENTO)) {
        // Cualquier GET a la API devuelve una cookie XSRF-TOKEN nueva
        return http.get('/api/portal/auth/yo').pipe(
          switchMap(() =>
            next(req.clone({ setHeaders: { [REINTENTO]: '1', 'X-XSRF-TOKEN': leerCookie('XSRF-TOKEN') ?? '' } })),
          ),
        );
      }

      if (!req.url.includes('/auth/login')) {
        const intranet = req.url.startsWith('/api/intranet');
        const portal = req.url.startsWith('/api/portal');
        if (e.status === 401 && intranet) {
          sesion.personal.set(null);
          router.navigate(['/intranet/login'], { queryParams: { expirada: 1 } });
        } else if (e.status === 401 && portal) {
          sesion.cliente.set(null);
          router.navigate(['/login'], { queryParams: { expirada: 1 } });
        } else if (e.status === 403 && e.error?.tipo === 'CAMBIO_PASSWORD') {
          router.navigate(['/intranet/cambiar-password']);
        }
      }
      return throwError(() => e);
    }),
  );
};
