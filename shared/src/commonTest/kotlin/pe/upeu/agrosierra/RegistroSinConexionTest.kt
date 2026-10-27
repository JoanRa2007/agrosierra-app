package pe.upeu.agrosierra

import kotlinx.coroutines.test.runTest
import pe.upeu.agrosierra.domain.model.Labor
import pe.upeu.agrosierra.domain.model.Resultado
import pe.upeu.agrosierra.domain.model.TipoLabor
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class RegistroSinConexionTest {
    private val nueva = Labor(0, 1, TipoLabor.RIEGO, "2026-10-27", "Joan Rodriguez", 15.0, "modo avion", pendiente = false)

    @Test
    fun sinRedLaLaborQuedaPendienteConIdProvisionalNegativo() = runTest {
        val e = Entorno { throw kotlinx.io.IOException("modo avion") }

        val primera = assertIs<Resultado.Exito<Labor>>(e.labores.registrar(nueva)).dato
        val segunda = assertIs<Resultado.Exito<Labor>>(e.labores.registrar(nueva.copy(cantidad = 20.0))).dato

        assertEquals(-1L, primera.id)
        assertEquals(-2L, segunda.id)
        assertTrue(primera.pendiente && segunda.pendiente)
        val filas = e.db.agroDbQueries.laboresPendientes().executeAsList()
        assertEquals(listOf(-1L, -2L), filas.map { it.id })
        assertTrue(filas.all { it.pendiente == 1L })
    }
}
