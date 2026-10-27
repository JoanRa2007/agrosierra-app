package pe.upeu.agrosierra.domain.model

/** Resumen que la interfaz muestra al terminar una sincronizacion. */
data class ResumenSincronizacion(
    val enviados: Int = 0,
    val rechazados: List<String> = emptyList(),
    val lotesRecibidos: Int = 0,
    val laboresRecibidas: Int = 0,
) {
    fun texto(): String = buildString {
        append("Enviados: $enviados · Recibidos: $lotesRecibidos lotes, $laboresRecibidas labores")
        if (rechazados.isNotEmpty()) append("\nRechazados (${rechazados.size}): ${rechazados.joinToString("; ")}")
    }
}

/** Datos del formulario de labor antes de convertirse en una [Labor]. */
data class FormularioLabor(
    val loteId: Long,
    val tipo: TipoLabor?,
    val fecha: String,
    val responsable: String,
    val cantidad: String,
    val observacion: String,
)
