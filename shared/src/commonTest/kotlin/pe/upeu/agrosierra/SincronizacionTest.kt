package pe.upeu.agrosierra

import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import pe.upeu.agrosierra.domain.model.Labor
import pe.upeu.agrosierra.domain.model.ResumenSincronizacion
import pe.upeu.agrosierra.domain.model.Resultado
import pe.upeu.agrosierra.domain.model.TipoLabor
import pe.upeu.agrosierra.domain.usecase.SincronizarUseCase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class SincronizacionTest {

    @Test
    fun primeroSubeLoPendienteYDespuesBajaSinDuplicarNiPerder() = runTest {
        var hayRed = false
        val e = Entorno { req ->
            if (!hayRed) throw kotlinx.io.IOException("sin senal")
            when {
                req.method == HttpMethod.Post && req.url.encodedPath.endsWith("/labores") ->
                    json(laborJson(50), HttpStatusCode.Created)
                req.url.encodedPath.endsWith("/lotes/1/labores") -> json(pagina(laborJson(50), laborJson(7, "2026-10-01")))
                req.url.encodedPath.endsWith("/lotes") -> json(pagina(LOTE_JSON))
                else -> json("{}", HttpStatusCode.NotFound)
            }
        }
        // 1) Registro en modo avion -> fila provisional -1
        e.labores.registrar(Labor(0, 1, TipoLabor.RIEGO, "2026-10-27", "Joan Rodriguez", 10.0, "ok", false))
        assertEquals(listOf(-1L), e.db.agroDbQueries.laboresPendientes().executeAsList().map { it.id })

        // 2) Vuelve la senal y se sincroniza
        hayRed = true
        e.peticiones.clear()
        val resumen = assertIs<Resultado.Exito<ResumenSincronizacion>>(SincronizarUseCase(e.lotes, e.labores)()).dato

        // Orden: primero el POST de lo pendiente, despues las descargas
        assertEquals("POST /api/v1/labores", e.peticiones.first())
        assertTrue(e.peticiones.indexOf("GET /api/v1/lotes") > 0)
        assertEquals(1, resumen.enviados)
        assertEquals(2, resumen.laboresRecibidas)

        // La provisional fue sustituida por la definitiva: sin duplicados ni perdidas
        val ids = e.db.agroDbQueries.observarLaboresPorLote(1).executeAsList().map { it.id }.sorted()
        assertEquals(listOf(7L, 50L), ids)
        assertEquals(0L, e.db.agroDbQueries.contarPendientes().executeAsOne())
    }
}
