package pe.upeu.agrosierra.presentation.sesion

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import pe.upeu.agrosierra.domain.model.Sesion
import pe.upeu.agrosierra.domain.usecase.ObservarSesionUseCase
import pe.upeu.agrosierra.domain.usecase.RestaurarSesionUseCase
import pe.upeu.agrosierra.presentation.navigation.Navegador

sealed interface EstadoRaiz {
    data object Cargando : EstadoRaiz
    data object SinSesion : EstadoRaiz
    data class ConSesion(val sesion: Sesion) : EstadoRaiz
}

class RaizViewModel(
    restaurarSesion: RestaurarSesionUseCase,
    observarSesion: ObservarSesionUseCase,
    navegador: Navegador,
) : ViewModel() {
    private val restaurada = MutableStateFlow(false)

    val estado: StateFlow<EstadoRaiz> = combine(restaurada, observarSesion()) { lista, sesion ->
        when {
            !lista -> EstadoRaiz.Cargando
            sesion == null -> EstadoRaiz.SinSesion
            else -> EstadoRaiz.ConSesion(sesion)
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, EstadoRaiz.Cargando)

    init {
        // RF-A02: si el token guardado sigue vigente se entra directamente.
        viewModelScope.launch {
            restaurarSesion()
            restaurada.value = true
        }
        // Al cerrar sesion (o recibir 401) se vuelve al inicio de la navegacion.
        viewModelScope.launch {
            observarSesion().filter { it == null }.collect { navegador.reiniciar() }
        }
    }
}
