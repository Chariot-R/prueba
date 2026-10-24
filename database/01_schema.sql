/* =====================================================================
   TI Solutions - Módulo de Cotizaciones (maestro-detalle)
   Motor: SQL Server 2019+
   Convenciones:
     - Montos en soles (PEN), DECIMAL(12,2), precios CON IGV incluido.
     - Las reglas críticas se repiten aquí como última barrera
       (NOT NULL, CHECK, FK, UNIQUE), aunque la capa de negocio ya las valide.
   ===================================================================== */

IF DB_ID('tisolutions') IS NULL
    CREATE DATABASE tisolutions;
GO
USE tisolutions;
GO

/* ---------- Limpieza (orden inverso a las dependencias) ---------- */
DROP TABLE IF EXISTS servicio_tecnico;
DROP TABLE IF EXISTS tecnico;
DROP TABLE IF EXISTS detalle_cotizacion;
DROP TABLE IF EXISTS cotizacion;
DROP TABLE IF EXISTS producto_precio_historial;
DROP TABLE IF EXISTS producto;
DROP TABLE IF EXISTS proveedor;
DROP TABLE IF EXISTS solicitud;
DROP TABLE IF EXISTS contacto;
DROP TABLE IF EXISTS cliente;
DROP TABLE IF EXISTS usuario;
DROP TABLE IF EXISTS rol;
DROP SEQUENCE IF EXISTS seq_cotizacion;
GO

/* ======================= Tablas de referencia ======================= */

CREATE TABLE rol (
    id      INT IDENTITY(1,1) CONSTRAINT pk_rol PRIMARY KEY,
    nombre  VARCHAR(50) NOT NULL CONSTRAINT uq_rol_nombre UNIQUE
);

CREATE TABLE usuario (
    id             INT IDENTITY(1,1) CONSTRAINT pk_usuario PRIMARY KEY,
    rol_id         INT          NOT NULL CONSTRAINT fk_usuario_rol REFERENCES rol(id),
    nombre         VARCHAR(100) NOT NULL,
    email          VARCHAR(120) NOT NULL CONSTRAINT uq_usuario_email UNIQUE,
    password_hash  VARCHAR(255) NOT NULL,
    estado         VARCHAR(15)  NOT NULL CONSTRAINT df_usuario_estado DEFAULT 'ACTIVO'
                   CONSTRAINT ck_usuario_estado CHECK (estado IN ('ACTIVO','BLOQUEADO','BAJA'))
);

CREATE TABLE cliente (
    id                  INT IDENTITY(1,1) CONSTRAINT pk_cliente PRIMARY KEY,
    tipo_cliente        CHAR(1)      NOT NULL CONSTRAINT ck_cliente_tipo CHECK (tipo_cliente IN ('N','J')), -- Natural / Jurídica
    nombre_razon_social VARCHAR(150) NOT NULL,
    documento           VARCHAR(11)  NOT NULL CONSTRAINT uq_cliente_documento UNIQUE,              -- DNI (8) o RUC (11)
    telefono            VARCHAR(15)  NULL,
    email               VARCHAR(120) NULL,
    medio_contacto      VARCHAR(20)  NULL,
    estado              VARCHAR(10)  NOT NULL CONSTRAINT df_cliente_estado DEFAULT 'ACTIVO'
                        CONSTRAINT ck_cliente_estado CHECK (estado IN ('ACTIVO','ARCHIVADO')),
    CONSTRAINT ck_cliente_documento CHECK (
        (tipo_cliente = 'N' AND LEN(documento) = 8) OR
        (tipo_cliente = 'J' AND LEN(documento) = 11))
);

CREATE TABLE contacto (
    id          INT IDENTITY(1,1) CONSTRAINT pk_contacto PRIMARY KEY,
    cliente_id  INT          NOT NULL CONSTRAINT fk_contacto_cliente REFERENCES cliente(id),
    nombre      VARCHAR(100) NOT NULL,
    cargo       VARCHAR(60)  NULL,
    telefono    VARCHAR(15)  NULL
);

