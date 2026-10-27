package pe.upeu.agrosierra.domain.usecase

import kotlinx.datetime.LocalDate
import pe.upeu.agrosierra.domain.model.ErrorApi
import pe.upeu.agrosierra.domain.model.FormularioLabor
import pe.upeu.agrosierra.domain.model.Labor
import pe.upeu.agrosierra.domain.model.Resultado
import pe.upeu.agrosierra.domain.repository.LaborRepository

class ObservarLaboresUseCase(private val repo: LaborRepository) {
    operator fun invoke(loteId: Long) = repo.observarLabores(loteId)
}

class ObtenerLaborUseCase(private val repo: LaborRepository) {
    suspend operator fun invoke(id: Long) = repo.obtener(id)
}

/**
 * Registra (id == null) o corrige una labor. Valida el formulario localmente; las reglas
 * de negocio (RN-01, RN-02, RN-04) las decide el servicio y su motivo llega como ErrorApi.
 */
class GuardarLaborUseCase(private val repo: LaborRepository) {
    suspend operator fun invoke(id: Long?, form: FormularioLabor): Resultado<Labor> {
        val campos = mutableMapOf<String, String>()
        if (form.tipo == null) campos["tipo"] = "Selecciona el tipo de labor"
        if (runCatching { LocalDate.parse(form.fecha) }.isFailure) campos["fecha"] = "Usa el formato AAAA-MM-DD"
        if (form.responsable.isBlank()) campos["responsable"] = "El responsable es obligatorio"
        val cantidad = form.cantidad.replace(',', '.').toDoubleOrNull()
        if (cantidad == null || cantidad <= 0) campos["cantidad"] = "Ingresa una cantidad mayor que cero"
        if (form.observacion.length > 250) campos["observacion"] = "Maximo 250 caracteres"
        if (campos.isNotEmpty()) return Resultado.Fallo(ErrorApi.Validacion("Revisa los campos marcados", campos))

        val labor = Labor(
            id = id ?: 0,
            loteId = form.loteId,
            tipo = form.tipo!!,
            fecha = form.fecha,
            responsable = form.responsable.trim(),
            cantidad = cantidad!!,
            observacion = form.observacion.trim(),
            pendiente = false,
        )
        return if (id == null) repo.registrar(labor) else repo.corregir(labor)
    }
}

class EliminarLaborUseCase(private val repo: LaborRepository) {
    suspend operator fun invoke(id: Long) = repo.eliminar(id)
}
