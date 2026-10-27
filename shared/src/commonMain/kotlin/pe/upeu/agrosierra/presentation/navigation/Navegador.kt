package pe.upeu.agrosierra.presentation.navigation

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner

/** Destinos de la aplicacion. */
sealed interface Destino {
    data object Lotes : Destino
    data class DetalleLote(val loteId: Long) : Destino
    data class FormularioLabor(val loteId: Long, val laborId: Long? = null) : Destino
    data object Perfil : Destino
}

/** Cada entrada de la pila tiene su propio ViewModelStore: al salir de la pantalla su ViewModel se destruye. */
class Entrada(val destino: Destino) : ViewModelStoreOwner {
    override val viewModelStore: ViewModelStore = ViewModelStore()
}

/** Pila de navegacion simple, observable por Compose. */
class Navegador {
    private val pila = mutableStateListOf(Entrada(Destino.Lotes))

    val actual: Entrada get() = pila.last()

    fun ir(destino: Destino) {
        pila.add(Entrada(destino))
    }

    /** Devuelve false si ya esta en la pantalla inicial. */
    fun atras(): Boolean {
        if (pila.size <= 1) return false
        pila.removeAt(pila.lastIndex).viewModelStore.clear()
        return true
    }

    fun reiniciar() {
        if (pila.size == 1 && pila[0].destino == Destino.Lotes) return
        pila.forEach { it.viewModelStore.clear() }
        pila.clear()
        pila.add(Entrada(Destino.Lotes))
    }
}
