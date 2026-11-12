import { HttpClient, HttpParams } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { errorApi } from '../core/errores';
import { ClaseEstadoPipe, FechaPipe, SolesPipe } from '../core/formato';
import { ConfigCotizacion, CotizacionResumen, Opcion, Pagina } from '../core/modelos';

/** Listado de cotizaciones con filtros. Los filtros viajan en la URL (se conservan al volver atrás). */
@Component({
  selector: 'app-cotizaciones-lista',
  imports: [FormsModule, RouterLink, FechaPipe, SolesPipe, ClaseEstadoPipe],
  templateUrl: './cotizaciones-lista.html',
})
export class CotizacionesLista {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);
  private readonly ruta = inject(ActivatedRoute);

  protected estados = signal<Opcion[]>([]);
  protected resultado = signal<Pagina<CotizacionResumen> | null>(null);
  protected error = signal('');
  protected filtros = { texto: '', estado: '', desde: '', hasta: '', incluirCerradas: false };

  constructor() {
    this.http.get<ConfigCotizacion>('/api/intranet/cotizaciones/config').subscribe((c) => this.estados.set(c.estados));
    this.ruta.queryParamMap.subscribe((q) => {
      this.filtros = {
        texto: q.get('texto') ?? '',
        estado: q.get('estado') ?? '',
        desde: q.get('desde') ?? '',
        hasta: q.get('hasta') ?? '',
        incluirCerradas: q.get('incluirCerradas') === 'true',
      };
      this.buscar(Number(q.get('pagina') ?? 0));
    });
  }

  private buscar(pagina: number) {
    let p = new HttpParams().set('pagina', pagina);
    for (const [k, v] of Object.entries(this.filtros)) {
      if (v !== '' && v !== false) p = p.set(k, String(v));
    }
    this.error.set('');
    this.http.get<Pagina<CotizacionResumen>>('/api/intranet/cotizaciones', { params: p }).subscribe({
      next: (r) => this.resultado.set(r),
      error: (e) => this.error.set(errorApi(e).mensaje),
    });
  }

  protected filtrar(pagina = 0) {
    const q: Record<string, string | number | boolean | null> = {
      texto: this.filtros.texto.trim() || null,
      estado: this.filtros.estado || null,
      desde: this.filtros.desde || null,
      hasta: this.filtros.hasta || null,
      incluirCerradas: this.filtros.incluirCerradas || null,
      pagina: pagina || null,
    };
    this.router.navigate([], { queryParams: q });
  }

  protected abrir(id: number) {
    this.router.navigate(['/intranet/cotizaciones', id]);
  }
}
