package pe.upeu.agrosierra.domain.model

/**
 * Jerarquia sellada de errores del dominio. La capa de datos traduce CADA respuesta
 * del servicio (o falla de red) a uno de estos casos, y la interfaz reacciona distinto a cada uno.
 */
sealed class ErrorApi(override val message: String) : Exception(message) {
    /** 400: validacion fallida; [campos] trae el mensaje de cada campo. */
    data class Validacion(val mensaje: String, val campos: Map<String, String>) : ErrorApi(mensaje)
    /** 401: token ausente, alterado o vencido (o credenciales incorrectas en el login). */
    data class NoAutorizado(val mensaje: String = "Tu sesion vencio. Inicia sesion nuevamente") : ErrorApi(mensaje)
    /** 403: autenticado pero sin el privilegio requerido. La sesion se mantiene. */
    data class SinPrivilegio(val mensaje: String = "No estas autorizado para esta operacion") : ErrorApi(mensaje)
    /** 404 */
    data class NoEncontrado(val mensaje: String = "El registro ya no existe") : ErrorApi(mensaje)
    /** 409: regla de negocio del servidor incumplida. */
    data class Conflicto(val mensaje: String) : ErrorApi(mensaje)
    /** 5xx */
    data class Servidor(val mensaje: String = "El servidor tuvo un problema. Intenta de nuevo") : ErrorApi(mensaje)
    data object SinConexion : ErrorApi("Sin conexion con el servidor")
    data object TiempoAgotado : ErrorApi("El servidor tardo demasiado en responder")
    data class Desconocido(val mensaje: String) : ErrorApi(mensaje)

    /** Fallas de red: el registro se guarda localmente como pendiente. */
    val esDeRed: Boolean get() = this is SinConexion || this is TiempoAgotado
}

/** Resultado de una operacion: exito con dato o fallo con un error tipado. */
sealed interface Resultado<out T> {
    data class Exito<T>(val dato: T) : Resultado<T>
    data class Fallo(val error: ErrorApi) : Resultado<Nothing>
}

inline fun <T, R> Resultado<T>.map(f: (T) -> R): Resultado<R> = when (this) {
    is Resultado.Exito -> Resultado.Exito(f(dato))
    is Resultado.Fallo -> this
}
