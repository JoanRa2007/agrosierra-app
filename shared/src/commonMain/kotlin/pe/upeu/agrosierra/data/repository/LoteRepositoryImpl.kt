package pe.upeu.agrosierra.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOne
import app.cash.sqldelight.coroutines.mapToOneOrNull
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import pe.upeu.agrosierra.data.local.aDominio
import pe.upeu.agrosierra.data.local.aFila
import pe.upeu.agrosierra.data.remote.AgroApi
import pe.upeu.agrosierra.data.remote.dto.aDominio
import pe.upeu.agrosierra.db.AgroDatabase
import pe.upeu.agrosierra.domain.model.Lote
import pe.upeu.agrosierra.domain.model.Resultado
import pe.upeu.agrosierra.domain.repository.LoteRepository

private const val CLAVE_ULTIMA_SINCRONIZACION = "ultima_sincronizacion"

/** Offline-first: la interfaz SIEMPRE lee de la base local mediante flujos observables. */
class LoteRepositoryImpl(
    private val db: AgroDatabase,
    private val api: AgroApi,
    private val io: CoroutineDispatcher,
) : LoteRepository {
    private val q get() = db.agroDbQueries

    override fun observarLotes(valle: String?, texto: String?): Flow<List<Lote>> =
        q.observarLotes(valle, texto).asFlow().mapToList(io).map { filas -> filas.map { it.aDominio() } }

    override fun observarLote(id: Long): Flow<Lote?> =
        q.observarLote(id).asFlow().mapToOneOrNull(io).map { it?.aDominio() }

    override fun observarValles(): Flow<List<String>> = q.observarValles().asFlow().mapToList(io)

    override fun observarUltimaSincronizacion(): Flow<String?> =
        q.observarMetadato(CLAVE_ULTIMA_SINCRONIZACION).asFlow().mapToOneOrNull(io)

    override fun observarTotalPendientes(): Flow<Long> = q.contarPendientes().asFlow().mapToOne(io)

    override suspend fun cerrarCampania(loteId: Long): Resultado<Lote> =
        when (val r = api.cerrarCampania(loteId)) {
            is Resultado.Fallo -> r
            is Resultado.Exito -> {
                val lote = r.dato.aDominio()
                withContext(io) { q.guardarLote(lote.aFila()) }
                Resultado.Exito(lote)
            }
        }

    override suspend fun descargar(): Resultado<List<Lote>> {
        val todos = mutableListOf<Lote>()
        var pagina = 0
        do {
            val r = api.lotes(pagina)
            if (r is Resultado.Fallo) return r
            val datos = (r as Resultado.Exito).dato
            todos += datos.contenido.map { it.aDominio() }
            pagina++
        } while (!datos.ultima)
        withContext(io) {
            db.transaction { todos.forEach { q.guardarLote(it.aFila()) } }
        }
        return Resultado.Exito(todos)
    }

    override suspend fun registrarSincronizacion(instante: String) {
        withContext(io) { q.guardarMetadato(CLAVE_ULTIMA_SINCRONIZACION, instante) }
    }
}
