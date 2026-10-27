# AgroCampo — Aplicación KMP (Android) de AgroSierra

**Autor:** Joan Rodriguez · Desarrollo de Aplicaciones Móviles · UPeU · Examen Parcial Unidad 2 (Versión A)

App que usan los técnicos de campo para consultar lotes y registrar labores **con o sin señal**.
Kotlin Multiplatform (Android) · Compose Multiplatform · Ktor Client · Koin · SQLDelight · offline-first.

## Cómo ejecutarla

1. Levantar el backend (`agrosierra-api`): `./gradlew bootRun`.
2. Abrir **esta carpeta** en Android Studio, esperar la sincronización de Gradle.
3. Ejecutar la configuración **androidApp** en un emulador (API 26+).
4. Ingresar con `tecnico1` / `jefe1` / `admin` y contraseña `Agro2026*`.

La dirección del servicio está centralizada en `shared/.../data/remote/ApiConfig.kt`
(`http://10.0.2.2:8080/api/v1/` = la PC vista desde el emulador). En un celular físico cambia la IP por
la de tu PC y agrégala en `androidApp/src/main/res/xml/network_security_config.xml`.

Pruebas de `commonTest` (motor HTTP simulado + base SQLite en memoria):

```bash
./gradlew :shared:testDebugUnitTest
```

## Estructura de paquetes

```
shared/src/commonMain/kotlin/pe/upeu/agrosierra/
├── domain/
│   ├── model/        Lote, TipoLabor, Labor, Sesion (+ Privilegios), ErrorApi (+ Resultado), Sincronizacion
│   ├── repository/   Interfaces: SesionRepository, LoteRepository, LaborRepository, MonitorConexion
│   └── usecase/      Sesion, Lotes, Labores y SincronizarUseCase
├── data/
│   ├── remote/       ClienteHttp.kt, ApiConfig.kt, MapeoErrores.kt, AgroApi.kt
│   │   └── dto/      DTO (@Serializable, @SerialName) y funciones de conversión al dominio
│   ├── local/        AlmacenSeguro.kt (interfaz) + EstadoSesion, BaseLocal.kt
│   └── repository/   Implementaciones offline-first
├── presentation/     sesion/, listado/, detalle/, perfil/, navigation/, theme/ (ViewModel + StateFlow + UiState)
└── di/               redModule, dataModule, domainModule, presentationModule (platformModule en androidApp)
shared/src/commonMain/sqldelight/pe/upeu/agrosierra/AgroDb.sq   Esquema local y consultas
shared/src/commonTest/                                          4 archivos de prueba (8 pruebas)

androidApp/src/main/kotlin/pe/upeu/agrosierra/
├── AlmacenSeguroAndroid.kt     EncryptedSharedPreferences + MasterKey (Android Keystore)
├── BaseLocalAndroid.kt         AndroidSqliteDriver
└── PlatformModule.android.kt   Motor OkHttp, ConfigPlataforma, MonitorConexionAndroid
```

## Contrato del API consumido

Ver `agrosierra-api/README.md`. Todas las rutas bajo `/api/v1`; los listados llegan dentro del
envoltorio de paginación (por eso los DTO usan `PaginaDto<T>` y no `List<T>`).

## Roles y privilegios en la app

La interfaz **ofrece** cada acción según el privilegio de la sesión (calculado en el ViewModel, no en el
composable) y el servicio **igual la valida** (403 si se intenta sin privilegio):

| Acción              | Privilegio requerido |
|---------------------|----------------------|
| Ver padrón / detalle | LOTE_LEER, LABOR_LEER |
| «Registrar labor»   | LABOR_CREAR (y campaña abierta) |
| Corregir labor      | LABOR_EDITAR (y campaña abierta) |
| Eliminar labor      | LABOR_ELIMINAR |
| «Cerrar campaña»    | CAMPANIA_CERRAR (y campaña abierta) |

## Decisiones de arquitectura

1. **Offline-first**: las pantallas observan la base local (`asFlow().mapToList()`); la red solo se usa
   para enviar y sincronizar. Así la app funciona en modo avión.
2. **Registro sin red**: si el envío falla por red, la labor se guarda con **id negativo** (`siguienteIdLocal`
   + `minOf(…, 0) - 1`) y `pendiente = 1`. Al sincronizar, la fila provisional se reemplaza por la definitiva
   en **la misma transacción**.
3. **Orden de sincronización** (`SincronizarUseCase`): 1) subir pendientes, 2) bajar lotes, 3) bajar labores
   sin pisar filas pendientes (`INSERT OR IGNORE`). Manual (botón) y automática al recuperar la conexión.
4. **Un solo HttpClient** (Koin `single`) con ContentNegotiation, URL base, timeouts, logging solo en debug
   y un plugin que agrega `Authorization: Bearer` en un único lugar; un 401 borra el token y vuelve al login.
5. **Errores tipados**: `MapeoErrores.kt` traduce cada respuesta a `ErrorApi` (Validacion, NoAutorizado,
   SinPrivilegio, NoEncontrado, Conflicto, Servidor, SinConexion, TiempoAgotado). Cada uno tiene una conducta:
   400 → mensaje bajo el campo; 401 → login; 403 → aviso sin cerrar sesión; 404 → aviso y refresco;
   409 → motivo del servidor; 500 → aviso con «Reintentar».
6. **Token seguro**: `EncryptedSharedPreferences` con clave maestra en el Android Keystore.
7. **Capas**: domain (sin dependencias de frameworks ni anotaciones de serialización) ← data ← presentation,
   con inyección por Koin.
