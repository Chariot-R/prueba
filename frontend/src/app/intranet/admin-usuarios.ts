import { HttpClient } from '@angular/common/http';
import { Component, ElementRef, inject, signal, viewChild } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { firstValueFrom } from 'rxjs';
import { errorApi } from '../core/errores';
import { FechaHoraPipe } from '../core/formato';
import { ClaveTemporal, Rol, UsuarioFila } from '../core/modelos';

/** Cuentas del personal (módulo 4.4 del informe): crear, editar, bloquear, dar de baja y restablecer clave. */
@Component({
  selector: 'app-admin-usuarios',
  imports: [FormsModule, FechaHoraPipe],
  templateUrl: './admin-usuarios.html',
})
export class AdminUsuarios {
  private readonly http = inject(HttpClient);
  private readonly dialogo = viewChild<ElementRef<HTMLDialogElement>>('dlgEditar');

  protected usuarios = signal<UsuarioFila[]>([]);
  protected roles = signal<Rol[]>([]);
  protected error = signal('');
  protected aviso = signal('');
  protected clave = signal<ClaveTemporal | null>(null);
  protected copiado = signal(false);
  protected ocupado = signal(false);

  protected nuevo = { nombre: '', email: '', rolId: null as number | null };
  protected edicion = { id: 0, nombre: '', rolId: 0 as number, email: '' };
  protected errorEdicion = signal('');

  constructor() {
    this.cargar();
    this.http.get<Rol[]>('/api/intranet/admin/roles').subscribe((r) => this.roles.set(r));
  }

  protected cargar() {
    this.http.get<UsuarioFila[]>('/api/intranet/admin/usuarios').subscribe({
      next: (u) => this.usuarios.set(u),
      error: (e) => this.error.set(errorApi(e).mensaje),
    });
  }

  private mensajes(error = '', aviso = '') {
    this.error.set(error);
    this.aviso.set(aviso);
  }

  protected async crear() {
    this.mensajes();
    this.clave.set(null);
    this.ocupado.set(true);
    try {
      const r = await firstValueFrom(this.http.post<ClaveTemporal>('/api/intranet/admin/usuarios', this.nuevo));
      this.clave.set(r);
      this.copiado.set(false);
      this.nuevo = { nombre: '', email: '', rolId: null };
      this.cargar();
    } catch (e) {
      this.mensajes(errorApi(e).mensaje);
    } finally {
      this.ocupado.set(false);
    }
  }

  protected async restablecer(u: UsuarioFila) {
    if (!confirm(`¿Generar una contraseña temporal nueva para ${u.nombre}? La actual dejará de funcionar.`)) return;
    this.mensajes();
    try {
      const r = await firstValueFrom(
        this.http.post<ClaveTemporal>(`/api/intranet/admin/usuarios/${u.id}/restablecer`, {}),
      );
      this.clave.set(r);
      this.copiado.set(false);
      this.cargar();
    } catch (e) {
      this.mensajes(errorApi(e).mensaje);
    }
  }

  protected async cambiarEstado(u: UsuarioFila, estado: 'ACTIVO' | 'BLOQUEADO' | 'BAJA') {
    const textos = {
      ACTIVO: `¿Activar la cuenta de ${u.nombre}?`,
      BLOQUEADO: `¿Bloquear la cuenta de ${u.nombre}? No podrá ingresar hasta que la actives.`,
      BAJA: `¿Dar de baja la cuenta de ${u.nombre}? Ya no podrá ingresar.`,
    };
    if (!confirm(textos[estado])) return;
    this.mensajes();
    try {
      await firstValueFrom(this.http.put(`/api/intranet/admin/usuarios/${u.id}/estado`, { estado }));
      this.mensajes('', 'Estado actualizado.');
      this.cargar();
    } catch (e) {
      this.mensajes(errorApi(e).mensaje);
    }
  }

  protected abrirEdicion(u: UsuarioFila) {
    this.edicion = { id: u.id, nombre: u.nombre, rolId: u.rolId, email: u.email };
    this.errorEdicion.set('');
    this.dialogo()?.nativeElement.showModal();
  }

  protected cerrarEdicion() {
    this.dialogo()?.nativeElement.close();
  }

  protected async guardarEdicion() {
    this.errorEdicion.set('');
    try {
      await firstValueFrom(
        this.http.put(`/api/intranet/admin/usuarios/${this.edicion.id}`, {
          nombre: this.edicion.nombre,
          rolId: Number(this.edicion.rolId),
        }),
      );
      this.cerrarEdicion();
      this.mensajes('', 'Datos actualizados.');
      this.cargar();
    } catch (e) {
      this.errorEdicion.set(errorApi(e).mensaje);
    }
  }

  protected async copiar() {
    const c = this.clave();
    if (!c) return;
    try {
      await navigator.clipboard.writeText(c.temporal);
      this.copiado.set(true);
    } catch {
      // Sin permiso de portapapeles: el administrador puede seleccionarla a mano
    }
  }

  protected textoEstado(e: string) {
    return e === 'ACTIVO' ? 'Activo' : e === 'BLOQUEADO' ? 'Bloqueado' : 'De baja';
  }
}
