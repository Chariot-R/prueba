import { HttpClient } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { errorApi } from '../core/errores';
import { ClaseEstadoPipe, FechaPipe, SolesPipe } from '../core/formato';
import { MiCuenta } from '../core/modelos';
import { SesionService } from '../core/sesion.service';

/** "Mi cuenta" del cliente: sus datos, nueva solicitud de servicio, sus solicitudes y cotizaciones. */
@Component({
  selector: 'app-mi-cuenta',
  imports: [FormsModule, FechaPipe, SolesPipe, ClaseEstadoPipe],
  templateUrl: './mi-cuenta.html',
})
export class MiCuentaPagina {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);
  protected readonly sesion = inject(SesionService);

  protected datos = signal<MiCuenta | null>(null);
  protected error = signal('');
  protected aviso = signal('');
  protected descripcion = signal('');
  protected enviando = signal(false);

  constructor() {
    this.cargar();
  }

  private cargar() {
    this.http.get<MiCuenta>('/api/portal/mi-cuenta').subscribe({
      next: (d) => this.datos.set(d),
      error: (e) => this.error.set(errorApi(e).mensaje),
    });
  }

  protected async solicitar() {
    this.error.set('');
    this.aviso.set('');
    const d = this.descripcion().trim();
    if (d.length < 10) {
      this.error.set('Describe lo que necesitas (al menos 10 caracteres).');
      return;
    }
    this.enviando.set(true);
    try {
      await firstValueFrom(this.http.post('/api/portal/solicitudes', { descripcion: d }));
      this.descripcion.set('');
      this.aviso.set('Recibimos tu solicitud. Un asesor de ventas te contactará pronto.');
      this.cargar();
    } catch (e) {
      const err = errorApi(e);
      this.error.set(err.errores[0]?.mensaje ?? err.mensaje);
    } finally {
      this.enviando.set(false);
    }
  }

  protected async salir() {
    await this.sesion.salirCliente();
    await this.router.navigate(['/login'], { queryParams: { salio: 1 } });
  }

  protected textoEstadoSolicitud(e: string) {
    const t: Record<string, string> = {
      NUEVA: 'Nueva',
      EN_ATENCION: 'En atención',
      COTIZADA: 'Cotizada',
      ATENDIDA: 'Atendida',
      CERRADA: 'Cerrada',
    };
    return t[e] ?? e;
  }
}
