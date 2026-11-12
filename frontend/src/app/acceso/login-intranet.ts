import { Component, inject, input, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { errorApi } from '../core/errores';
import { SesionService } from '../core/sesion.service';
import { CampoClave } from './campo-clave';

/** Inicio de sesión del PERSONAL. No hay registro: las cuentas las crea el administrador. */
@Component({
  selector: 'app-login-intranet',
  imports: [FormsModule, RouterLink, CampoClave],
  host: { class: 'acceso-intranet' },
  template: `
    <main class="tarjeta-intranet">
      <div class="marca"><span class="marca-icono">TI</span> TI SOLUTIONS</div>
      <div class="etiqueta-zona">Intranet · acceso solo para personal</div>

      <h1>Acceso del personal</h1>
      <p class="sub">Administración, Ventas y Técnicos.</p>

      @if (error()) {
        <div class="alerta alerta-error" role="alert">{{ error() }}</div>
      } @else if (salio()) {
        <div class="alerta alerta-info" role="status">Cerraste sesión correctamente.</div>
      } @else if (expirada()) {
        <div class="alerta alerta-aviso" role="status">Tu sesión terminó. Vuelve a iniciar sesión.</div>
      }

      <form (ngSubmit)="entrar()" novalidate>
        <div class="campo">
          <label for="email">Correo corporativo</label>
          <input id="email" name="email" type="email" maxlength="120" autocomplete="username"
                 placeholder="usuario@tisolutions.pe" [(ngModel)]="email" />
        </div>
        <div class="campo">
          <label for="password">Contraseña</label>
          <app-campo-clave idCampo="password" [(valor)]="password" />
        </div>
        <button class="btn btn-primario btn-bloque" type="submit" [disabled]="enviando()">
          {{ enviando() ? 'Ingresando...' : 'Ingresar a la intranet' }}
        </button>
      </form>

      <p class="nota-pie">
        ¿Olvidaste tu contraseña o se bloqueó tu cuenta? Pide al administrador que la restablezca.
        Los accesos quedan registrados.
      </p>
      <p class="nota-pie"><a routerLink="/login">Ir al acceso de clientes</a></p>
    </main>
  `,
})
export class LoginIntranet {
  private readonly sesion = inject(SesionService);
  private readonly router = inject(Router);

  readonly salio = input<string>();
  readonly expirada = input<string>();

  protected email = signal('');
  protected password = signal('');
  protected error = signal('');
  protected enviando = signal(false);

  protected async entrar() {
    this.error.set('');
    if (!this.email().trim() || !this.password()) {
      this.error.set('Ingresa tu correo y tu contraseña.');
      return;
    }
    this.enviando.set(true);
    try {
      const s = await this.sesion.loginPersonal(this.email(), this.password());
      await this.router.navigateByUrl(SesionService.inicioPersonal(s));
    } catch (e) {
      this.error.set(errorApi(e).mensaje);
      this.password.set('');
    } finally {
      this.enviando.set(false);
    }
  }
}
