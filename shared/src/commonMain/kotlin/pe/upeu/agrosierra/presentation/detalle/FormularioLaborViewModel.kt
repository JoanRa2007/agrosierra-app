package pe.upeu.agrosierra.presentation.detalle

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import pe.upeu.agrosierra.domain.model.ErrorApi
import pe.upeu.agrosierra.domain.model.FormularioLabor
import pe.upeu.agrosierra.domain.model.Resultado
import pe.upeu.agrosierra.domain.model.TipoLabor
import pe.upeu.agrosierra.domain.usecase.GuardarLaborUseCase
import pe.upeu.agrosierra.domain.usecase.ObservarLoteUseCase
import pe.upeu.agrosierra.domain.usecase.ObservarSesionUseCase
import pe.upeu.agrosierra.domain.usecase.ObtenerLaborUseCase
import pe.upeu.agrosierra.presentation.Avisos
import pe.upeu.agrosierra.presentation.navigation.Destino

data class FormularioUiState(
    val esEdicion: Boolean = false,
    val tipo: TipoLabor? = null,
    val fecha: String = "",
    val responsable: String = "",
    val cantidad: String = "",
    val observacion: String = "",
    /** Mensaje de error por campo (locales o devueltos por el servicio en validationErrors). */
    val errores: Map<String, String> = emptyMap(),
    val mensaje: String? = null,
    val reintentable: Boolean = false,
    val dosisMaximaTotal: Double? = null,
    val guardando: Boolean = false,
    val terminado: Boolean = false,
)

class FormularioLaborViewModel(
    private val destino: Destino.FormularioLabor,
    private val obtenerLabor: ObtenerLaborUseCase,
    private val guardarLabor: GuardarLaborUseCase,
    private val observarLote: ObservarLoteUseCase,
    private val observarSesion: ObservarSesionUseCase,
    private val avisos: Avisos,
) : ViewModel() {
    private val _estado = MutableStateFlow(
        FormularioUiState(
            esEdicion = destino.laborId != null,
            fecha = Clock.System.todayIn(TimeZone.currentSystemDefault()).toString(),
            responsable = observarSesion().value?.nombre.orEmpty(),
        ),
    )
    val estado: StateFlow<FormularioUiState> = _estado.asStateFlow()

    init {
        viewModelScope.launch {
            val lote = observarLote(destino.loteId).first()
            _estado.update { it.copy(dosisMaximaTotal = lote?.dosisMaximaTotal) }
            destino.laborId?.let { id ->
                obtenerLabor(id)?.let { l ->
                    _estado.update {
                        it.copy(
                            tipo = l.tipo, fecha = l.fecha, responsable = l.responsable,
                            cantidad = l.cantidad.toString(), observacion = l.observacion,
                        )
                    }
                }
            }
        }
    }

    fun onTipo(v: TipoLabor) = _estado.update { it.copy(tipo = v, errores = it.errores - "tipo" - "cantidad") }
    fun onFecha(v: String) = _estado.update { it.copy(fecha = v, errores = it.errores - "fecha") }
    fun onResponsable(v: String) = _estado.update { it.copy(responsable = v, errores = it.errores - "responsable") }
    fun onCantidad(v: String) = _estado.update { it.copy(cantidad = v, errores = it.errores - "cantidad") }
    fun onObservacion(v: String) = _estado.update { it.copy(observacion = v, errores = it.errores - "observacion") }

    fun guardar() {
        val s = _estado.value
        if (s.guardando) return
        _estado.update { it.copy(guardando = true, mensaje = null, reintentable = false) }
        viewModelScope.launch {
            val form = FormularioLabor(destino.loteId, s.tipo, s.fecha, s.responsable, s.cantidad, s.observacion)
            when (val r = guardarLabor(destino.laborId, form)) {
                is Resultado.Exito -> {
                    avisos.mostrar(
                        if (r.dato.pendiente) "Guardado sin conexion: quedara pendiente hasta sincronizar"
                        else "Labor guardada en el servidor",
                    )
                    _estado.update { it.copy(guardando = false, terminado = true) }
                }
                is Resultado.Fallo -> _estado.update {
                    when (val e = r.error) {
                        // 400: cada mensaje se muestra bajo el campo que lo origino.
                        is ErrorApi.Validacion -> it.copy(
                            guardando = false,
                            errores = e.campos,
                            mensaje = e.mensaje.takeIf { e.campos.isEmpty() },
                        )
                        is ErrorApi.Servidor -> it.copy(guardando = false, mensaje = e.mensaje, reintentable = true)
                        is ErrorApi.SinPrivilegio -> it.copy(guardando = false, mensaje = "Operacion no autorizada: ${e.mensaje}")
                        else -> it.copy(guardando = false, mensaje = e.message)
                    }
                }
            }
        }
    }
}
