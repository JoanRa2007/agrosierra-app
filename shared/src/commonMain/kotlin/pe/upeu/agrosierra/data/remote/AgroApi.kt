package pe.upeu.agrosierra.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import pe.upeu.agrosierra.data.remote.dto.LaborDto
import pe.upeu.agrosierra.data.remote.dto.LaborRequestDto
import pe.upeu.agrosierra.data.remote.dto.LoginRequestDto
import pe.upeu.agrosierra.data.remote.dto.LoginResponseDto
import pe.upeu.agrosierra.data.remote.dto.LoteDto
import pe.upeu.agrosierra.data.remote.dto.PaginaDto
import pe.upeu.agrosierra.data.remote.dto.UsuarioDto
import pe.upeu.agrosierra.domain.model.Resultado

/** Rutas del contrato (seccion 4.3). Rutas relativas a ApiConfig.URL_BASE (/api/v1/). */
class AgroApi(private val http: HttpClient) {

    suspend fun login(req: LoginRequestDto): Resultado<LoginResponseDto> = llamar {
        http.post("auth/login") { contentType(ContentType.Application.Json); setBody(req) }
    }

    suspend fun usuarioActual(): Resultado<UsuarioDto> = llamar { http.get("usuarios/me") }

    suspend fun lotes(pagina: Int, tamanio: Int = ApiConfig.TAMANIO_PAGINA): Resultado<PaginaDto<LoteDto>> = llamar {
        http.get("lotes") { parameter("pagina", pagina); parameter("tamanio", tamanio) }
    }

    suspend fun labores(loteId: Long, pagina: Int, tamanio: Int = ApiConfig.TAMANIO_PAGINA): Resultado<PaginaDto<LaborDto>> =
        llamar { http.get("lotes/$loteId/labores") { parameter("pagina", pagina); parameter("tamanio", tamanio) } }

    suspend fun crearLabor(req: LaborRequestDto): Resultado<LaborDto> = llamar {
        http.post("labores") { contentType(ContentType.Application.Json); setBody(req) }
    }

    suspend fun corregirLabor(id: Long, req: LaborRequestDto): Resultado<LaborDto> = llamar {
        http.put("labores/$id") { contentType(ContentType.Application.Json); setBody(req) }
    }

    suspend fun eliminarLabor(id: Long): Resultado<Unit> = llamar { http.delete("labores/$id") }

    suspend fun cerrarCampania(loteId: Long): Resultado<LoteDto> = llamar { http.post("lotes/$loteId/cerrar-campania") }
}
