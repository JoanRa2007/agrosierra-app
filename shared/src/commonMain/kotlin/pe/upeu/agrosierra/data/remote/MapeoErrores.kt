package pe.upeu.agrosierra.data.remote

import io.ktor.client.call.body
import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.network.sockets.SocketTimeoutException
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.statement.HttpResponse
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException
import kotlinx.io.IOException
import kotlinx.serialization.SerializationException
import pe.upeu.agrosierra.data.remote.dto.ErrorDto
import pe.upeu.agrosierra.domain.model.ErrorApi
import pe.upeu.agrosierra.domain.model.Resultado

/**
 * Punto UNICO donde cada respuesta HTTP se traduce a Resultado/ErrorApi.
 * 204 (Unit) no intenta convertir el cuerpo: solo se comprueba el codigo.
 */
suspend inline fun <reified T> llamar(peticion: () -> HttpResponse): Resultado<T> =
    try {
        val respuesta = peticion()
        if (respuesta.status.isSuccess()) {
            if (T::class == Unit::class) Resultado.Exito(Unit as T) else Resultado.Exito(respuesta.body<T>())
        } else {
            Resultado.Fallo(respuesta.aErrorApi())
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Throwable) {
        Resultado.Fallo(e.aErrorApi())
    }

suspend fun HttpResponse.aErrorApi(): ErrorApi {
    val cuerpo: ErrorDto? = try { body<ErrorDto>() } catch (e: Exception) { null }
    val mensaje = cuerpo?.message
    return when (status.value) {
        400 -> ErrorApi.Validacion(mensaje ?: "Datos invalidos", cuerpo?.validationErrors.orEmpty())
        401 -> if (mensaje != null) ErrorApi.NoAutorizado(mensaje) else ErrorApi.NoAutorizado()
        403 -> if (mensaje != null) ErrorApi.SinPrivilegio(mensaje) else ErrorApi.SinPrivilegio()
        404 -> if (mensaje != null) ErrorApi.NoEncontrado(mensaje) else ErrorApi.NoEncontrado()
        409 -> ErrorApi.Conflicto(mensaje ?: "La operacion entra en conflicto con una regla del negocio")
        in 500..599 -> ErrorApi.Servidor()
        else -> ErrorApi.Desconocido(mensaje ?: "Respuesta inesperada (${status.value})")
    }
}

fun Throwable.aErrorApi(): ErrorApi = when (this) {
    is ErrorApi -> this
    is HttpRequestTimeoutException, is ConnectTimeoutException, is SocketTimeoutException -> ErrorApi.TiempoAgotado
    is IOException -> ErrorApi.SinConexion
    is SerializationException -> ErrorApi.Desconocido("Respuesta del servidor con formato inesperado")
    else -> ErrorApi.Desconocido(message ?: "Error inesperado")
}
