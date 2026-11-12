import { HttpClient } from '@angular/common/http';
import { Component, HostListener, OnInit, inject, input, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import * as C from '../core/calculo';
import { errorApi } from '../core/errores';
import { ConCambiosPendientes } from '../core/guardias';
import {
  ConfigCotizacion,
  CotizacionResponse,
  ErrorCampo,
  LineaResponse,
  ProductoOpcion,
  SolicitudOpcion,
  TipoItem,
} from '../core/modelos';

/** Una fila del detalle mientras se edita (los números se guardan como texto, tal como se escriben). */
interface Linea {
  uid: number;
  id: number | null;
  tipoItem: TipoItem;
  productoId: number | null;
  proveedor: string | null;
  stock: number | null;
  descripcion: string;
  unidad: string;
  cantidad: string;
  costoUnitario: string;
  margenPct: string;
  precioUnitario: string;
  margenManual: boolean;
  costoActual: number | null;
  unidadEntera: boolean;
  sugerencias: ProductoOpcion[] | null;
  marcada: number;
}

const ETIQUETA_TIPO: Record<TipoItem, string> = { PRODUCTO: 'Producto', MATERIAL: 'Material', MANO_OBRA: 'Mano de obra' };
const CAMPO_VISIBLE: Record<string, string> = { productoId: 'descripcion', id: 'descripcion', tipoItem: 'descripcion' };

/**
 * Formulario maestro-detalle de cotización (nueva o edición de un borrador).
 * - Recalcula importes y totales en vivo (vista previa; el servidor recalcula al guardar).
 * - Valida en el navegador y envía cabecera + todas las líneas en UNA petición (una transacción).
 * - Muestra los errores del servidor sobre el campo o la fila exacta.
 */
@Component({
  selector: 'app-cotizacion-form',
  imports: [FormsModule],
  templateUrl: './cotizacion-form.html',
})
export class CotizacionForm implements OnInit, ConCambiosPendientes {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);

  /** :id de la ruta (solo al editar). */
  readonly id = input<string>();

  protected readonly etiquetaTipo = ETIQUETA_TIPO;
  protected readonly C = C;

  protected config = signal<ConfigCotizacion | null>(null);
  protected solicitudes = signal<SolicitudOpcion[]>([]);
  protected lineas = signal<Linea[]>([]);
  protected tasaIgvBp = signal(1800);
  protected titulo = signal('Nueva cotización');
  protected guardando = signal(false);
  protected cargando = signal(true);

  protected errorGeneral = signal('');
  protected resumenErrores = signal<string[]>([]);
  protected erroresCab = signal<Record<string, string>>({});
  protected erroresLinea = signal<Record<number, Record<string, string>>>({});

  protected cab = {
    clienteId: null as number | null,
    solicitudId: null as number | null,
    margenAplicado: '30',
    diasValidez: '15',
    observaciones: '',
  };

  private filaVersion: number | null = null;
  private sucio = false;
  private contadorUid = 0;
  private temporizador: ReturnType<typeof setTimeout> | undefined;

  async ngOnInit() {
    try {
      const config = await firstValueFrom(this.http.get<ConfigCotizacion>('/api/intranet/cotizaciones/config'));
      this.config.set(config);
      this.tasaIgvBp.set(Math.round(Number(config.tasaIgv) * 10000));
      const id = this.id();
      if (id) {
        const c = await firstValueFrom(this.http.get<CotizacionResponse>(`/api/intranet/cotizaciones/${id}`));
        if (!c.editable) {
          await this.router.navigate(['/intranet/cotizaciones', id], { replaceUrl: true });
          return;
        }
        this.cargarDesdeServidor(c);
        await this.cargarSolicitudes(c.clienteId, c.solicitudId);
      } else {
        this.cab.margenAplicado = String(Number(config.margenDefecto));
      }
    } catch (e) {
      this.errorGeneral.set(errorApi(e).mensaje);
    } finally {
      this.cargando.set(false);
    }
  }

  // ------------------------------------------------------------------ modelo de línea

  private nuevaLinea(tipo: TipoItem): Linea {
    return {
      uid: ++this.contadorUid, id: null, tipoItem: tipo, productoId: null, proveedor: null, stock: null,
      descripcion: '', unidad: tipo === 'MANO_OBRA' ? 'SERV' : '', cantidad: '1', costoUnitario: '',
      margenPct: tipo === 'MANO_OBRA' ? '' : this.cab.margenAplicado, precioUnitario: '', margenManual: false,
      costoActual: null, unidadEntera: false, sugerencias: null, marcada: -1,
    };
  }

  private desdeServidor(l: LineaResponse, margenCabecera: number): Linea {
    const txt = (n: number | null) => (n == null ? '' : String(Number(n)));
    return {
      uid: ++this.contadorUid, id: l.id, tipoItem: l.tipoItem, productoId: l.productoId, proveedor: l.proveedorNombre,
      stock: null, descripcion: l.descripcion ?? '', unidad: l.unidad ?? '', cantidad: txt(l.cantidad),
      costoUnitario: l.costoUnitario == null ? '' : Number(l.costoUnitario).toFixed(2),
      margenPct: txt(l.margenPct),
      precioUnitario: l.precioUnitario == null ? '' : Number(l.precioUnitario).toFixed(2),
      margenManual: l.margenPct != null && Number(l.margenPct) !== Number(margenCabecera),
      costoActual: l.costoActualProveedor == null ? null : C.aCentesimas(Number(l.costoActualProveedor).toFixed(2)),
      unidadEntera: (l.unidad ?? '').toUpperCase() === 'UND' && l.productoId != null,
      sugerencias: null, marcada: -1,
    };
  }

  private cargarDesdeServidor(c: CotizacionResponse) {
    this.titulo.set('Editar cotización ' + c.codigoCompleto);
    this.filaVersion = c.filaVersion;
    this.tasaIgvBp.set(Math.round(Number(c.tasaIgv) * 10000));
    this.cab = {
      clienteId: c.clienteId,
      solicitudId: c.solicitudId,
      margenAplicado: String(Number(c.margenAplicado)),
      diasValidez: String(c.diasValidez),
      observaciones: c.observaciones ?? '',
    };
    this.lineas.set(c.lineas.map((l) => this.desdeServidor(l, c.margenAplicado)));
  }

  protected tieneProducto(l: Linea) {
    return l.productoId != null;
  }

  protected usaCatalogo(l: Linea) {
    return l.tipoItem === 'PRODUCTO' || l.tipoItem === 'MATERIAL';
  }

  protected unidadBloqueada(l: Linea) {
    return l.tipoItem === 'PRODUCTO' || (this.usaCatalogo(l) && this.tieneProducto(l));
  }

  protected costoBloqueado(l: Linea) {
    return l.tipoItem !== 'MATERIAL' || this.tieneProducto(l);
  }

  protected calcular(l: Linea): { precio: number | null; importe: number | null } {
    const cant = C.aCentesimas(l.cantidad);
    let precio: number | null;
    if (l.tipoItem === 'MANO_OBRA') {
      precio = C.aCentesimas(l.precioUnitario);
    } else {
      const costo = C.aCentesimas(l.costoUnitario);
      const margen = C.aCentesimas(l.margenPct);
      precio = costo != null && margen != null ? C.precioVenta(costo, margen) : null;
    }
    return { precio, importe: cant != null && precio != null ? C.importe(cant, precio) : null };
  }

  protected totales() {
    const importes = this.lineas()
      .map((l) => this.calcular(l).importe)
      .filter((x): x is number => x != null);
    return C.totales(importes, this.tasaIgvBp());
  }

  /** Texto de ayuda bajo la descripción: proveedor, stock y avisos de precio. */
  protected meta(l: Linea): { texto: string; aviso: boolean } {
    const partes: string[] = [];
    let aviso = false;
    if (this.tieneProducto(l)) {
      if (l.proveedor) partes.push('Proveedor: ' + l.proveedor);
      if (l.stock != null) {
        partes.push('Stock: ' + l.stock);
        if (Number(l.cantidad) > l.stock) {
          partes.push('cantidad mayor al stock del proveedor');
          aviso = true;
        }
      }
      const costo = C.aCentesimas(l.costoUnitario);
      if (l.costoActual != null && costo != null && l.costoActual !== costo) {
        partes.push('precio actual del proveedor ' + C.solesCent(l.costoActual) + ' (se mantiene el original)');
        aviso = true;
      }
    } else if (l.tipoItem === 'MATERIAL' && l.descripcion) {
      partes.push('Material fuera de catálogo: ingrese su costo');
    }
    return { texto: partes.join(' · '), aviso };
  }

  // ------------------------------------------------------------------ eventos del detalle

  protected agregar(tipo: TipoItem) {
    const l = this.nuevaLinea(tipo);
    this.lineas.update((x) => [...x, l]);
    this.sucio = true;
    this.limpiarErrores();
    setTimeout(() => document.getElementById('desc-' + l.uid)?.focus());
  }

  protected quitar(l: Linea) {
    this.lineas.update((x) => x.filter((y) => y !== l));
    this.sucio = true;
    this.erroresLinea.set({});
  }

  protected cambiar(l: Linea, campo: 'descripcion' | 'unidad' | 'cantidad' | 'costoUnitario' | 'margenPct' | 'precioUnitario', valor: string) {
    this.sucio = true;
    l[campo] = valor;
    this.quitarErrorLinea(l, campo);
    if (campo === 'margenPct') l.margenManual = true;
    if (campo === 'descripcion' && this.usaCatalogo(l)) {
      if (this.tieneProducto(l)) {
        // Cambió el texto de un producto ya elegido: se desvincula del catálogo
        Object.assign(l, { productoId: null, proveedor: null, stock: null, costoActual: null, unidadEntera: false, costoUnitario: '', unidad: '' });
      }
      this.buscarProductos(l, valor);
    }
  }

  // ------------------------------------------------------------------ autocompletado de productos

  private buscarProductos(l: Linea, texto: string) {
    clearTimeout(this.temporizador);
    if (!texto || texto.trim().length < 2) {
      l.sugerencias = null;
      return;
    }
    this.temporizador = setTimeout(() => {
      this.http
        .get<ProductoOpcion[]>('/api/intranet/catalogo/productos', { params: { q: texto.trim() } })
        .subscribe({
          next: (lista) => {
            if (document.activeElement?.id !== 'desc-' + l.uid) return; // el usuario ya salió del campo
            l.sugerencias = lista;
            l.marcada = -1;
            this.lineas.update((x) => [...x]);
          },
          error: () => {
            /* la búsqueda es auxiliar; si falla no se interrumpe al usuario */
          },
        });
    }, 250);
  }

  protected teclas(l: Linea, e: KeyboardEvent) {
    const lista = l.sugerencias;
    if (!lista || !lista.length) return;
    if (e.key === 'ArrowDown' || e.key === 'ArrowUp') {
      e.preventDefault();
      l.marcada = e.key === 'ArrowDown' ? Math.min(l.marcada + 1, lista.length - 1) : Math.max(l.marcada - 1, 0);
    } else if (e.key === 'Enter' && l.marcada >= 0) {
      e.preventDefault();
      this.elegir(l, lista[l.marcada]);
    } else if (e.key === 'Escape') {
      l.sugerencias = null;
    }
  }

  protected cerrarSugerencias(l: Linea) {
    setTimeout(() => {
      l.sugerencias = null;
      this.lineas.update((x) => [...x]);
    }, 150);
  }

  protected elegir(l: Linea, p: ProductoOpcion) {
    Object.assign(l, {
      productoId: p.id, descripcion: p.nombre, proveedor: p.proveedor, stock: p.stock, unidad: p.unidad,
      unidadEntera: (p.unidad ?? '').toUpperCase() === 'UND',
      costoUnitario: Number(p.precio).toFixed(2), // foto del precio actual del proveedor
      costoActual: null, sugerencias: null,
    });
    this.sucio = true;
    this.quitarErrorLinea(l, 'descripcion');
    this.lineas.update((x) => [...x]);
    setTimeout(() => document.getElementById('cant-' + l.uid)?.focus());
  }

  // ------------------------------------------------------------------ cabecera

  protected cambioMargen(valor: string) {
    this.sucio = true;
    this.cab.margenAplicado = valor;
    this.quitarErrorCab('margenAplicado');
    for (const l of this.lineas()) {
      if (l.tipoItem !== 'MANO_OBRA' && !l.margenManual) l.margenPct = valor;
    }
  }

  protected cambioCliente(id: number | null) {
    this.sucio = true;
    this.cab.clienteId = id;
    this.cab.solicitudId = null;
    this.quitarErrorCab('clienteId');
    this.cargarSolicitudes(id, null);
  }

  protected marcarSucio(campo: string) {
    this.sucio = true;
    this.quitarErrorCab(campo);
  }

  private async cargarSolicitudes(clienteId: number | null, seleccionada: number | null) {
    this.solicitudes.set([]);
    if (!clienteId) return;
    try {
      const lista = await firstValueFrom(
        this.http.get<SolicitudOpcion[]>(`/api/intranet/catalogo/clientes/${clienteId}/solicitudes`),
      );
      this.solicitudes.set(lista);
      this.cab.solicitudId = seleccionada;
    } catch {
      this.errorGeneral.set('No se pudieron cargar las solicitudes del cliente.');
    }
  }

  // ------------------------------------------------------------------ validación y errores

  private validar(): ErrorCampo[] {
    const errores: ErrorCampo[] = [];
    const add = (campo: string, mensaje: string) => errores.push({ campo, mensaje });
    const margen = C.aCentesimas(this.cab.margenAplicado);
    const dias = Number(this.cab.diasValidez);

    if (!this.cab.clienteId) add('clienteId', 'Seleccione un cliente');
    if (margen == null || margen > 10000) add('margenAplicado', 'Ingrese un margen entre 0 y 100 (máx. 2 decimales)');
    if (!Number.isInteger(dias) || dias < 1 || dias > 90) add('diasValidez', 'Ingrese entre 1 y 90 días');
    if (this.lineas().length === 0) add('lineas', 'Agregue al menos una línea a la cotización');

    this.lineas().forEach((l, i) => {
      const p = `lineas[${i}].`;
      const cant = C.aCentesimas(l.cantidad);
      if (cant == null || cant <= 0) add(p + 'cantidad', 'Cantidad mayor a 0 (máx. 2 decimales)');
      else if (l.unidadEntera && cant % 100 !== 0) add(p + 'cantidad', 'Este producto se vende por unidades enteras');

      if (l.tipoItem === 'MANO_OBRA') {
        if (!l.descripcion.trim()) add(p + 'descripcion', 'Describa el trabajo');
        if (C.aCentesimas(l.precioUnitario) == null) add(p + 'precioUnitario', 'Ingrese un precio válido');
        return;
      }
      const m = C.aCentesimas(l.margenPct);
      if (m == null || m > 10000) add(p + 'margenPct', 'Margen entre 0 y 100');
      if (l.tipoItem === 'PRODUCTO' && !this.tieneProducto(l)) add(p + 'productoId', 'Seleccione un producto de la lista');
      if (l.tipoItem === 'MATERIAL' && !this.tieneProducto(l)) {
        if (!l.descripcion.trim()) add(p + 'descripcion', 'Describa el material');
        if (!l.unidad.trim()) add(p + 'unidad', 'Indique la unidad');
        if (C.aCentesimas(l.costoUnitario) == null) add(p + 'costoUnitario', 'Ingrese un costo válido');
      }
    });
    return errores;
  }

  private limpiarErrores() {
    this.errorGeneral.set('');
    this.resumenErrores.set([]);
    this.erroresCab.set({});
    this.erroresLinea.set({});
  }

  private quitarErrorCab(campo: string) {
    if (this.erroresCab()[campo]) {
      const { [campo]: _, ...resto } = this.erroresCab();
      this.erroresCab.set(resto);
    }
  }

  private quitarErrorLinea(l: Linea, campo: string) {
    const i = this.lineas().indexOf(l);
    const actuales = this.erroresLinea()[i];
    if (actuales?.[campo]) {
      const { [campo]: _, ...resto } = actuales;
      this.erroresLinea.set({ ...this.erroresLinea(), [i]: resto });
    }
  }

  private mostrarErrores(errores: ErrorCampo[], mensaje?: string) {
    const cab: Record<string, string> = {};
    const porLinea: Record<number, Record<string, string>> = {};
    const resumen: string[] = [];
    for (const err of errores) {
      const m = /^lineas\[(\d+)\]\.?(\w*)$/.exec(err.campo ?? '');
      if (m) {
        const i = Number(m[1]);
        const campo = CAMPO_VISIBLE[m[2]] ?? m[2];
        porLinea[i] = { ...(porLinea[i] ?? {}), [campo || 'descripcion']: err.mensaje };
        resumen.push(`Línea ${i + 1}: ${err.mensaje}`);
      } else if (err.campo === 'lineas' || !err.campo) {
        cab['lineas'] = err.mensaje;
        resumen.push(err.mensaje);
      } else {
        cab[err.campo] = err.mensaje;
      }
    }
    this.erroresCab.set(cab);
    this.erroresLinea.set(porLinea);
    this.resumenErrores.set(resumen.slice(0, 8));
    this.errorGeneral.set(mensaje ?? 'Revise los datos marcados antes de guardar.');
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }

  protected errorDe(i: number, campo: string): string | undefined {
    return this.erroresLinea()[i]?.[campo];
  }

  // ------------------------------------------------------------------ guardar

  private numeroONulo(texto: string): number | null {
    const c = C.aCentesimas(texto);
    return c == null ? null : c / 100;
  }

  private construirPeticion() {
    return {
      clienteId: this.cab.clienteId,
      solicitudId: this.cab.solicitudId,
      margenAplicado: this.numeroONulo(this.cab.margenAplicado),
      diasValidez: Number(this.cab.diasValidez) || null,
      observaciones: this.cab.observaciones.trim() || null,
      filaVersion: this.filaVersion,
      // Importes y totales NO se envían: el servidor los calcula
      lineas: this.lineas().map((l) => ({
        id: l.id,
        tipoItem: l.tipoItem,
        productoId: l.productoId,
        descripcion: l.descripcion.trim() || null,
        unidad: l.unidad.trim() || null,
        cantidad: this.numeroONulo(l.cantidad),
        costoUnitario: l.tipoItem === 'MANO_OBRA' ? null : this.numeroONulo(l.costoUnitario),
        margenPct: l.tipoItem === 'MANO_OBRA' ? null : this.numeroONulo(l.margenPct),
        precioUnitario: l.tipoItem === 'MANO_OBRA' ? this.numeroONulo(l.precioUnitario) : null,
      })),
    };
  }

  protected async guardar() {
    const errores = this.validar();
    if (errores.length) {
      this.mostrarErrores(errores);
      return;
    }
    this.limpiarErrores();
    this.guardando.set(true);
    try {
      const id = this.id();
      const r = await firstValueFrom(
        id
          ? this.http.put<CotizacionResponse>(`/api/intranet/cotizaciones/${id}`, this.construirPeticion())
          : this.http.post<CotizacionResponse>('/api/intranet/cotizaciones', this.construirPeticion()),
      );
      this.sucio = false;
      await this.router.navigate(['/intranet/cotizaciones', r.id], { queryParams: { guardado: 1 } });
    } catch (e) {
      const err = errorApi(e);
      if (err.tipo === 'VALIDACION') this.mostrarErrores(err.errores, err.mensaje);
      else {
        this.errorGeneral.set(err.mensaje);
        window.scrollTo({ top: 0, behavior: 'smooth' });
      }
    } finally {
      this.guardando.set(false);
    }
  }

  protected cancelar() {
    const id = this.id();
    this.router.navigate(id ? ['/intranet/cotizaciones', id] : ['/intranet/cotizaciones']);
  }

  hayCambiosSinGuardar(): boolean {
    return this.sucio;
  }

  @HostListener('window:beforeunload', ['$event'])
  protected antesDeSalir(e: BeforeUnloadEvent) {
    if (this.sucio) e.preventDefault();
  }
}
