package pe.upeu.agrosierra

import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import pe.upeu.agrosierra.data.remote.dto.LaborRequestDto
import pe.upeu.agrosierra.domain.model.ErrorApi
import pe.upeu.agrosierra.domain.model.Resultado
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class MapeoErroresTest {
    private val req = LaborRequestDto(1, "FUMIGACION", "2026-10-27", "Joan Rodriguez", 99.0, "")

    private fun fallo(r: Resultado<*>): ErrorApi = assertIs<Resultado.Fallo>(r).error

    @Test
    fun cada409Y400SeTraduceAUnErrorTipadoConSuMotivo() = runTest {
        val e409 = Entorno { json(errorJson(409, "La campania esta cerrada"), HttpStatusCode.Conflict) }
        val conflicto = assertIs<ErrorApi.Conflicto>(fallo(e409.api.crearLabor(req)))
        assertEquals("La campania esta cerrada", conflicto.mensaje)

        val e400 = Entorno {
            json(errorJson(400, "Datos invalidos", ""","validationErrors":{"cantidad":"Supera la dosis"}"""), HttpStatusCode.BadRequest)
        }
        val validacion = assertIs<ErrorApi.Validacion>(fallo(e400.api.crearLabor(req)))
        assertEquals("Supera la dosis", validacion.campos["cantidad"])
    }

    @Test
    fun el403MantieneLaSesionYEl401LaBorra() = runTest {
        val e403 = Entorno { json(errorJson(403, "Sin privilegio"), HttpStatusCode.Forbidden) }
        assertIs<ErrorApi.SinPrivilegio>(fallo(e403.api.eliminarLabor(5)))
        assertEquals("token-prueba", e403.almacen.leerToken())

        val e401 = Entorno { json(errorJson(401, "Token vencido"), HttpStatusCode.Unauthorized) }
        assertIs<ErrorApi.NoAutorizado>(fallo(e401.api.lotes(0)))
        assertNull(e401.almacen.leerToken(), "un 401 borra el token")
    }

    @Test
    fun el500YLaFaltaDeRedSonErroresDistintos() = runTest {
        val e500 = Entorno { json("{}", HttpStatusCode.InternalServerError) }
        assertIs<ErrorApi.Servidor>(fallo(e500.api.lotes(0)))

        val sinRed = Entorno { throw kotlinx.io.IOException("sin senal") }
        assertEquals(ErrorApi.SinConexion, fallo(sinRed.api.lotes(0)))
    }

    @Test
    fun el204NoIntentaConvertirElCuerpo() = runTest {
        val e = Entorno { respond("", HttpStatusCode.NoContent) }
        assertIs<Resultado.Exito<Unit>>(e.api.eliminarLabor(3))
        assertEquals(listOf("DELETE /api/v1/labores/3"), e.peticiones)
    }
}
