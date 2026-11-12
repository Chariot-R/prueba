import { Component, computed, inject, signal } from '@angular/core';
import { NavigationEnd, Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { SesionService } from '../core/sesion.service';

interface ItemMenu {
  ruta: string;
  texto: string;
  roles: string[];
  exacto?: boolean;
}

/** Estructura de la intranet: menú lateral (según el rol) y contenido. */
@Component({
  selector: 'app-intranet-layout',
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  template: `
    <div class="barra-movil">
      <span class="marca"><span class="marca-icono">TI</span> TI SOLUTIONS</span>
      <button type="button" (click)="menuAbierto.set(!menuAbierto())">Menú</button>
    </div>
    <div class="app">
      <aside class="lateral" [class.abierto]="menuAbierto()">
        <a class="marca" [routerLink]="inicio()"><span class="marca-icono">TI</span> TI SOLUTIONS</a>
        @if (sesion.personal(); as s) {
          <div class="perfil">
            <span class="inicial">{{ s.nombre.charAt(0).toUpperCase() }}</span>
            <div><strong [title]="s.nombre">{{ s.nombre }}</strong><small>{{ s.rolEtiqueta }}</small></div>
          </div>
          <div class="menu-titulo">MENÚ</div>
          <nav class="menu">
            @for (item of menu(); track item.ruta) {
              <a [routerLink]="item.ruta" routerLinkActive="activo"
                 [routerLinkActiveOptions]="{ exact: !!item.exacto }">{{ item.texto }}</a>
            }
          </nav>
        }
        <div class="salir">
          <button type="button" (click)="salir()">Cerrar sesión</button>
        </div>
      </aside>
      <main class="contenido">
        <router-outlet />
      </main>
    </div>
  `,
})
export class IntranetLayout {
  protected readonly sesion = inject(SesionService);
  private readonly router = inject(Router);
  protected readonly menuAbierto = signal(false);

  private readonly items: ItemMenu[] = [
    { ruta: '/intranet/admin', texto: 'Dashboard', roles: ['ADMINISTRADOR'], exacto: true },
    { ruta: '/intranet/admin/usuarios', texto: 'Usuarios', roles: ['ADMINISTRADOR'] },
    { ruta: '/intranet/cotizaciones', texto: 'Cotizaciones', roles: ['ADMINISTRADOR', 'VENTAS'] },
    { ruta: '/intranet/tecnico', texto: 'Mi panel', roles: ['TECNICO'] },
    { ruta: '/intranet/cambiar-password', texto: 'Cambiar contraseña', roles: ['ADMINISTRADOR', 'VENTAS', 'TECNICO'] },
  ];

  protected readonly menu = computed(() => {
    const s = this.sesion.personal();
    if (!s) return [];
    if (s.debeCambiarPassword) return this.items.filter((i) => i.ruta === '/intranet/cambiar-password');
    return this.items.filter((i) => i.roles.includes(s.rol));
  });

  protected readonly inicio = computed(() => {
    const s = this.sesion.personal();
    return s ? SesionService.inicioPersonal(s) : '/intranet/login';
  });

  constructor() {
    // En celulares, el menú se cierra al navegar
    this.router.events.subscribe((e) => {
      if (e instanceof NavigationEnd) this.menuAbierto.set(false);
    });
  }

  protected async salir() {
    await this.sesion.salirPersonal();
    await this.router.navigate(['/intranet/login'], { queryParams: { salio: 1 } });
  }
}
