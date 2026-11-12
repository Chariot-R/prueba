import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

/** Panel izquierdo de las páginas de clientes (se oculta en celulares). */
@Component({
  selector: 'app-panel-marca',
  imports: [RouterLink],
  host: { class: 'acceso-lado' },
  template: `
    <a class="marca" routerLink="/"><span class="marca-icono">TI</span> TI SOLUTIONS</a>
    <div>
      <h2>Instalación y mantenimiento de sistemas tecnológicos</h2>
      <p>
        Solicita nuestros servicios, revisa tus cotizaciones y sigue el estado de tus pedidos
        desde un solo lugar.
      </p>
      <ul>
        <li>Solicitudes de servicio en línea.</li>
        <li>Cotizaciones disponibles en tu cuenta.</li>
        <li>Atención a personas naturales y empresas.</li>
      </ul>
    </div>
    <small>TI Solutions</small>
  `,
})
export class PanelMarca {}
