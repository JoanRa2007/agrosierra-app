package pe.upeu.agrosierra.presentation.perfil

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import pe.upeu.agrosierra.domain.usecase.CerrarSesionUseCase
import pe.upeu.agrosierra.domain.usecase.ObservarSesionUseCase

class PerfilViewModel(
    observarSesion: ObservarSesionUseCase,
    private val cerrarSesionUseCase: CerrarSesionUseCase,
) : ViewModel() {
    val sesion = observarSesion()

    /** Borra el token del almacenamiento seguro; la raiz detecta la sesion nula y muestra el login. */
    fun cerrarSesion() {
        viewModelScope.launch { cerrarSesionUseCase() }
    }
}
