import { HttpClient } from '@angular/common/http';
import { Component, ElementRef, OnInit, inject, input, signal, viewChild } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { errorApi } from '../core/errores';
import { ClaseEstadoPipe, FechaHoraPipe, FechaPipe, NumeroPipe, SolesPipe } from '../core/formato';
import { CotizacionResponse, OpcionEstado } from '../core/modelos';

/** Vista de una cotización: datos, detalle, totales y acciones (emitir, cambiar estado, versión, eliminar). */
@Component({
  selector: 'app-cotizacion-detalle',
  imports: [FormsModule, RouterLink, SolesPipe, NumeroPipe, FechaPipe, FechaHoraPipe, ClaseEstadoPipe],
  templateUrl: './cotizacion-detalle.html',
})
export class CotizacionDetalle implements OnInit {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);
  private readonly dlgMotivo = viewChild<ElementRef<HTMLDialogElement>>('dlgMotivo');

  readonly id = input.required<string>();
  readonly guardado = input<string>();

  protected c = signal<CotizacionResponse | null>(null);
  protected error = signal('');
  protected aviso = signal('');
  protected ocupado = signal(false);

  protected pendiente = signal<OpcionEstado | null>(null);
  protected motivo = signal('');
  protected errorMotivo = signal('');

  ngOnInit() {
    if (this.guardado()) this.aviso.set('Cotización guardada correctamente.');
    this.cargar();
  }

  private async cargar() {
    try {
      this.c.set(await firstValueFrom(this.http.get<CotizacionResponse>(`/api/intranet/cotizaciones/${this.id()}`)));
    } catch (e) {
      this.error.set(errorApi(e).mensaje);
    }
  }

  protected textoBoton(op: OpcionEstado) {
    if (op.valor === 'EMITIDA') return 'Emitir';
    if (op.valor === 'ANULADA') return 'Anular';
    return 'Marcar: ' + op.etiqueta;
  }

  protected claseBoton(op: OpcionEstado) {
    if (op.valor === 'EMITIDA') return 'btn btn-primario';
    if (op.valor === 'ANULADA') return 'btn btn-peligro';
    return 'btn';
  }

  /** El precio del proveedor cambió después de cotizar (la línea conserva el original). */
  protected precioCambiado(costoActual: number | null, costo: number | null) {
    return costoActual != null && costo != null && Number(costoActual) !== Number(costo);
  }

  protected elegirEstado(op: OpcionEstado) {
    if (op.requiereMotivo) {
      this.pendiente.set(op);
      this.motivo.set('');
      this.errorMotivo.set('');
      this.dlgMotivo()?.nativeElement.showModal();
      return;
    }
    if (op.valor === 'EMITIDA' && !confirm('¿Emitir la cotización? Los precios quedarán fijos y ya no podrás editar el borrador.')) {
      return;
    }
    this.enviarEstado(op.valor, null);
  }

  protected confirmarMotivo() {
    const m = this.motivo().trim();
    if (!m) {
      this.errorMotivo.set('Indica el motivo.');
      return;
    }
    this.dlgMotivo()?.nativeElement.close();
    this.enviarEstado(this.pendiente()!.valor, m);
  }

  protected cerrarMotivo() {
    this.dlgMotivo()?.nativeElement.close();
  }

  private async enviarEstado(estado: string, motivo: string | null) {
    const c = this.c();
    if (!c) return;
    this.mensajes();
    this.ocupado.set(true);
    try {
      const r = await firstValueFrom(
        this.http.post<CotizacionResponse>(`/api/intranet/cotizaciones/${c.id}/estado`, {
          estado,
          motivo,
          filaVersion: c.filaVersion,
        }),
      );
      this.c.set(r);
      this.aviso.set('Estado actualizado: ' + r.estadoEtiqueta + '.');
    } catch (e) {
      this.mostrarError(e);
    } finally {
      this.ocupado.set(false);
    }
  }

  protected async nuevaVersion() {
    const c = this.c();
    if (!c || !confirm('Se creará un borrador copiando esta cotización. ¿Continuar?')) return;
    this.mensajes();
    this.ocupado.set(true);
    try {
      const r = await firstValueFrom(this.http.post<CotizacionResponse>(`/api/intranet/cotizaciones/${c.id}/nueva-version`, {}));
      await this.router.navigate(['/intranet/cotizaciones', r.id, 'editar']);
    } catch (e) {
      this.mostrarError(e);
    } finally {
      this.ocupado.set(false);
    }
  }

  protected async eliminar() {
    const c = this.c();
    if (!c || !confirm('Se eliminará el borrador y todas sus líneas. ¿Continuar?')) return;
    this.mensajes();
    this.ocupado.set(true);
    try {
      await firstValueFrom(this.http.delete(`/api/intranet/cotizaciones/${c.id}`));
      await this.router.navigate(['/intranet/cotizaciones']);
    } catch (e) {
      this.mostrarError(e);
    } finally {
      this.ocupado.set(false);
    }
  }

  protected imprimir() {
    window.print();
  }

  private mensajes() {
    this.error.set('');
    this.aviso.set('');
  }

  private mostrarError(e: unknown) {
    const err = errorApi(e);
    const detalle = err.tipo === 'VALIDACION' && err.errores.length ? err.errores.map((x) => x.mensaje).join('. ') : err.mensaje;
    this.error.set(detalle);
  }
}
