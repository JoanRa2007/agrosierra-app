package pe.upeu.agrosierra.presentation

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/** Mensaje breve para el usuario; con [accion] se muestra un boton (p. ej. "Reintentar" ante un 500). */
data class Aviso(
    val texto: String,
    val accion: String? = null,
    val alAccionar: (() -> Unit)? = null,
)

/** Canal unico de avisos (snackbar) que los ViewModel usan para informar resultados. */
class Avisos {
    private val _flujo = MutableSharedFlow<Aviso>(extraBufferCapacity = 8)
    val flujo: SharedFlow<Aviso> = _flujo.asSharedFlow()

    fun mostrar(aviso: Aviso) { _flujo.tryEmit(aviso) }
    fun mostrar(texto: String) = mostrar(Aviso(texto))
}
