import { Component } from '@angular/core';

@Component({
  selector: 'app-tecnico-panel',
  template: `
    <header class="cabecera">
      <div>
        <h1>Panel técnico</h1>
        <div class="sub">Órdenes de servicio asignadas</div>
      </div>
    </header>
    <div class="pagina">
      <section class="tarjeta">
        <h2>Mis órdenes de servicio</h2>
        <p>
          Aquí verás las instalaciones y mantenimientos que te asignen, con su fecha, dirección y estado.
          Esta sección se completa con el módulo de Servicios técnicos (4.3).
        </p>
      </section>
    </div>
  `,
})
export class TecnicoPanel {}