CREATE TABLE solicitud (
    id              INT IDENTITY(1,1) CONSTRAINT pk_solicitud PRIMARY KEY,
    cliente_id      INT          NOT NULL CONSTRAINT fk_solicitud_cliente REFERENCES cliente(id),
    usuario_id      INT          NULL     CONSTRAINT fk_solicitud_usuario REFERENCES usuario(id),
    canal           VARCHAR(20)  NOT NULL,
    descripcion     VARCHAR(300) NULL,
    estado          VARCHAR(15)  NOT NULL CONSTRAINT df_solicitud_estado DEFAULT 'NUEVA'
                    CONSTRAINT ck_solicitud_estado CHECK (estado IN ('NUEVA','EN_ATENCION','COTIZADA','ATENDIDA','CERRADA')),
    fecha_creacion  DATETIME2(0) NOT NULL CONSTRAINT df_solicitud_fecha DEFAULT SYSDATETIME(),
    -- Clave alterna para que cotizacion pueda exigir "la solicitud es de ESTE cliente"
    CONSTRAINT uq_solicitud_id_cliente UNIQUE (id, cliente_id)
);

CREATE TABLE proveedor (
    id        INT IDENTITY(1,1) CONSTRAINT pk_proveedor PRIMARY KEY,
    nombre    VARCHAR(100) NOT NULL CONSTRAINT uq_proveedor_nombre UNIQUE,
    contacto  VARCHAR(150) NULL
);

CREATE TABLE producto (
    id                   INT IDENTITY(1,1) CONSTRAINT pk_producto PRIMARY KEY,
    proveedor_id         INT           NOT NULL CONSTRAINT fk_producto_proveedor REFERENCES proveedor(id),
    nombre               VARCHAR(150)  NOT NULL,
    unidad               VARCHAR(10)   NOT NULL CONSTRAINT df_producto_unidad DEFAULT 'UND',
    precio               DECIMAL(12,2) NOT NULL CONSTRAINT ck_producto_precio CHECK (precio >= 0), -- precio proveedor, IGV incluido
    stock                INT           NOT NULL CONSTRAINT ck_producto_stock  CHECK (stock >= 0),
    fecha_actualizacion  DATETIME2(0)  NOT NULL CONSTRAINT df_producto_fecha DEFAULT SYSDATETIME(),
    activo               BIT           NOT NULL CONSTRAINT df_producto_activo DEFAULT 1
);

-- Histórico de precios por proveedor (lo alimenta el módulo de proveedores)
CREATE TABLE producto_precio_historial (
    id           INT IDENTITY(1,1) CONSTRAINT pk_producto_precio_historial PRIMARY KEY,
    producto_id  INT           NOT NULL CONSTRAINT fk_pph_producto REFERENCES producto(id),
    precio       DECIMAL(12,2) NOT NULL,
    stock        INT           NOT NULL,
    fecha        DATETIME2(0)  NOT NULL CONSTRAINT df_pph_fecha DEFAULT SYSDATETIME()
);
GO

/* ============================ MAESTRO ============================ */

CREATE SEQUENCE seq_cotizacion AS INT START WITH 1 INCREMENT BY 1;
GO

