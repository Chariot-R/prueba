import { Component, input, model, signal } from '@angular/core';

/** Campo de contraseña con botón para mostrarla y aviso de Bloq Mayús. */
@Component({
  selector: 'app-campo-clave',
  template: `
    <div class="con-boton">
      <input
        [id]="idCampo()"
        [type]="visible() ? 'text' : 'password'"
        [value]="valor()"
        (input)="valor.set($any($event.target).value)"
        (keyup)="revisarMayus($event)"
        (keydown)="revisarMayus($event)"
        (blur)="mayus.set(false)"
        [attr.autocomplete]="autocompletar()"
        [attr.placeholder]="ayuda()"
        [class.invalido]="invalido()"
        maxlength="64"
      />
      <button
        type="button"
        class="ver-pass"
        (click)="visible.set(!visible())"
        [attr.aria-pressed]="visible()"
        [attr.aria-label]="visible() ? 'Ocultar contraseña' : 'Mostrar contraseña'"
      >
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"
             stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
          @if (visible()) {
            <path d="M3 3l18 18M10.6 10.6a2 2 0 002.8 2.8M9.9 5.1A9.7 9.7 0 0112 5c6.5 0 10 7 10 7a17 17 0 01-3.2 4.2M6.6 6.6C3.9 8.4 2 12 2 12s3.5 7 10 7a9.6 9.6 0 005.4-1.6" />
          } @else {
            <path d="M2 12s3.5-7 10-7 10 7 10 7-3.5 7-10 7S2 12 2 12z" /><circle cx="12" cy="12" r="3" />
          }
        </svg>
      </button>
    </div>
    @if (mayus()) {
      <div class="mayus">Bloq Mayús está activado.</div>
    }
  `,
})
export class CampoClave {
  readonly valor = model('');
  readonly idCampo = input('password');
  readonly autocompletar = input('current-password');
  readonly ayuda = input('');
  readonly invalido = input(false);

  protected readonly visible = signal(false);
  protected readonly mayus = signal(false);

  protected revisarMayus(e: KeyboardEvent) {
    if (e.getModifierState) this.mayus.set(e.getModifierState('CapsLock'));
  }
}
