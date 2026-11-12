/* =====================================================================
   TI Solutions - Módulo de acceso (login)
   Ejecutar DESPUÉS de 01_schema.sql y 02_datos_prueba.sql.

   Hay DOS tipos de cuenta, guardadas en tablas distintas a propósito:
     - usuario         -> personal (Administrador, Ventas, Técnico). Entra por /intranet/login.
     - cuenta_cliente  -> clientes (persona natural o empresa). Entra por /login.
   Así una cuenta de cliente nunca puede usarse para entrar a la intranet.
   ===================================================================== */
USE tisolutions;
GO

/* ---------- Personal: columnas de seguridad ---------- */
IF COL_LENGTH('usuario', 'intentos_fallidos') IS NULL
    ALTER TABLE usuario ADD
        intentos_fallidos      INT          NOT NULL CONSTRAINT df_usuario_intentos DEFAULT 0,
        bloqueado_hasta        DATETIME2(0) NULL,     -- bloqueo temporal por intentos fallidos
        debe_cambiar_password  BIT          NOT NULL CONSTRAINT df_usuario_cambiar DEFAULT 1,
        ultimo_acceso          DATETIME2(0) NULL,
        fecha_creacion         DATETIME2(0) NOT NULL CONSTRAINT df_usuario_fecha DEFAULT SYSDATETIME();
GO

/* ---------- Clientes: cuenta web ligada a un registro de cliente ---------- */
DROP TABLE IF EXISTS cuenta_cliente;
CREATE TABLE cuenta_cliente (
    id                 INT IDENTITY(1,1) CONSTRAINT pk_cuenta_cliente PRIMARY KEY,
    cliente_id         INT          NOT NULL CONSTRAINT fk_cuenta_cliente_cliente REFERENCES cliente(id)
                                    CONSTRAINT uq_cuenta_cliente_cliente UNIQUE,   -- una cuenta por cliente
    email              VARCHAR(120) NOT NULL CONSTRAINT uq_cuenta_cliente_email UNIQUE,
    password_hash      VARCHAR(255) NOT NULL,
    nombre_contacto    VARCHAR(100) NOT NULL,      -- persona que usa la cuenta (en empresas, el contacto)
    estado             VARCHAR(15)  NOT NULL CONSTRAINT df_cuenta_cliente_estado DEFAULT 'ACTIVO'
                       CONSTRAINT ck_cuenta_cliente_estado CHECK (estado IN ('ACTIVO','BLOQUEADO','BAJA')),
    intentos_fallidos  INT          NOT NULL CONSTRAINT df_cuenta_cliente_intentos DEFAULT 0,
    bloqueado_hasta    DATETIME2(0) NULL,
    acepto_terminos    DATETIME2(0) NOT NULL,      -- cuándo aceptó los términos y condiciones
    fecha_registro     DATETIME2(0) NOT NULL CONSTRAINT df_cuenta_cliente_fecha DEFAULT SYSDATETIME(),
    ultimo_acceso      DATETIME2(0) NULL
);
GO

/* ---------- Bitácora de accesos (auditoría) ---------- */
DROP TABLE IF EXISTS registro_acceso;
CREATE TABLE registro_acceso (
    id           BIGINT IDENTITY(1,1) CONSTRAINT pk_registro_acceso PRIMARY KEY,
    tipo_cuenta  VARCHAR(10)  NOT NULL CONSTRAINT ck_registro_acceso_tipo CHECK (tipo_cuenta IN ('PERSONAL','CLIENTE')),
    email        VARCHAR(120) NOT NULL,
    exito        BIT          NOT NULL,
    motivo       VARCHAR(40)  NULL,                -- CREDENCIALES, BLOQUEADA, DESHABILITADA...
    ip           VARCHAR(45)  NULL,
    fecha        DATETIME2(0) NOT NULL CONSTRAINT df_registro_acceso_fecha DEFAULT SYSDATETIME()
);
CREATE INDEX ix_registro_acceso_fecha ON registro_acceso (fecha DESC);
GO

/* Los usuarios de prueba tienen un hash falso: no pueden entrar hasta que el
   administrador les restablezca la contraseña desde Intranet > Usuarios.
   El primer administrador se crea solo al arrancar la app con las variables
   de entorno ADMIN_EMAIL y ADMIN_PASSWORD (ver README). */
