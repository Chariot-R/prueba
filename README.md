# TI Solutions — Módulo de Cotizaciones (maestro-detalle)

Spring Boot 3.3 · Java 17 · SQL Server · Thymeleaf. Montos en **soles (S/)**, precios con **IGV incluido**.

## Requisitos

| Herramienta | Versión | Notas |
|---|---|---|
| JDK | 17 | Por ejemplo Eclipse Temurin 17 (`winget install EclipseAdoptium.Temurin.17.JDK`) |
| Maven | 3.9+ | Lo trae IntelliJ; en VS Code instale Maven aparte y agréguelo al `PATH` |
| SQL Server | 2019 o superior | Instancia por defecto, **TCP/IP activo en el puerto 1433** y **autenticación mixta** |
| SSMS | cualquiera | Para ejecutar los scripts y crear el usuario |
| IDE | IntelliJ IDEA o VS Code | En VS Code: *Extension Pack for Java* + *Spring Boot Extension Pack* |

## Cómo ejecutarlo

### 1. Crear la base de datos
En SQL Server Management Studio (conectado con *Windows Authentication*) ejecute, en orden:
- `database/01_schema.sql` (crea la base `tisolutions`, tablas, restricciones y secuencia)
- `database/02_datos_prueba.sql` (clientes, proveedores SEGO/DIGICORP, productos y una cotización de ejemplo)

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

### 4. Ejecutar
```
mvn spring-boot:run
```
(o en IntelliJ / VS Code: ejecutar `TiSolutionsApplication`). Abra http://localhost:8080/cotizaciones

Pruebas unitarias: `mvn test` (cálculos y reglas de estado).

### Problemas comunes
| Error | Causa |
|---|---|
| `Connection refused` / `TCP/IP connection to the host localhost, port 1433 has failed` | SQL Server apagado o TCP/IP desactivado (*SQL Server Configuration Manager → Protocols → TCP/IP → Enable* y reiniciar el servicio) |
| `Login failed for user` | Contraseña incorrecta, usuario sin acceso a `tisolutions`, o el servidor solo acepta Windows (active la autenticación mixta) |
| `The UPDATE permission was denied on the object 'seq_cotizacion'` | Falta el `GRANT` del paso 2 |
| `mvn` no se reconoce | Maven no está instalado o no está en el `PATH` |

## Estructura

```
database/                     Script de BD (DDL + CHECK/FK) y datos de prueba
src/main/java/pe/tisolutions/
  dominio/                    Entidades JPA (Cotizacion = maestro, DetalleCotizacion = detalle) y enums
  repositorio/                Spring Data JPA
  servicio/
    CalculadoraCotizacion     Fórmulas de precio, importe, subtotal, IGV y total
    CotizacionService         Reglas de negocio y transacciones
  dto/                        Lo que entra y sale de la API (records con Bean Validation)
  excepcion/                  Excepciones de negocio y ManejadorErroresApi (formato único de errores)
  web/api/                    API JSON usada por el formulario
  web/pagina/                 Páginas Thymeleaf (lista, formulario, detalle)
src/main/resources/
  templates/cotizaciones/     lista.html · formulario.html · ver.html
  static/js/                  calculo.js (mismo cálculo que Java, en céntimos) · formulario.js · ver.js · api.js
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

| Método | Ruta | Uso |
|---|---|---|
| GET | `/api/cotizaciones/{id}` | Maestro + detalle |
| POST | `/api/cotizaciones` | Registrar (201) |
| PUT | `/api/cotizaciones/{id}` | Editar borrador |
| POST | `/api/cotizaciones/{id}/estado` | Emitir, negociar, aceptar, rechazar, anular |
| POST | `/api/cotizaciones/{id}/nueva-version` | Copiar una emitida en un nuevo borrador |
| DELETE | `/api/cotizaciones/{id}` | Eliminar borrador (204) |
| GET | `/api/productos?q=` | Buscar productos activos |
| GET | `/api/clientes/{id}/solicitudes` | Solicitudes abiertas del cliente |

Errores: 422 validación (`errores: [{campo: "lineas[2].cantidad", mensaje}]`), 409 regla de negocio o concurrencia, 404 no existe, 500 inesperado (con código de referencia en el log).

## Pendiente / supuestos

- Todavía no hay login: el usuario que registra se toma de `app.usuario-actual-id`. Al implementar Spring Security, se reemplaza en `CotizacionService`.
- La secuencia de códigos puede dejar saltos si una transacción falla después de pedir el número (comportamiento normal de `SEQUENCE`).
- La generación de PDF y el envío por correo/WhatsApp pertenecen a otra historia de usuario.
