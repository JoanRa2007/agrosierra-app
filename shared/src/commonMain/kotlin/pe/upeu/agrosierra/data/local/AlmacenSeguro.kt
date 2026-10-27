package pe.upeu.agrosierra.data.local

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import pe.upeu.agrosierra.domain.model.Sesion

/**
 * Almacen seguro del token. En Android lo implementa AlmacenSeguroAndroid con
 * preferencias cifradas respaldadas por el almacen de claves (Android Keystore).
 */
interface AlmacenSeguro {
    fun guardarSesion(sesion: Sesion)
    fun leerSesion(): Sesion?
    fun leerToken(): String?
    fun borrar()
}

/** Sesion en memoria observada por la interfaz (null = sin sesion -> pantalla de login). */
class EstadoSesion {
    private val _sesion = MutableStateFlow<Sesion?>(null)
    val sesion: StateFlow<Sesion?> = _sesion.asStateFlow()

    fun establecer(sesion: Sesion) { _sesion.value = sesion }
    fun limpiar() { _sesion.value = null }
}
