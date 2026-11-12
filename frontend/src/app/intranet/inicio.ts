import { Component, inject } from '@angular/core';
import { Router } from '@angular/router';
import { SesionService } from '../core/sesion.service';

/** /intranet sin más: lleva a la pantalla de inicio según el rol. */
@Component({ selector: 'app-inicio-intranet', template: '' })
export class InicioIntranet {
  constructor() {
    const s = inject(SesionService).personal();
    inject(Router).navigateByUrl(s ? SesionService.inicioPersonal(s) : '/intranet/login', { replaceUrl: true });
  }
}