CREATE TABLE cotizacion (
    id                    INT IDENTITY(1,1) CONSTRAINT pk_cotizacion PRIMARY KEY,
    codigo                VARCHAR(12)   NOT NULL,                       -- C-00001
    version               SMALLINT      NOT NULL CONSTRAINT df_cot_version DEFAULT 1,
    cotizacion_origen_id  INT           NULL CONSTRAINT fk_cot_origen REFERENCES cotizacion(id),
    cliente_id            INT           NOT NULL,
    solicitud_id          INT           NULL,
    usuario_id            INT           NOT NULL CONSTRAINT fk_cot_usuario REFERENCES usuario(id),
    fecha                 DATETIME2(0)  NOT NULL CONSTRAINT df_cot_fecha DEFAULT SYSDATETIME(),
    fecha_emision         DATETIME2(0)  NULL,
    dias_validez          SMALLINT      NOT NULL CONSTRAINT df_cot_validez DEFAULT 15,
    estado                VARCHAR(15)   NOT NULL CONSTRAINT df_cot_estado DEFAULT 'BORRADOR',
    margen_aplicado       DECIMAL(5,2)  NOT NULL,                       -- margen por defecto (%)
    tasa_igv              DECIMAL(5,4)  NOT NULL CONSTRAINT df_cot_tasa_igv DEFAULT 0.18, -- se congela con la cotización
    subtotal              DECIMAL(12,2) NOT NULL CONSTRAINT df_cot_subtotal DEFAULT 0,    -- valor de venta (sin IGV)
    igv                   DECIMAL(12,2) NOT NULL CONSTRAINT df_cot_igv DEFAULT 0,
    monto_total           DECIMAL(12,2) NOT NULL CONSTRAINT df_cot_total DEFAULT 0,       -- precio de venta (con IGV)
    observaciones         VARCHAR(500)  NULL,
    motivo_no_cierre      VARCHAR(300)  NULL,
    motivo_anulacion      VARCHAR(300)  NULL,
    fecha_anulacion       DATETIME2(0)  NULL,
    fila_version          INT           NOT NULL CONSTRAINT df_cot_fila_version DEFAULT 0, -- bloqueo optimista (JPA @Version)

    CONSTRAINT uq_cot_codigo_version UNIQUE (codigo, version),
    CONSTRAINT fk_cot_cliente   FOREIGN KEY (cliente_id) REFERENCES cliente(id),
    -- La solicitud (si existe) debe pertenecer al mismo cliente.
    -- Con solicitud_id NULL la FK no se evalúa.
    CONSTRAINT fk_cot_solicitud_cliente FOREIGN KEY (solicitud_id, cliente_id)
        REFERENCES solicitud(id, cliente_id),

    CONSTRAINT ck_cot_estado CHECK (estado IN
        ('BORRADOR','EMITIDA','EN_NEGOCIACION','ACEPTADA','RECHAZADA','SIN_RESPUESTA','ANULADA','REEMPLAZADA')),
    CONSTRAINT ck_cot_margen   CHECK (margen_aplicado BETWEEN 0 AND 100),
    CONSTRAINT ck_cot_tasa_igv CHECK (tasa_igv BETWEEN 0 AND 1),
    CONSTRAINT ck_cot_validez  CHECK (dias_validez BETWEEN 1 AND 90),
    CONSTRAINT ck_cot_montos   CHECK (subtotal >= 0 AND igv >= 0 AND monto_total = subtotal + igv),
    CONSTRAINT ck_cot_emision  CHECK (estado = 'BORRADOR' OR fecha_emision IS NOT NULL),
    CONSTRAINT ck_cot_no_cierre CHECK (estado NOT IN ('RECHAZADA','SIN_RESPUESTA')
                                       OR (motivo_no_cierre IS NOT NULL AND LEN(LTRIM(RTRIM(motivo_no_cierre))) > 0)),
    CONSTRAINT ck_cot_anulacion CHECK (estado <> 'ANULADA'
                                       OR (motivo_anulacion IS NOT NULL AND LEN(LTRIM(RTRIM(motivo_anulacion))) > 0
                                           AND fecha_anulacion IS NOT NULL)),
    CONSTRAINT ck_cot_version  CHECK ((version = 1 AND cotizacion_origen_id IS NULL)
                                   OR (version > 1 AND cotizacion_origen_id IS NOT NULL))
);

CREATE INDEX ix_cot_cliente   ON cotizacion(cliente_id);
CREATE INDEX ix_cot_solicitud ON cotizacion(solicitud_id);
CREATE INDEX ix_cot_estado    ON cotizacion(estado, fecha);
GO

/* ============================ DETALLE ============================ */

