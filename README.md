# TI Solutions — Plataforma web

**Frontend:** Angular 22 (carpeta `frontend/`) · **Backend:** API REST con Spring Boot 3.3 y Java 17 · **Base de datos:** SQL Server.
Montos en **soles (S/)**, precios con **IGV incluido**.

Módulos implementados: acceso (clientes y personal por separado), usuarios del personal, portal del cliente
(solicitudes) y cotizaciones (maestro-detalle).

## Requisitos

| Herramienta | Versión | Notas |
|---|---|---|
| JDK | 17 | Por ejemplo Eclipse Temurin 17 (`winget install EclipseAdoptium.Temurin.17.JDK`) |
| Maven | 3.9+ | Lo trae IntelliJ; en VS Code instale Maven aparte y agréguelo al `PATH` |
| SQL Server | 2019 o superior | Instancia por defecto, **TCP/IP activo en el puerto 1433** y **autenticación mixta** |
| SSMS | cualquiera | Para ejecutar los scripts y crear el usuario |
| Node.js | 22, 24 o 26 | Para el frontend Angular (`winget install OpenJS.NodeJS`) |
| IDE | IntelliJ IDEA o VS Code | En VS Code: *Extension Pack for Java* + *Spring Boot Extension Pack* |

## Cómo ejecutarlo

### 1. Crear la base de datos
En SQL Server Management Studio (conectado con *Windows Authentication*) ejecute, en orden:
- `database/01_schema.sql` (crea la base `tisolutions`, tablas, restricciones y secuencia)
- `database/02_datos_prueba.sql` (clientes, proveedores SEGO/DIGICORP, productos y una cotización de ejemplo)
- `database/03_login.sql` (cuentas de clientes, columnas de seguridad del personal y bitácora de accesos)

### 2. Crear el usuario de la aplicación
La app entra con un usuario de SQL Server (no con Windows). Abra **New Query** en SSMS, cambie la contraseña y ejecute:

```sql
CREATE LOGIN app_tisolutions WITH PASSWORD = 'CambieEstaClave123!', CHECK_POLICY = ON, CHECK_EXPIRATION = OFF;
GO
USE tisolutions;
GO
CREATE USER app_tisolutions FOR LOGIN app_tisolutions;
ALTER ROLE db_datareader ADD MEMBER app_tisolutions;
ALTER ROLE db_datawriter ADD MEMBER app_tisolutions;
GRANT UPDATE ON OBJECT::seq_cotizacion TO app_tisolutions;  -- para numerar cotizaciones (C-00001...)
GO
```

<details>
<summary>Alternativa con clics en SSMS</summary>

1. *Security → Logins →* clic derecho *New Login*.
2. Ponga el nombre, marque **SQL Server authentication** y escriba la contraseña.
3. Desmarque *Enforce password expiration* y *User must change password at next login*.
4. En *User Mapping* marque la base `tisolutions` y los roles **db_datareader** y **db_datawriter**.
5. Ejecute igualmente la línea `GRANT UPDATE ON OBJECT::seq_cotizacion ...` de arriba (sin ella falla al crear cotizaciones).

</details>

### 3. Indicar usuario y contraseña a la app
Use variables de entorno (así la contraseña **no se sube a GitHub**). En PowerShell, una sola vez:

```
setx DB_USER "app_tisolutions"
setx DB_PASSWORD "su_clave"
```

Cierre y vuelva a abrir la terminal o el IDE para que las tome.
No escriba la contraseña en `application.properties`, porque ese archivo se sube al repositorio.

**Primer administrador** (solo la primera vez). Cada integrante elige su propio correo y su propia clave temporal
(mínimo 10 caracteres). El sistema pedirá cambiarla al entrar:

```
setx ADMIN_EMAIL "su_correo@tisolutions.pe"
setx ADMIN_PASSWORD "una_clave_temporal"
```

Si usa `admin@tisolutions.pe` (el de los datos de prueba), se activa esa cuenta. El administrador crea luego las
cuentas de Ventas y Técnicos desde **Intranet → Usuarios**.

### 4. Ejecutar mientras se programa (dos terminales)

**Terminal 1, backend** (en la carpeta `tisolutions`):
```
mvn spring-boot:run
```

**Terminal 2, frontend** (en la carpeta `tisolutions/frontend`; la primera vez ejecute antes `npm.cmd install`).
En PowerShell use `npm.cmd` en vez de `npm` si Windows dice que "la ejecución de scripts está deshabilitada":
```
npm.cmd start
```

