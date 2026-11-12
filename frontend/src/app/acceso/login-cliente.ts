import { Component, inject, input, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { errorApi } from '../core/errores';
import { SesionService } from '../core/sesion.service';
import { CampoClave } from './campo-clave';
import { PanelMarca } from './panel-marca';

/** Inicio de sesión de CLIENTES (persona natural o empresa). */
@Component({
  selector: 'app-login-cliente',
  imports: [FormsModule, RouterLink, CampoClave, PanelMarca],
  host: { class: 'acceso' },
  templateUrl: './login-cliente.html',
})
export class LoginCliente {
  private readonly sesion = inject(SesionService);
  private readonly router = inject(Router);

  /** Parámetros de la URL: ?salio=1, ?expirada=1 */
  readonly salio = input<string>();
  readonly expirada = input<string>();

  protected email = signal('');
  protected password = signal('');
  protected error = signal('');
  protected enviando = signal(false);
  /** Correo de la cuenta recién creada (llega desde el registro, fuera de la URL). */
  protected registrado = signal<string | null>(null);

  ngOnInit() {
    const correo = history.state?.registrado;
    if (typeof correo === 'string') {
      this.registrado.set(correo);
      this.email.set(correo);
    }
  }

  protected async entrar() {
    this.error.set('');
    if (!this.email().trim() || !this.password()) {
      this.error.set('Ingresa tu correo y tu contraseña.');
      return;
    }
    this.enviando.set(true);
    try {
      await this.sesion.loginCliente(this.email(), this.password());
      await this.router.navigateByUrl('/mi-cuenta');
    } catch (e) {
      this.error.set(errorApi(e).mensaje);
      this.password.set('');
    } finally {
      this.enviando.set(false);
    }
  }
}
