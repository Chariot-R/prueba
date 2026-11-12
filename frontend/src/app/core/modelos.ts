/** Tipos de los datos que envía y recibe la API (mismos nombres que los DTO de Java). */

export interface Sesion {
  tipo: 'PERSONAL' | 'CLIENTE';
  id: number;
  nombre: string;
  email: string;
  rol: string;
  rolEtiqueta: string;
  debeCambiarPassword: boolean;
}

export interface ErrorCampo {
  campo: string;
  mensaje: string;
}

export interface ErrorApi {
  status: number;
  tipo: string;
  mensaje: string;
  errores: ErrorCampo[];
  referencia?: string | null;
}

export interface Pagina<T> {
  contenido: T[];
  pagina: number;
  totalPaginas: number;
  totalElementos: number;
}

export interface Opcion {
  valor: string;
  etiqueta: string;
}

// ----------------------------- Cotizaciones -----------------------------

export interface CotizacionResumen {
  id: number;
  codigoCompleto: string;
  clienteNombre: string;
  fecha: string;
  estado: string;
  estadoEtiqueta: string;
  montoTotal: number;
}

export interface ClienteOpcion {
  id: number;
  nombre: string;
  documento: string;
}

export interface ConfigCotizacion {
  margenDefecto: number;
  tasaIgv: number;
  estados: Opcion[];
  clientes: ClienteOpcion[];
}

export type TipoItem = 'PRODUCTO' | 'MATERIAL' | 'MANO_OBRA';

export interface LineaResponse {
  id: number;
  nroLinea: number;
  tipoItem: TipoItem;
  tipoEtiqueta: string;
  productoId: number | null;
  proveedorNombre: string | null;
  descripcion: string;
  unidad: string;
  cantidad: number;
  costoUnitario: number | null;
  margenPct: number | null;
  precioUnitario: number;
  importe: number;
  costoActualProveedor: number | null;
}

export interface OpcionEstado {
  valor: string;
  etiqueta: string;
  requiereMotivo: boolean;
}

export interface CotizacionResponse {
  id: number;
  codigo: string;
  codigoCompleto: string;
  numeroVersion: number;
  codigoOrigen: string | null;
  origenId: number | null;
  estado: string;
  estadoEtiqueta: string;
  clienteId: number;
  clienteNombre: string;
  clienteDocumento: string;
  solicitudId: number | null;
  solicitudDescripcion: string | null;
  usuarioNombre: string;
  fecha: string;
  fechaEmision: string | null;
  fechaVencimiento: string | null;
  diasValidez: number;
  margenAplicado: number;
  tasaIgv: number;
  subtotal: number;
  igv: number;
  montoTotal: number;
  observaciones: string | null;
  motivoNoCierre: string | null;
  motivoAnulacion: string | null;
  filaVersion: number;
  editable: boolean;
  eliminable: boolean;
  permiteNuevaVersion: boolean;
  estadosSiguientes: OpcionEstado[];
  lineas: LineaResponse[];
}

export interface ProductoOpcion {
  id: number;
  nombre: string;
  proveedor: string;
  unidad: string;
  precio: number;
  stock: number;
}

export interface SolicitudOpcion {
  id: number;
  etiqueta: string;
  estado: string;
}

// ----------------------------- Administración -----------------------------

export interface Rol {
  id: number;
  nombre: string;
  etiqueta: string;
}

export interface UsuarioFila {
  id: number;
  nombre: string;
  email: string;
  rolId: number;
  rol: string;
  rolEtiqueta: string;
  estado: 'ACTIVO' | 'BLOQUEADO' | 'BAJA';
  bloqueoTemporal: boolean;
  bloqueadoHasta: string | null;
  debeCambiarPassword: boolean;
  ultimoAcceso: string | null;
  esUsted: boolean;
}

export interface ClaveTemporal {
  email: string;
  temporal: string;
}

export interface Acceso {
  fecha: string;
  tipoCuenta: 'PERSONAL' | 'CLIENTE';
  email: string;
  exito: boolean;
  motivo: string | null;
  ip: string | null;
}

export interface ResumenAdmin {
  personalActivo: number;
  clientesWeb: number;
  cotizaciones: number;
  fallidos24h: number;
  accesos: Acceso[];
}

// ----------------------------- Portal de clientes -----------------------------

export interface RegistroCliente {
  tipo: 'N' | 'J';
  nombres: string;
  apellidos: string;
  razonSocial: string;
  nombreContacto: string;
  documento: string;
  email: string;
  telefono: string;
  password: string;
  confirmar: string;
  terminos: boolean;
  sitioWeb: string;
}

export interface MiCuenta {
  nombre: string;
  contacto: string;
  empresa: boolean;
  documento: string;
  email: string;
  telefono: string | null;
  solicitudes: { id: number; fecha: string; descripcion: string; estado: string }[];
  cotizaciones: {
    codigo: string;
    fecha: string;
    vence: string | null;
    total: number;
    estado: string;
    estadoEtiqueta: string;
  }[];
}
