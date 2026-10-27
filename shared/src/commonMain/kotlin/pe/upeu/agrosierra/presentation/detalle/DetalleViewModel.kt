package pe.upeu.agrosierra.presentation.detalle

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import pe.upeu.agrosierra.domain.model.ErrorApi
import pe.upeu.agrosierra.domain.model.Labor
import pe.upeu.agrosierra.domain.model.Lote
import pe.upeu.agrosierra.domain.model.Privilegios
import pe.upeu.agrosierra.domain.model.Resultado
import pe.upeu.agrosierra.domain.usecase.CerrarCampaniaUseCase
import pe.upeu.agrosierra.domain.usecase.EliminarLaborUseCase
import pe.upeu.agrosierra.domain.usecase.ObservarLaboresUseCase
import pe.upeu.agrosierra.domain.usecase.ObservarLoteUseCase
import pe.upeu.agrosierra.domain.usecase.ObservarSesionUseCase
import pe.upeu.agrosierra.presentation.Avisos
import pe.upeu.agrosierra.presentation.informar

data class DetalleUiState(
    val cargando: Boolean = true,
    val lote: Lote? = null,
    val labores: List<Labor> = emptyList(),
    // Que acciones se OFRECEN: se decide aqui, por privilegio de la sesion (el servicio igual las valida).
    val puedeRegistrar: Boolean = false,
    val puedeEditar: Boolean = false,
    val puedeEliminar: Boolean = false,
    val puedeCerrarCampania: Boolean = false,
    val procesando: Boolean = false,
)

class DetalleViewModel(
    private val loteId: Long,
    observarLote: ObservarLoteUseCase,
    observarLabores: ObservarLaboresUseCase,
    observarSesion: ObservarSesionUseCase,
    private val cerrarCampaniaUseCase: CerrarCampaniaUseCase,
    private val eliminarLaborUseCase: EliminarLaborUseCase,
    private val avisos: Avisos,
) : ViewModel() {
    private val procesando = MutableStateFlow(false)

    val estado: StateFlow<DetalleUiState> = combine(
        observarLote(loteId), observarLabores(loteId), observarSesion(), procesando,
    ) { lote, labores, sesion, proc ->
        val abierta = lote?.campaniaCerrada == false
        fun tiene(p: String) = sesion?.tiene(p) == true
        DetalleUiState(
            cargando = false,
            lote = lote,
            labores = labores,
            puedeRegistrar = abierta && tiene(Privilegios.LABOR_CREAR),
            puedeEditar = abierta && tiene(Privilegios.LABOR_EDITAR),
            puedeEliminar = tiene(Privilegios.LABOR_ELIMINAR),
            puedeCerrarCampania = abierta && tiene(Privilegios.CAMPANIA_CERRAR),
            procesando = proc,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DetalleUiState())

    fun cerrarCampania() = ejecutar {
        when (val r = cerrarCampaniaUseCase(loteId)) {
            is Resultado.Exito -> avisos.mostrar("Campania del lote ${r.dato.codigo} cerrada")
            is Resultado.Fallo -> when (r.error) {
                ErrorApi.SinConexion, ErrorApi.TiempoAgotado -> avisos.mostrar("Cerrar la campania requiere conexion")
                else -> avisos.informar(r.error) { cerrarCampania() }
            }
        }
    }

    fun eliminar(labor: Labor) = ejecutar {
        when (val r = eliminarLaborUseCase(labor.id)) {
            is Resultado.Exito -> avisos.mostrar("Labor eliminada")
            is Resultado.Fallo -> when (r.error) {
                ErrorApi.SinConexion, ErrorApi.TiempoAgotado -> avisos.mostrar("Eliminar una labor sincronizada requiere conexion")
                // 404: la fila local ya se borro y el listado se refresco solo; no se ofrece reintentar.
                is ErrorApi.NoEncontrado -> avisos.mostrar("La labor ya no existia en el servidor; se actualizo el listado")
                else -> avisos.informar(r.error) { eliminar(labor) }
            }
        }
    }

    private fun ejecutar(bloque: suspend () -> Unit) {
        if (procesando.value) return
        viewModelScope.launch {
            procesando.value = true
            try { bloque() } finally { procesando.value = false }
        }
    }
}
