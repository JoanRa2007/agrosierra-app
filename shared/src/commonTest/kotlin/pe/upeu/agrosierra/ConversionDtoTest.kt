package pe.upeu.agrosierra

import kotlinx.serialization.decodeFromString
import pe.upeu.agrosierra.data.remote.dto.LaborDto
import pe.upeu.agrosierra.data.remote.dto.LoteDto
import pe.upeu.agrosierra.data.remote.dto.PaginaDto
import pe.upeu.agrosierra.data.remote.dto.aDominio
import pe.upeu.agrosierra.data.remote.jsonAgro
import pe.upeu.agrosierra.domain.model.TipoLabor
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class ConversionDtoTest {

    @Test
    fun loteDtoConNombreSerializadoDistintoYClavesDesconocidas() {
        val json = LOTE_JSON.replace("}", ""","campoNuevoDelServidor":123}""")
        val lote = jsonAgro.decodeFromString<LoteDto>(json).aDominio()
        assertEquals(4.0, lote.dosisMaximaPorHectarea)
        assertEquals(10.0, lote.dosisMaximaTotal)
        assertEquals("L-001", lote.codigo)
        assertFalse(lote.campaniaCerrada)
    }

    @Test
    fun listadoSeDeserializaDesdeElEnvoltorioDePaginacion() {
        val pagina = jsonAgro.decodeFromString<PaginaDto<LaborDto>>(pagina(laborJson(7), laborJson(8)))
        val labores = pagina.contenido.map { it.aDominio() }
        assertEquals(listOf(7L, 8L), labores.map { it.id })
        assertEquals(TipoLabor.RIEGO, labores.first().tipo)
        assertFalse(labores.first().pendiente, "lo que llega del servidor nunca esta pendiente")
    }
}
