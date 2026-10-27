package pe.upeu.agrosierra.domain.model

// domain/model/Labor.kt
data class Labor(
    val id: Long,              // negativo mientras este pendiente de envio
    val loteId: Long,
    val tipo: TipoLabor,
    val fecha: String,         // ISO-8601: 2026-10-27
    val responsable: String,
    val cantidad: Double,
    val observacion: String,
    val pendiente: Boolean,    // true mientras no haya llegado al servidor
)
