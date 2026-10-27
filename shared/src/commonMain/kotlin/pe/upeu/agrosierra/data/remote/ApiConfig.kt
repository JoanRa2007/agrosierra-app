package pe.upeu.agrosierra.data.remote

/** Direccion base centralizada y tiempos de espera del cliente HTTP. */
object ApiConfig {
    /** 10.0.2.2 es el "localhost" de la PC vista desde el emulador de Android. */
    const val URL_BASE = "http://10.0.2.2:8080/api/v1/"
    const val TIEMPO_CONEXION_MS = 10_000L
    const val TIEMPO_PETICION_MS = 20_000L
    const val TAMANIO_PAGINA = 50
}

/** Configuracion que aporta cada plataforma (url y si se registran las peticiones). */
data class ConfigPlataforma(
    val urlBase: String = ApiConfig.URL_BASE,
    val depurar: Boolean = false,
)
