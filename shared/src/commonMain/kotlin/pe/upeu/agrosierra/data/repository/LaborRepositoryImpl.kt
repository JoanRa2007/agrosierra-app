package pe.upeu.agrosierra.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import pe.upeu.agrosierra.data.local.aDominio
import pe.upeu.agrosierra.data.local.aFila
import pe.upeu.agrosierra.data.remote.AgroApi
import pe.upeu.agrosierra.data.remote.dto.LaborDto
import pe.upeu.agrosierra.data.remote.dto.aDominio
import pe.upeu.agrosierra.data.remote.dto.aRequest
import pe.upeu.agrosierra.db.AgroDatabase
import pe.upeu.agrosierra.domain.model.ErrorApi
import pe.upeu.agrosierra.domain.model.Labor
import pe.upeu.agrosierra.domain.model.Resultado
import pe.upeu.agrosierra.domain.repository.LaborRepository

/**
 * Implementacion offline-first de las labores.
 * - Con red: se envia al servicio y se guarda la fila definitiva (pendiente = 0).
 * - Sin red: se guarda localmente con id NEGATIVO y pendiente = 1, y se envia al sincronizar.
 */
class LaborRepositoryImpl(
    private val db: AgroDatabase,
    private val api: AgroApi,
    private val io: CoroutineDispatcher,
) : LaborRepository {
    private val q get() = db.agroDbQueries

    override fun observarLabores(loteId: Long): Flow<List<Labor>> =
        q.observarLaboresPorLote(loteId).asFlow().mapToList(io).map { filas -> filas.map { it.aDominio() } }

    override suspend fun obtener(id: Long): Labor? =
        withContext(io) { q.obtenerLabor(id).executeAsOneOrNull()?.aDominio() }

    override suspend fun registrar(labor: Labor): Resultado<Labor> =
        when (val r = api.crearLabor(labor.aRequest())) {
            is Resultado.Exito -> {
                val definitiva = r.dato.aDominio()
                withContext(io) { q.guardarLabor(definitiva.aFila()) }
                Resultado.Exito(definitiva)
            }
            is Resultado.Fallo ->
                if (r.error.esDeRed) Resultado.Exito(guardarProvisional(labor)) else r
        }

    /** Asigna un id de un rango que el servidor nunca emite (negativo) dentro de una transaccion. */
    private suspend fun guardarProvisional(labor: Labor): Labor = withContext(io) {
        db.transactionWithResult {
            val minimo: Long = q.siguienteIdLocal().executeAsOneOrNull() ?: 0L
            val provisional = labor.copy(id = minOf(minimo, 0L) - 1, pendiente = true)
            q.guardarLabor(provisional.aFila())
            provisional
        }
    }

    override suspend fun corregir(labor: Labor): Resultado<Labor> {
        if (labor.id < 0) {
            // Aun no existe en el servidor: se corrige la fila provisional y sigue pendiente.
            val local = labor.copy(pendiente = true)
            withContext(io) { q.guardarLabor(local.aFila()) }
            return Resultado.Exito(local)
        }
        return when (val r = api.corregirLabor(labor.id, labor.aRequest())) {
            is Resultado.Exito -> {
                val definitiva = r.dato.aDominio()
                withContext(io) { q.guardarLabor(definitiva.aFila()) }
                Resultado.Exito(definitiva)
            }
            is Resultado.Fallo -> when {
                r.error.esDeRed -> {
                    val local = labor.copy(pendiente = true)
                    withContext(io) { q.guardarLabor(local.aFila()) }
                    Resultado.Exito(local)
                }
                r.error is ErrorApi.NoEncontrado -> {
                    withContext(io) { q.eliminarLabor(labor.id) }
                    r
                }
                else -> r
            }
        }
    }

    override suspend fun eliminar(id: Long): Resultado<Unit> {
        if (id < 0) {
            withContext(io) { q.eliminarLabor(id) }
            return Resultado.Exito(Unit)
        }
        val r = api.eliminarLabor(id)
        if (r is Resultado.Exito || (r is Resultado.Fallo && r.error is ErrorApi.NoEncontrado)) {
            withContext(io) { q.eliminarLabor(id) } // 404: ya no existe, se refresca el listado
        }
        return r
    }

    override suspend fun enviarPendientes(): Resultado<Pair<Int, List<String>>> {
        val pendientes = withContext(io) { q.laboresPendientes().executeAsList().map { it.aDominio() } }
        var enviadas = 0
        val rechazos = mutableListOf<String>()
        for (labor in pendientes) {
            val r = if (labor.id < 0) api.crearLabor(labor.aRequest()) else api.corregirLabor(labor.id, labor.aRequest())
            when (r) {
                is Resultado.Exito -> {
                    // La fila provisional se sustituye por la definitiva en la MISMA transaccion.
                    withContext(io) {
                        db.transaction {
                            q.eliminarLabor(labor.id)
                            q.guardarLabor(r.dato.aDominio().aFila())
                        }
                    }
                    enviadas++
                }
                is Resultado.Fallo -> {
                    // Sin red o sesion vencida: se detiene y lo pendiente se conserva.
                    if (r.error.esDeRed || r.error is ErrorApi.NoAutorizado) return r
                    // Rechazo definitivo del servidor (400/403/404/409): se informa el motivo y
                    // se descarta el cambio local; la descarga trae la version del servidor.
                    rechazos += "${labor.tipo.name.lowercase()} del ${labor.fecha}: ${r.error.message}"
                    withContext(io) { q.eliminarLabor(labor.id) }
                }
            }
        }
        return Resultado.Exito(enviadas to rechazos)
    }

    override suspend fun descargar(loteIds: List<Long>): Resultado<Int> {
        var total = 0
        for (loteId in loteIds) {
            val labores = mutableListOf<LaborDto>()
            var pagina = 0
            do {
                val r = api.labores(loteId, pagina)
                if (r is Resultado.Fallo) return r
                val datos = (r as Resultado.Exito).dato
                labores += datos.contenido
                pagina++
            } while (!datos.ultima)
            withContext(io) {
                db.transaction {
                    q.eliminarSincronizadasDeLote(loteId)
                    // INSERT OR IGNORE: no pisa filas con cambios locales pendientes.
                    labores.forEach { q.guardarLaborSiNoPendiente(it.aDominio().aFila()) }
                }
            }
            total += labores.size
        }
        return Resultado.Exito(total)
    }
}
