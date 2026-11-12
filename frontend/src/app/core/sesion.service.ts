import { HttpClient } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { RegistroCliente, Sesion } from './modelos';

/**
 * Estado de las DOS sesiones posibles en este navegador:
 *  - personal: alguien de la intranet (Administrador, Ventas, Técnico)
 *  - cliente:  alguien del portal de clientes
 * La sesión real vive en el servidor; aquí solo se guarda quién es, para mostrar el menú.
 * undefined = todavía no se consultó al servidor; null = no hay sesión.
 */
@Injectable({ providedIn: 'root' })
export class SesionService {
  private readonly http = inject(HttpClient);

  readonly personal = signal<Sesion | null | undefined>(undefined);
  readonly cliente = signal<Sesion | null | undefined>(undefined);

  /** Primera llamada al abrir la app: deja lista la cookie XSRF-TOKEN. */
  async prepararCsrf(): Promise<void> {
    try {
      await this.cargarCliente();
    } catch {
      // Si el backend está apagado, cada pantalla mostrará su propio aviso.
    }
  }

  async cargarPersonal(forzar = false): Promise<Sesion | null> {
    if (!forzar && this.personal() !== undefined) return this.personal() ?? null;
    const s = await firstValueFrom(this.http.get<Sesion | null>('/api/intranet/auth/yo'));
    this.personal.set(s ?? null);
    return s ?? null;
  }

  async cargarCliente(forzar = false): Promise<Sesion | null> {
    if (!forzar && this.cliente() !== undefined) return this.cliente() ?? null;
    const s = await firstValueFrom(this.http.get<Sesion | null>('/api/portal/auth/yo'));
    this.cliente.set(s ?? null);
    return s ?? null;
  }

  async loginPersonal(email: string, password: string): Promise<Sesion> {
    const s = await firstValueFrom(this.http.post<Sesion>('/api/intranet/auth/login', { email, password }));
    this.personal.set(s);
    return s;
  }

  async loginCliente(email: string, password: string): Promise<Sesion> {
    const s = await firstValueFrom(this.http.post<Sesion>('/api/portal/auth/login', { email, password }));
    this.cliente.set(s);
    return s;
  }

  async salirPersonal(): Promise<void> {
    try {
      await firstValueFrom(this.http.post('/api/intranet/auth/logout', {}));
    } finally {
      this.personal.set(null);
      await this.renovarCsrf();
    }
  }

  async salirCliente(): Promise<void> {
    try {
      await firstValueFrom(this.http.post('/api/portal/auth/logout', {}));
    } finally {
      this.cliente.set(null);
      await this.renovarCsrf();
    }
  }

  /** Al cerrar sesión Spring borra el token CSRF; un GET cualquiera entrega uno nuevo. */
  private async renovarCsrf() {
    try {
      await firstValueFrom(this.http.get('/api/portal/auth/yo'));
    } catch {
      // sin conexión: el interceptor lo reintentará en la próxima petición
    }
  }

  async cambiarPassword(actual: string, nueva: string, confirmar: string): Promise<Sesion> {
    const s = await firstValueFrom(
      this.http.post<Sesion>('/api/intranet/auth/cambiar-password', { actual, nueva, confirmar }),
    );
    this.personal.set(s);
    return s;
  }

  registrar(datos: RegistroCliente): Promise<unknown> {
    return firstValueFrom(this.http.post('/api/portal/auth/registro', datos));
  }

  /** Pantalla de inicio de cada trabajador según su rol. */
  static inicioPersonal(s: Sesion): string {
    if (s.debeCambiarPassword) return '/intranet/cambiar-password';
    switch (s.rol) {
      case 'ADMINISTRADOR':
        return '/intranet/admin';
      case 'VENTAS':
        return '/intranet/cotizaciones';
      case 'TECNICO':
        return '/intranet/tecnico';
      default:
        return '/intranet/login';
    }
  }
}
