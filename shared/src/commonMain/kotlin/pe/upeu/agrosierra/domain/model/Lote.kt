package pe.upeu.agrosierra.domain.model

// domain/model/Lote.kt
data class Lote(
    val id: Long,
    val codigo: String,
    val nombre: String,
    val hectareas: Double,
    val cultivo: String,
    val valle: String,
    val campaniaCerrada: Boolean,
    val dosisMaximaPorHectarea: Double,
) {
    /** Tope de RN-04 para una fumigacion en este lote. */
    val dosisMaximaTotal: Double get() = dosisMaximaPorHectarea * hectareas
}
