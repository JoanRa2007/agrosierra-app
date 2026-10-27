package pe.upeu.agrosierra.domain.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import pe.upeu.agrosierra.domain.model.Labor
import pe.upeu.agrosierra.domain.model.Lote
import pe.upeu.agrosierra.domain.model.Resultado
import pe.upeu.agrosierra.domain.model.Sesion

/** Contratos del dominio. Las implementaciones offline-first viven en data/repository. */
interface SesionRepository {
    val sesion: StateFlow<Sesion?>
    /** Recupera la sesion guardada si el token sigue vigente (RF-A02). */
    suspend fun restaurar(): Sesion?
    suspend fun iniciarSesion(usuario: String, contrasena: String): Resultado<Sesion>
    suspend fun cerrarSesion()
}

interface LoteRepository {
    fun observarLotes(valle: String?, texto: String?): Flow<List<Lote>>
    fun observarLote(id: Long): Flow<Lote?>
    fun observarValles(): Flow<List<String>>
    fun observarUltimaSincronizacion(): Flow<String?>
    fun observarTotalPendientes(): Flow<Long>
    suspend fun cerrarCampania(loteId: Long): Resultado<Lote>
    /** Baja todos los lotes del servidor y los guarda en la base local. */
    suspend fun descargar(): Resultado<List<Lote>>
    suspend fun registrarSincronizacion(instante: String)
}

interface LaborRepository {
    fun observarLabores(loteId: Long): Flow<List<Labor>>
    suspend fun obtener(id: Long): Labor?
    /** Devuelve la labor guardada; si no hay red queda pendiente con id negativo. */
    suspend fun registrar(labor: Labor): Resultado<Labor>
    suspend fun corregir(labor: Labor): Resultado<Labor>
    suspend fun eliminar(id: Long): Resultado<Unit>
    /** Envia las filas pendientes. Devuelve (enviadas, motivos de rechazo). */
    suspend fun enviarPendientes(): Resultado<Pair<Int, List<String>>>
    /** Baja las labores de los lotes indicados sin pisar filas pendientes. */
    suspend fun descargar(loteIds: List<Long>): Resultado<Int>
}

/** Estado de la red del dispositivo (implementado por la plataforma). */
interface MonitorConexion {
    val conectado: StateFlow<Boolean>
}
