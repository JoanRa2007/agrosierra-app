package pe.upeu.agrosierra.domain.usecase

import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import pe.upeu.agrosierra.domain.model.ResumenSincronizacion
import pe.upeu.agrosierra.domain.model.Resultado
import pe.upeu.agrosierra.domain.repository.LaborRepository
import pe.upeu.agrosierra.domain.repository.LoteRepository

/**
 * Sincronizacion offline-first. El ORDEN importa:
 *  1. Primero se SUBE lo pendiente (para no perder lo registrado sin senal).
 *  2. Despues se BAJA lo actualizado (lotes y labores), sin pisar filas pendientes.
 * Si se invirtiera, la descarga podria sobrescribir o duplicar los cambios locales.
 */
class SincronizarUseCase(
    private val lotes: LoteRepository,
    private val labores: LaborRepository,
    private val reloj: Clock = Clock.System,
) {
    suspend operator fun invoke(): Resultado<ResumenSincronizacion> {
        // 1) Subir
        val (enviados, rechazados) = when (val r = labores.enviarPendientes()) {
            is Resultado.Exito -> r.dato
            is Resultado.Fallo -> return r
        }
        // 2) Bajar lotes
        val lotesBajados = when (val r = lotes.descargar()) {
            is Resultado.Exito -> r.dato
            is Resultado.Fallo -> return r
        }
        // 3) Bajar labores de esos lotes
        val laboresBajadas = when (val r = labores.descargar(lotesBajados.map { it.id })) {
            is Resultado.Exito -> r.dato
            is Resultado.Fallo -> return r
        }
        val ahora = reloj.now().toLocalDateTime(TimeZone.currentSystemDefault())
        lotes.registrarSincronizacion(ahora.toString().take(16).replace('T', ' '))
        return Resultado.Exito(ResumenSincronizacion(enviados, rechazados, lotesBajados.size, laboresBajadas))
    }
}
