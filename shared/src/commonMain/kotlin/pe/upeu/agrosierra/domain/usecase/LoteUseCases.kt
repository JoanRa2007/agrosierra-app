package pe.upeu.agrosierra.domain.usecase

import pe.upeu.agrosierra.domain.repository.LoteRepository

class ObservarLotesUseCase(private val repo: LoteRepository) {
    /** El filtro se resuelve en la consulta local, no recorriendo la lista en la interfaz. */
    operator fun invoke(valle: String?, texto: String?) =
        repo.observarLotes(valle?.takeIf { it.isNotBlank() }, texto?.trim()?.takeIf { it.isNotEmpty() })
}

class ObservarLoteUseCase(private val repo: LoteRepository) {
    operator fun invoke(id: Long) = repo.observarLote(id)
}

class ObservarVallesUseCase(private val repo: LoteRepository) {
    operator fun invoke() = repo.observarValles()
}

class ObservarUltimaSincronizacionUseCase(private val repo: LoteRepository) {
    operator fun invoke() = repo.observarUltimaSincronizacion()
}

class ObservarTotalPendientesUseCase(private val repo: LoteRepository) {
    operator fun invoke() = repo.observarTotalPendientes()
}

class CerrarCampaniaUseCase(private val repo: LoteRepository) {
    suspend operator fun invoke(loteId: Long) = repo.cerrarCampania(loteId)
}
