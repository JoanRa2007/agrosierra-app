package pe.upeu.agrosierra.presentation

import pe.upeu.agrosierra.domain.model.ErrorApi

/**
 * Conducta de la interfaz ante cada error tipado (seccion 3.5):
 * 401 lo resuelve el cliente HTTP (vuelve al login); 500 ofrece reintentar; el resto se informa.
 */
fun Avisos.informar(error: ErrorApi, reintentar: (() -> Unit)? = null) {
    when (error) {
        is ErrorApi.NoAutorizado -> Unit
        is ErrorApi.Servidor -> mostrar(Aviso(error.mensaje, "Reintentar", reintentar))
        is ErrorApi.SinPrivilegio -> mostrar("Operacion no autorizada: ${error.mensaje}")
        else -> mostrar(error.message)
    }
}
