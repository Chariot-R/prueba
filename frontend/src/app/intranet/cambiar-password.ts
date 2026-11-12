import { Component, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { CampoClave } from '../acceso/campo-clave';
import { errorApi } from '../core/errores';
import { SesionService } from '../core/sesion.service';

@Component({
  selector: 'app-cambiar-password',
  imports: [CampoClave],
  template: `
    <header class="cabecera">
      <div>
        <h1>Cambiar contraseña</h1>
        <div class="sub">Elige una contraseña que solo tú conozcas.</div>
      </div>
    </header>
    <div class="pagina">
      @if (obligatorio()) {
        <div class="alerta alerta-aviso">
          Estás usando una contraseña temporal. Por seguridad debes cambiarla antes de continuar.
        </div>
      }
      @if (error()) {
        <div class="alerta alerta-error" role="alert">{{ error() }}</div>
      }
      @if (listo()) {
        <div class="alerta alerta-ok" role="status">Tu contraseña se cambió correctamente.</div>
      }

      <form class="tarjeta" style="max-width: 460px" (submit)="$event.preventDefault(); guardar()">
        <div class="campo">
          <label for="actual">Contraseña actual</label>
          <app-campo-clave idCampo="actual" [(valor)]="actual" />
        </div>
        <div class="campo">
          <label for="nueva">Nueva contraseña</label>
          <app-campo-clave idCampo="nueva" autocompletar="new-password" [(valor)]="nueva" />
          <div class="ayuda">Mínimo 10 caracteres, con mayúsculas, minúsculas y números.</div>
        </div>
        <div class="campo">
          <label for="confirmar">Confirmar nueva contraseña</label>
          <app-campo-clave idCampo="confirmar" autocompletar="new-password" [(valor)]="confirmar" />
        </div>
        <button class="btn btn-primario" type="submit" [disabled]="enviando()">
          {{ enviando() ? 'Guardando...' : 'Guardar contraseña' }}
        </button>
      </form>
    </div>
  `,
})
export class CambiarPassword {
  private readonly sesion = inject(SesionService);
  private readonly router = inject(Router);

  protected actual = signal('');
  protected nueva = signal('');
  protected confirmar = signal('');
  protected error = signal('');
  protected listo = signal(false);
  protected enviando = signal(false);
  protected obligatorio = computed(() => !!this.sesion.personal()?.debeCambiarPassword);

  protected async guardar() {
    this.error.set('');
    this.listo.set(false);
    if (!this.actual() || !this.nueva()) {
      this.error.set('Completa todos los campos.');
      return;
    }
    if (this.nueva() !== this.confirmar()) {
      this.error.set('Las contraseñas nuevas no coinciden.');
      return;
    }
    const eraObligatorio = this.obligatorio();
    this.enviando.set(true);
    try {
      const s = await this.sesion.cambiarPassword(this.actual(), this.nueva(), this.confirmar());
      this.actual.set('');
      this.nueva.set('');
      this.confirmar.set('');
      if (eraObligatorio) {
        await this.router.navigateByUrl(SesionService.inicioPersonal(s));
      } else {
        this.listo.set(true);
      }
    } catch (e) {
      this.error.set(errorApi(e).mensaje);
    } finally {
      this.enviando.set(false);
    }
  }
}
