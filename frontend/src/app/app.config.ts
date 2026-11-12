import { ApplicationConfig, inject, provideAppInitializer, provideBrowserGlobalErrorListeners } from '@angular/core';
import { provideHttpClient, withInterceptors, withXsrfConfiguration } from '@angular/common/http';
import { provideRouter, withComponentInputBinding } from '@angular/router';
import { routes } from './app.routes';
import { erroresInterceptor } from './core/errores.interceptor';
import { SesionService } from './core/sesion.service';

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideRouter(routes, withComponentInputBinding()),
    provideHttpClient(
      // El backend entrega la cookie XSRF-TOKEN; Angular la devuelve en la cabecera X-XSRF-TOKEN
      // en cada POST/PUT/DELETE. Así otra página no puede enviar formularios en nombre del usuario.
      withXsrfConfiguration({ cookieName: 'XSRF-TOKEN', headerName: 'X-XSRF-TOKEN' }),
      withInterceptors([erroresInterceptor]),
    ),
    // Al abrir la app se pide la cookie CSRF antes de cualquier formulario
    provideAppInitializer(() => inject(SesionService).prepararCsrf()),
  ],
};