Abra **http://localhost:4200**. Angular recarga solo al guardar cambios y le pasa las llamadas `/api` al backend
(ver `frontend/proxy.conf.json`).

| Quién | Dirección |
|---|---|
| Clientes (persona natural o empresa) | http://localhost:4200/login (y **Registrarse**) |
| Personal (Administrador, Ventas, Técnico) | http://localhost:4200/intranet/login |

### 5. Versión final (un solo servidor)
```
cd frontend
npm.cmd run build
cd ..
mvn package
java -jar target/tisolutions-cotizaciones-1.0.0.jar
```
Maven copia el Angular compilado dentro del `.jar` y todo queda en **http://localhost:8080**.
Con HTTPS, defina además `SESSION_COOKIE_SECURE=true`.

Pruebas unitarias: `mvn test` (cálculos, reglas de estado, DNI/RUC y contraseñas).

### Seguridad del acceso
- **Dos puertas separadas**: los clientes y el personal están en tablas distintas (`cuenta_cliente` y `usuario`) y
  cada puerta guarda su sesión aparte. Con una cuenta de cliente no se puede entrar a la intranet.
- Contraseñas cifradas con **BCrypt**. Nadie puede verlas, ni el administrador.
- **Bloqueo de 15 minutos** tras 5 intentos fallidos. El mensaje de error no dice si el correo existe.
- Protección **CSRF**: el backend entrega la cookie `XSRF-TOKEN` y Angular la devuelve en la cabecera `X-XSRF-TOKEN`.
- La sesión vive en el servidor; el navegador solo guarda una cookie `HttpOnly` (JavaScript no puede leerla, a diferencia
  de un token guardado en `localStorage`). Se cierra sola a los 30 minutos sin uso.
- Los guardias de Angular solo deciden qué pantalla mostrar; **el backend revisa la sesión y el rol en cada petición**.
- El personal nuevo recibe una **clave temporal** y debe cambiarla al primer ingreso.
- Cada intento de acceso queda en la **bitácora** (`registro_acceso`), visible en el Dashboard del administrador.
- Cada cotización queda registrada a nombre de quien inició sesión.

### Problemas comunes
| Error | Causa |
|---|---|
| `Connection refused` / `TCP/IP connection to the host localhost, port 1433 has failed` | SQL Server apagado o TCP/IP desactivado (*SQL Server Configuration Manager → Protocols → TCP/IP → Enable* y reiniciar el servicio) |
| `Login failed for user` | Contraseña incorrecta, usuario sin acceso a `tisolutions`, o el servidor solo acepta Windows (active la autenticación mixta) |
| `The UPDATE permission was denied on the object 'seq_cotizacion'` | Falta el `GRANT` del paso 2 |
| `mvn` no se reconoce | Maven no está instalado o no está en el `PATH` |

## Estructura

```
database/                     Scripts de BD (DDL + CHECK/FK), datos de prueba y módulo de acceso
src/main/java/pe/tisolutions/          BACKEND (API REST)
  dominio/                    Entidades JPA (Cotizacion, Usuario, CuentaCliente, RegistroAcceso...) y enums
  repositorio/                Spring Data JPA
  servicio/                   Reglas de negocio (cotizaciones, registro de clientes, usuarios, DNI/RUC)
  seguridad/                  Spring Security: dos puertas, bloqueo por intentos, CSRF, primer administrador
  dto/                        Lo que entra y sale de la API
  excepcion/                  Excepciones y ManejadorErroresApi (formato único de errores)
  web/api/                    Controladores REST
frontend/src/app/                      FRONTEND (Angular)
  core/                       Sesión, guardias, interceptor de errores, formatos, cálculo y modelos
  acceso/                     Login de clientes, registro, login de la intranet
  portal/                     "Mi cuenta" del cliente
  intranet/                   Menú lateral, dashboard, usuarios, cambio de contraseña, panel técnico
  cotizaciones/               Lista, formulario maestro-detalle y vista de una cotización
```

## Cálculos (precios con IGV incluido)

| Dato | Fórmula |
|---|---|
| Precio unitario (producto/material) | `ROUND(costo × (100 + margen) / 100, 2)` |
| Precio unitario (mano de obra) | lo ingresa el usuario |
| Importe de línea | `ROUND(cantidad × precio, 2)` |
| Total | `Σ importes` |
| Valor de venta (subtotal) | `ROUND(total / (1 + tasa IGV), 2)` |
| IGV | `total − subtotal` |

