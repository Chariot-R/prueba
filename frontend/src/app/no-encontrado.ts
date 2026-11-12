import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-no-encontrado',
  imports: [RouterLink],
  host: { class: 'acceso-intranet' },
  template: `
    <main class="tarjeta-intranet">
      <div class="marca"><span class="marca-icono">TI</span> TI SOLUTIONS</div>
      <h1>Página no encontrada</h1>
      <p class="sub">La dirección no existe o fue movida.</p>
      <a class="btn btn-primario btn-bloque" routerLink="/">Ir al inicio</a>
    </main>
  `,
})
export class NoEncontrado {}
