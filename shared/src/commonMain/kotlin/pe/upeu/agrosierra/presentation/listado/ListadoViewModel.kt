package pe.upeu.agrosierra.presentation.listado

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import pe.upeu.agrosierra.domain.model.ErrorApi
import pe.upeu.agrosierra.domain.model.Lote
import pe.upeu.agrosierra.domain.model.Resultado
import pe.upeu.agrosierra.domain.repository.MonitorConexion
import pe.upeu.agrosierra.domain.usecase.ObservarLotesUseCase
import pe.upeu.agrosierra.domain.usecase.ObservarTotalPendientesUseCase
import pe.upeu.agrosierra.domain.usecase.ObservarUltimaSincronizacionUseCase
import pe.upeu.agrosierra.domain.usecase.ObservarVallesUseCase
import pe.upeu.agrosierra.domain.usecase.SincronizarUseCase
import pe.upeu.agrosierra.presentation.Avisos
import pe.upeu.agrosierra.presentation.informar

data class ListadoUiState(
    val cargando: Boolean = true,
    val lotes: List<Lote> = emptyList(),
    val valles: List<String> = emptyList(),
    val valle: String? = null,
    val ultimaSincronizacion: String? = null,
    val pendientes: Long = 0,
    val conectado: Boolean = true,
    val sincronizando: Boolean = false,
)

private data class Filtro(val valle: String? = null, val texto: String = "")

/**
 * Padron de lotes. La pantalla se alimenta de la BASE LOCAL (flujo observable),
 * nunca directamente de la red. La red solo se usa al sincronizar.
 */
@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class ListadoViewModel(
    observarLotes: ObservarLotesUseCase,
    observarValles: ObservarVallesUseCase,
    observarUltimaSincronizacion: ObservarUltimaSincronizacionUseCase,
    observarTotalPendientes: ObservarTotalPendientesUseCase,
    private val sincronizarUseCase: SincronizarUseCase,
    private val monitor: MonitorConexion,
    private val avisos: Avisos,
) : ViewModel() {

    private val valle = MutableStateFlow<String?>(null)
    private val _texto = MutableStateFlow("")
    /** El texto del buscador se expone aparte para que el campo responda sin retraso. */
    val texto: StateFlow<String> = _texto.asStateFlow()
    private val sincronizando = MutableStateFlow(false)
    private val candado = Mutex()

    private val filtro = combine(valle, _texto.debounce(250)) { v, t -> Filtro(v, t) }

    val estado: StateFlow<ListadoUiState> = combine(
        filtro.flatMapLatest { observarLotes(it.valle, it.texto) },
        observarValles(),
        observarUltimaSincronizacion(),
        observarTotalPendientes(),
        combine(valle, sincronizando, monitor.conectado) { v, sinc, red -> Triple(v, sinc, red) },
    ) { lotes, valles, ultima, pendientes, (v, sinc, red) ->
        ListadoUiState(
            cargando = false,
            lotes = lotes,
            valles = valles,
            valle = v,
            ultimaSincronizacion = ultima,
            pendientes = pendientes,
            conectado = red,
            sincronizando = sinc,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ListadoUiState())

    init {
        // Sincronizacion automatica: al entrar con red y cada vez que se recupera la conexion.
        viewModelScope.launch {
            monitor.conectado.collect { conectado -> if (conectado) sincronizar() }
        }
    }

    fun onTexto(valor: String) { _texto.value = valor }
    fun onValle(valor: String?) { valle.value = valor }

    /** Accion manual o automatica: primero sube lo pendiente y luego baja lo actualizado. */
    fun sincronizar() {
        if (!candado.tryLock()) return
        viewModelScope.launch {
            sincronizando.value = true
            try {
                when (val r = sincronizarUseCase()) {
                    is Resultado.Exito -> avisos.mostrar(r.dato.texto())
                    is Resultado.Fallo -> when (val e = r.error) {
                        ErrorApi.SinConexion, ErrorApi.TiempoAgotado ->
                            avisos.mostrar("Sin conexion: se sincronizara al recuperar la senal")
                        else -> avisos.informar(e) { sincronizar() }
                    }
                }
            } finally {
                sincronizando.value = false
                candado.unlock()
            }
        }
    }
}
