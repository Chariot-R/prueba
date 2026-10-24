/* Datos de prueba para el módulo de cotizaciones (montos en soles, IGV incluido) */
USE tisolutions;
GO

INSERT INTO rol (nombre) VALUES ('ADMINISTRADOR'), ('VENTAS'), ('TECNICO');

-- password_hash: reemplazar por un hash BCrypt real cuando se implemente el login
INSERT INTO usuario (rol_id, nombre, email, password_hash) VALUES
 (1, 'Administrador', 'admin@tisolutions.pe',  '$2a$10$reemplazarPorHashReal..............................'),
 (2, 'María López',   'mlopez@tisolutions.pe', '$2a$10$reemplazarPorHashReal..............................');

INSERT INTO cliente (tipo_cliente, nombre_razon_social, documento, telefono, email, medio_contacto) VALUES
 ('J', 'Empresa Logística Norte S.A.C.', '20512345671', '987654321', 'compras@logisticanorte.pe', 'WEB'),
 ('J', 'Constructora Pacífico S.A.',     '20498765432', '956123478', 'logistica@cpacifico.pe',    'WHATSAPP'),
 ('N', 'Juan Pérez Quispe',              '45678912',    '912345678', 'jperez@gmail.com',          'REFERIDO');

INSERT INTO solicitud (cliente_id, usuario_id, canal, descripcion, estado) VALUES
 (1, 2, 'WEB',      'Sistema de videovigilancia para almacén', 'EN_ATENCION'),
 (2, 2, 'WHATSAPP', 'Ampliación de red WiFi en oficinas',      'NUEVA'),
 (3, NULL, 'WEB',   'Cerco eléctrico para vivienda',           'NUEVA');

INSERT INTO proveedor (nombre, contacto) VALUES
 ('SEGO',     'ventas@sego.pe'),
 ('DIGICORP', 'cotizaciones@digicorp.pe');

INSERT INTO producto (proveedor_id, nombre, unidad, precio, stock) VALUES
 (1, 'Cámara domo 2MP Hikvision',          'UND',  120.00, 25),
 (1, 'DVR 8 canales 1080p',                'UND',  350.00,  6),
 (2, 'Cable UTP Cat6 (metro)',             'M',      2.50, 900),
 (2, 'Disco duro 2TB videovigilancia',     'UND',  280.00,  4),
 (1, 'Energizador cerco eléctrico 10 km',  'UND',  540.00,  3),
 (2, 'Access point WiFi 6 doble banda',    'UND',  410.00,  0);

INSERT INTO producto_precio_historial (producto_id, precio, stock)
SELECT id, precio, stock FROM producto;

INSERT INTO tecnico (nombre, especialidad, zona) VALUES
 ('Pedro Gómez', 'CCTV', 'Lima Norte'),
 ('Luis Vera',   'Redes', 'Lima Centro');
GO

/* Cotización de ejemplo EMITIDA, con importes ya calculados:
     4 x 156.00 = 624.00  (cámara: 120.00 + 30 %)
     1 x 455.00 = 455.00  (DVR:     350.00 + 30 %)
    50 x   3.25 = 162.50  (cable:     2.50 + 30 %)
     1 x 400.00 = 400.00  (mano de obra)
   Total con IGV = 1641.50 -> subtotal = ROUND(1641.50 / 1.18, 2) = 1391.10 ; IGV = 250.40 */
DECLARE @n INT = NEXT VALUE FOR seq_cotizacion;
DECLARE @codigo VARCHAR(12) = 'C-' + RIGHT('00000' + CAST(@n AS VARCHAR(10)), 5);

INSERT INTO cotizacion (codigo, cliente_id, solicitud_id, usuario_id, fecha_emision, estado,
                        margen_aplicado, tasa_igv, subtotal, igv, monto_total, observaciones)
VALUES (@codigo, 1, 1, 2, SYSDATETIME(), 'EMITIDA', 30.00, 0.18, 1391.10, 250.40, 1641.50,
        'Incluye configuración de acceso remoto desde celular.');

DECLARE @cot INT = SCOPE_IDENTITY();

INSERT INTO detalle_cotizacion (cotizacion_id, nro_linea, tipo_item, producto_id, descripcion, unidad,
                                cantidad, costo_unitario, margen_pct, precio_unitario, importe) VALUES
 (@cot, 1, 'PRODUCTO',  1,    'Cámara domo 2MP Hikvision',             'UND',  4, 120.00, 30, 156.00, 624.00),
 (@cot, 2, 'PRODUCTO',  2,    'DVR 8 canales 1080p',                   'UND',  1, 350.00, 30, 455.00, 455.00),
 (@cot, 3, 'MATERIAL',  3,    'Cable UTP Cat6 (metro)',                'M',   50,   2.50, 30,   3.25, 162.50),
 (@cot, 4, 'MANO_OBRA', NULL, 'Instalación y configuración de 4 cámaras', 'SERV', 1, NULL, NULL, 400.00, 400.00);

UPDATE solicitud SET estado = 'COTIZADA' WHERE id = 1;
GO