CREATE TABLE detalle_cotizacion (
    id               INT IDENTITY(1,1) CONSTRAINT pk_detalle_cotizacion PRIMARY KEY,
    cotizacion_id    INT           NOT NULL,
    nro_linea        SMALLINT      NOT NULL,
    tipo_item        VARCHAR(10)   NOT NULL,
    producto_id      INT           NULL,
    descripcion      VARCHAR(200)  NOT NULL,   -- copia del nombre al momento de cotizar
    unidad           VARCHAR(10)   NOT NULL,
    cantidad         DECIMAL(12,2) NOT NULL,
    costo_unitario   DECIMAL(12,2) NULL,       -- copia del precio del proveedor (con IGV)
    margen_pct       DECIMAL(5,2)  NULL,
    precio_unitario  DECIMAL(12,2) NOT NULL,   -- precio de venta (con IGV)
    importe          DECIMAL(12,2) NOT NULL,

    -- Las líneas no existen sin su cotización: si se borra un BORRADOR, se borran con él.
    CONSTRAINT fk_det_cotizacion FOREIGN KEY (cotizacion_id) REFERENCES cotizacion(id) ON DELETE CASCADE,
    -- Un producto usado en cotizaciones no puede borrarse (se desactiva con producto.activo = 0).
    CONSTRAINT fk_det_producto   FOREIGN KEY (producto_id)   REFERENCES producto(id),

    CONSTRAINT ck_det_tipo      CHECK (tipo_item IN ('PRODUCTO','MATERIAL','MANO_OBRA')),
    CONSTRAINT ck_det_cantidad  CHECK (cantidad > 0),
    CONSTRAINT ck_det_costo     CHECK (costo_unitario IS NULL OR costo_unitario >= 0),
    CONSTRAINT ck_det_margen    CHECK (margen_pct IS NULL OR margen_pct BETWEEN 0 AND 100),
    CONSTRAINT ck_det_precio    CHECK (precio_unitario >= 0),
    CONSTRAINT ck_det_nro_linea CHECK (nro_linea > 0),
    CONSTRAINT ck_det_importe   CHECK (importe = ROUND(cantidad * precio_unitario, 2)),
    -- Reglas por tipo de línea (ojo: un CHECK que da NULL se considera válido, por eso los IS NOT NULL)
    CONSTRAINT ck_det_reglas_tipo CHECK (
        (tipo_item = 'PRODUCTO'  AND producto_id IS NOT NULL AND costo_unitario IS NOT NULL AND margen_pct IS NOT NULL)
     OR (tipo_item = 'MATERIAL'  AND costo_unitario IS NOT NULL AND margen_pct IS NOT NULL)
     OR (tipo_item = 'MANO_OBRA' AND producto_id IS NULL AND costo_unitario IS NULL AND margen_pct IS NULL))
);

CREATE INDEX ix_det_cotizacion ON detalle_cotizacion(cotizacion_id, nro_linea);
CREATE INDEX ix_det_producto   ON detalle_cotizacion(producto_id);
GO

/* ================== Documentos relacionados ================== */

CREATE TABLE tecnico (
    id           INT IDENTITY(1,1) CONSTRAINT pk_tecnico PRIMARY KEY,
    nombre       VARCHAR(100) NOT NULL,
    especialidad VARCHAR(60)  NULL,
    zona         VARCHAR(60)  NULL
);

CREATE TABLE servicio_tecnico (
    id                    INT IDENTITY(1,1) CONSTRAINT pk_servicio_tecnico PRIMARY KEY,
    -- NO ACTION: no se puede borrar una cotización que tiene servicios.
    cotizacion_id         INT          NOT NULL CONSTRAINT fk_serv_cotizacion REFERENCES cotizacion(id),
    tecnico_id            INT          NULL     CONSTRAINT fk_serv_tecnico REFERENCES tecnico(id),
    fecha_programada      DATETIME2(0) NULL,
    fecha_estimada_fin    DATETIME2(0) NULL,
    estado                VARCHAR(15)  NOT NULL CONSTRAINT df_serv_estado DEFAULT 'PENDIENTE'
                          CONSTRAINT ck_serv_estado CHECK (estado IN ('PENDIENTE','PROGRAMADO','EN_PROCESO','FINALIZADO','ANULADO')),
    motivo_reprogramacion VARCHAR(300) NULL
);
GO
