import { HttpClient } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { errorApi } from '../core/errores';
import { FechaHoraPipe } from '../core/formato';
import { Acceso, ResumenAdmin } from '../core/modelos';

/** Dashboard del administrador: cifras generales y últimos accesos (bitácora). */
@Component({
  selector: 'app-admin-resumen',
  imports: [RouterLink, FechaHoraPipe],
  template: `
    <header class="cabecera">
      <div>
        <h1>Dashboard</h1>
        <div class="sub">Resumen general y seguridad de accesos</div>
      </div>
      <div class="acciones">
        <button class="btn" type="button" (click)="cargar()">Actualizar</button>
        <a class="btn btn-primario" routerLink="/intranet/admin/usuarios">Gestionar usuarios</a>
      </div>
    </header>
    <div class="pagina">
      @if (error()) {
        <div class="alerta alerta-error">{{ error() }}</div>
      }
      @if (datos(); as d) {
        <div class="kpis">
          <div class="kpi"><small>Personal activo</small><strong>{{ d.personalActivo }}</strong></div>
          <div class="kpi"><small>Clientes con cuenta web</small><strong>{{ d.clientesWeb }}</strong></div>
          <div class="kpi"><small>Cotizaciones registradas</small><strong>{{ d.cotizaciones }}</strong></div>
          <div class="kpi" [class.alerta-kpi]="d.fallidos24h > 0">
            <small>Intentos fallidos (24 h)</small><strong>{{ d.fallidos24h }}</strong>
          </div>
        </div>

        <section class="tarjeta">
          <h2>Últimos accesos <span class="nota">Intranet y portal de clientes</span></h2>
          <div class="tabla-envoltura">
            <table>
              <thead>
                <tr><th>Fecha</th><th>Zona</th><th>Correo</th><th>Resultado</th><th>IP</th></tr>
              </thead>
              <tbody>
                @for (a of d.accesos; track $index) {
                  <tr>
                    <td>{{ a.fecha | fechaHora }}</td>
                    <td>{{ a.tipoCuenta === 'PERSONAL' ? 'Intranet' : 'Clientes' }}</td>
                    <td>{{ a.email }}</td>
                    <td><span [class]="'estado ' + claseResultado(a)">{{ textoResultado(a) }}</span></td>
                    <td>{{ a.ip }}</td>
                  </tr>
                } @empty {
                  <tr><td colspan="5" class="vacio">Todavía no hay accesos registrados.</td></tr>
                }
              </tbody>
            </table>
          </div>
        </section>
      } @else if (!error()) {
        <p class="ayuda">Cargando...</p>
      }
    </div>
  `,
})
export class AdminResumen {
  private readonly http = inject(HttpClient);
  protected datos = signal<ResumenAdmin | null>(null);
  protected error = signal('');

  constructor() {
    this.cargar();
  }

  protected cargar() {
    this.error.set('');
    this.http.get<ResumenAdmin>('/api/intranet/admin/resumen').subscribe({
      next: (d) => this.datos.set(d),
      error: (e) => this.error.set(errorApi(e).mensaje),
    });
  }

  protected textoResultado(a: Acceso): string {
    if (a.exito) return 'Correcto';
    switch (a.motivo) {
      case 'SE_BLOQUEO':
        return 'Fallido, se bloqueó';
      case 'BLOQUEADA':
        return 'Cuenta bloqueada';
      case 'DESHABILITADA':
        return 'Cuenta inactiva';
      default:
        return 'Fallido';
    }
  }

  protected claseResultado(a: Acceso): string {
    if (a.exito) return 'estado-ok';
    return a.motivo === 'SE_BLOQUEO' || a.motivo === 'BLOQUEADA' ? 'estado-aviso' : 'estado-error';
  }
}