El navegador calcula una vista previa; **el servidor ignora cualquier importe enviado y recalcula siempre**.
La base de datos verifica `importe = ROUND(cantidad * precio_unitario, 2)` y `monto_total = subtotal + igv`.

## Reglas principales

- **Una transacción por operación**: cabecera y todas sus líneas se guardan juntas o no se guarda nada.
- **Editar líneas**: el formulario envía todas las líneas; las que traen `id` se modifican, las nuevas se insertan y las que faltan se eliminan.
- **Solo el BORRADOR es editable.** Al emitir, los precios quedan congelados. Para cambiar una cotización emitida se crea una **nueva versión** (C-00001 v2); al emitirla, la anterior pasa a *Reemplazada*.
- **Precio original conservado**: cada línea guarda una copia del costo del proveedor y de la descripción. Si el proveedor cambia el precio, la vista de detalle lo informa, pero la cotización no cambia.
- **Eliminación**: un borrador se elimina físicamente; una cotización emitida solo se **anula** con motivo (eliminación lógica). No se puede eliminar ni anular si tiene servicios técnicos activos; la FK de la BD también lo impide.
- **Concurrencia**: si dos usuarios editan la misma cotización, el segundo recibe un aviso en lugar de sobrescribir los cambios (`fila_version`).
- **Estados**: Borrador → Emitida → En negociación → Aceptada / Rechazada / Sin respuesta. Rechazada, Sin respuesta y Anulada exigen motivo.

## API

**Portal de clientes** (`/api/portal`, sesión de cliente):

| Método | Ruta | Uso |
|---|---|---|
| POST | `/api/portal/auth/login` · `/logout` | Iniciar / cerrar sesión |
| GET | `/api/portal/auth/yo` | Sesión actual (204 si no hay) |
| POST | `/api/portal/auth/registro` | Crear cuenta (persona natural o empresa) |
| GET | `/api/portal/mi-cuenta` | Datos, solicitudes y cotizaciones del cliente |
| POST | `/api/portal/solicitudes` | Nueva solicitud de servicio |

**Intranet** (`/api/intranet`, sesión del personal):

| Método | Ruta | Uso |
|---|---|---|
| POST | `/api/intranet/auth/login` · `/logout` | Iniciar / cerrar sesión |
| GET | `/api/intranet/auth/yo` | Sesión actual (204 si no hay) |
| POST | `/api/intranet/auth/cambiar-password` | Cambiar la contraseña propia |
| GET | `/api/intranet/admin/resumen` | Cifras y últimos accesos (Administrador) |
| GET · POST | `/api/intranet/admin/usuarios` | Listar / crear trabajador (devuelve clave temporal) |
| PUT | `/api/intranet/admin/usuarios/{id}` | Editar nombre y rol |
| PUT | `/api/intranet/admin/usuarios/{id}/estado` | Activar, bloquear, dar de baja |
| POST | `/api/intranet/admin/usuarios/{id}/restablecer` | Nueva clave temporal |
| GET | `/api/intranet/cotizaciones` | Listado con filtros y paginación (Ventas y Administrador) |
| GET | `/api/intranet/cotizaciones/config` | Margen, IGV, estados y clientes para el formulario |
| GET · PUT · DELETE | `/api/intranet/cotizaciones/{id}` | Consultar, editar borrador, eliminar borrador |
| POST | `/api/intranet/cotizaciones` | Registrar (201) |
| POST | `/api/intranet/cotizaciones/{id}/estado` | Emitir, negociar, aceptar, rechazar, anular |
| POST | `/api/intranet/cotizaciones/{id}/nueva-version` | Copiar una emitida en un nuevo borrador |
| GET | `/api/intranet/catalogo/productos?q=` | Buscar productos (sin distinguir tildes) |
| GET | `/api/intranet/catalogo/clientes/{id}/solicitudes` | Solicitudes abiertas del cliente |

Errores: 401 sin sesión o credenciales, 403 sin permiso (o `CAMBIO_PASSWORD`, `CSRF`), 422 validación
(`errores: [{campo: "lineas[2].cantidad", mensaje}]`), 409 regla de negocio o concurrencia, 404 no existe,
500 inesperado (con código de referencia en el log).

## Pendiente / supuestos

- "¿Olvidaste tu contraseña?", verificación del correo y doble factor para el administrador (requieren servicio de correo).
- La secuencia de códigos puede dejar saltos si una transacción falla después de pedir el número (comportamiento normal de `SEQUENCE`).
- La generación de PDF y el envío por correo/WhatsApp pertenecen a otra historia de usuario.
