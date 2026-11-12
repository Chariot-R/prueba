import { Routes } from '@angular/router';
import {
  avisarCambiosPendientes,
  invitadoCliente,
  invitadoPersonal,
  soloCliente,
  soloPersonal,
} from './core/guardias';

/**
 * Dos zonas separadas:
 *  - Portal de clientes: /login, /registro, /mi-cuenta
 *  - Intranet del personal: /intranet/...
 */
export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'login' },

  // ----------------------------- Clientes -----------------------------
  {
    path: 'login',
    canActivate: [invitadoCliente],
    title: 'Iniciar sesión · TI Solutions',
    loadComponent: () => import('./acceso/login-cliente').then((m) => m.LoginCliente),
  },
  {
    path: 'registro',
    canActivate: [invitadoCliente],
    title: 'Crear cuenta · TI Solutions',
    loadComponent: () => import('./acceso/registro').then((m) => m.Registro),
  },
  {
    path: 'terminos',
    title: 'Términos y condiciones · TI Solutions',
    loadComponent: () => import('./acceso/terminos').then((m) => m.Terminos),
  },
  {
    path: 'mi-cuenta',
    canActivate: [soloCliente],
    title: 'Mi cuenta · TI Solutions',
    loadComponent: () => import('./portal/mi-cuenta').then((m) => m.MiCuentaPagina),
  },

  // ----------------------------- Personal -----------------------------
  {
    path: 'intranet/login',
    canActivate: [invitadoPersonal],
    title: 'Intranet · TI Solutions',
    loadComponent: () => import('./acceso/login-intranet').then((m) => m.LoginIntranet),
  },
  {
    path: 'intranet',
    canActivate: [soloPersonal()],
    loadComponent: () => import('./intranet/intranet-layout').then((m) => m.IntranetLayout),
    children: [
      { path: '', pathMatch: 'full', loadComponent: () => import('./intranet/inicio').then((m) => m.InicioIntranet) },
      {
        path: 'cambiar-password',
        title: 'Cambiar contraseña · TI Solutions',
        loadComponent: () => import('./intranet/cambiar-password').then((m) => m.CambiarPassword),
      },
      {
        path: 'admin',
        canActivate: [soloPersonal('ADMINISTRADOR')],
        title: 'Dashboard · TI Solutions',
        loadComponent: () => import('./intranet/admin-resumen').then((m) => m.AdminResumen),
      },
      {
        path: 'admin/usuarios',
        canActivate: [soloPersonal('ADMINISTRADOR')],
        title: 'Usuarios · TI Solutions',
        loadComponent: () => import('./intranet/admin-usuarios').then((m) => m.AdminUsuarios),
      },
      {
        path: 'tecnico',
        canActivate: [soloPersonal('TECNICO')],
        title: 'Panel técnico · TI Solutions',
        loadComponent: () => import('./intranet/tecnico-panel').then((m) => m.TecnicoPanel),
      },
      {
        path: 'cotizaciones',
        canActivate: [soloPersonal('VENTAS', 'ADMINISTRADOR')],
        children: [
          {
            path: '',
            title: 'Cotizaciones · TI Solutions',
            loadComponent: () => import('./cotizaciones/cotizaciones-lista').then((m) => m.CotizacionesLista),
          },
          {
            path: 'nueva',
            title: 'Nueva cotización · TI Solutions',
            canDeactivate: [avisarCambiosPendientes],
            loadComponent: () => import('./cotizaciones/cotizacion-form').then((m) => m.CotizacionForm),
          },
          {
            path: ':id/editar',
            title: 'Editar cotización · TI Solutions',
            canDeactivate: [avisarCambiosPendientes],
            loadComponent: () => import('./cotizaciones/cotizacion-form').then((m) => m.CotizacionForm),
          },
          {
            path: ':id',
            title: 'Cotización · TI Solutions',
            loadComponent: () => import('./cotizaciones/cotizacion-detalle').then((m) => m.CotizacionDetalle),
          },
        ],
      },
    ],
  },

  {
    path: '**',
    title: 'Página no encontrada · TI Solutions',
    loadComponent: () => import('./no-encontrado').then((m) => m.NoEncontrado),
  },
];
