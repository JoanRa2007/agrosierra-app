package pe.upeu.agrosierra.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.logging.SIMPLE
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import pe.upeu.agrosierra.data.local.AlmacenSeguro
import pe.upeu.agrosierra.data.local.EstadoSesion

/** Tolerante a claves desconocidas: el servicio puede agregar campos sin romper la app. */
val jsonAgro = Json {
    ignoreUnknownKeys = true
    explicitNulls = false
    coerceInputValues = true
}

/**
 * UNICA instancia de HttpClient (Koin la registra como single).
 * - La cabecera Authorization se agrega aqui, en un solo lugar, para TODAS las peticiones.
 * - Un 401 en cualquier ruta protegida borra el token y limpia la sesion -> la app vuelve al login.
 */
fun crearClienteHttp(
    motor: HttpClientEngine,
    almacen: AlmacenSeguro,
    estadoSesion: EstadoSesion,
    config: ConfigPlataforma,
): HttpClient = HttpClient(motor) {
    expectSuccess = false

    install(ContentNegotiation) { json(jsonAgro) }

    install(HttpTimeout) {
        connectTimeoutMillis = ApiConfig.TIEMPO_CONEXION_MS
        requestTimeoutMillis = ApiConfig.TIEMPO_PETICION_MS
        socketTimeoutMillis = ApiConfig.TIEMPO_PETICION_MS
    }

    // Registro reducido en produccion: solo se registran peticiones en depuracion.
    install(Logging) {
        logger = Logger.SIMPLE
        level = if (config.depurar) LogLevel.INFO else LogLevel.NONE
        sanitizeHeader { it == HttpHeaders.Authorization }
    }

    defaultRequest { url(config.urlBase) }

    install(
        createClientPlugin("AutorizacionAgro") {
            onRequest { request, _ ->
                almacen.leerToken()?.let { request.headers[HttpHeaders.Authorization] = "Bearer $it" }
            }
            onResponse { response ->
                val esLogin = response.call.request.url.encodedPath.endsWith("auth/login")
                if (response.status == HttpStatusCode.Unauthorized && !esLogin) {
                    almacen.borrar()
                    estadoSesion.limpiar()
                }
            }
        },
    )
}
