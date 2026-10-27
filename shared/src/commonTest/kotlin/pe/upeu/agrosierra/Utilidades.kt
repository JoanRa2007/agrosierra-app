package pe.upeu.agrosierra

import app.cash.sqldelight.db.SqlDriver
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.Dispatchers
import pe.upeu.agrosierra.data.local.AlmacenSeguro
import pe.upeu.agrosierra.data.local.EstadoSesion
import pe.upeu.agrosierra.data.remote.AgroApi
import pe.upeu.agrosierra.data.remote.ConfigPlataforma
import pe.upeu.agrosierra.data.remote.crearClienteHttp
import pe.upeu.agrosierra.data.repository.LaborRepositoryImpl
import pe.upeu.agrosierra.data.repository.LoteRepositoryImpl
import pe.upeu.agrosierra.db.AgroDatabase
import pe.upeu.agrosierra.domain.model.Sesion

/** Controlador de base de datos en memoria (cada plataforma de prueba lo provee). */
expect fun crearDriverPrueba(): SqlDriver

class AlmacenEnMemoria : AlmacenSeguro {
    var sesion: Sesion? = Sesion("token-prueba", "tecnico1", "TECNICO", setOf("LABOR_CREAR"), expiraEn = Long.MAX_VALUE)
    override fun guardarSesion(sesion: Sesion) { this.sesion = sesion }
    override fun leerSesion() = sesion
    override fun leerToken() = sesion?.token
    override fun borrar() { sesion = null }
}

/** Arma el sistema con el motor HTTP simulado y la base en memoria. */
class Entorno(manejador: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData) {
    val peticiones = mutableListOf<String>()
    val almacen = AlmacenEnMemoria()
    val estado = EstadoSesion()
    val db = AgroDatabase(crearDriverPrueba())
    private val motor = MockEngine { req ->
        peticiones += "${req.method.value} ${req.url.encodedPath}"
        manejador(req)
    }
    val api = AgroApi(crearClienteHttp(motor, almacen, estado, ConfigPlataforma("http://servidor/api/v1/")))
    val lotes = LoteRepositoryImpl(db, api, Dispatchers.Unconfined)
    val labores = LaborRepositoryImpl(db, api, Dispatchers.Unconfined)
}

fun MockRequestHandleScope.json(cuerpo: String, estado: HttpStatusCode = HttpStatusCode.OK) =
    respond(cuerpo, estado, headersOf(HttpHeaders.ContentType, "application/json"))

fun errorJson(estado: Int, mensaje: String, campos: String = "") =
    """{"timestamp":"2026-10-27T09:14:22","status":$estado,"error":"x","message":"$mensaje","path":"/api/v1/labores"$campos}"""

const val LOTE_JSON =
    """{"id":1,"codigo":"L-001","nombre":"Chacra Santa Rosa","hectareas":2.5,"cultivo":"Papa","valle":"Mantaro","campaniaCerrada":false,"dosisMaximaPorHectarea":4.0}"""

fun laborJson(id: Long, fecha: String = "2026-10-27") =
    """{"id":$id,"loteId":1,"tipo":"RIEGO","fecha":"$fecha","responsable":"Joan Rodriguez","cantidad":10.0,"observacion":"ok","registradoPor":"tecnico1"}"""

fun pagina(vararg elementos: String) =
    """{"contenido":[${elementos.joinToString(",")}],"pagina":0,"tamanio":50,"totalElementos":${elementos.size},"totalPaginas":1,"ultima":true}"""
