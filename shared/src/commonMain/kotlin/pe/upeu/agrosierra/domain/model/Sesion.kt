package pe.upeu.agrosierra.domain.model

// domain/model/Sesion.kt
data class Sesion(
    val token: String,
    val usuario: String,
    val rol: String,
    val privilegios: Set<String>,
    val nombre: String = usuario,
    /** Vencimiento del token (epoch ms). */
    val expiraEn: Long = 0,
) {
    fun tiene(privilegio: String): Boolean = privilegio in privilegios
}

/** Nombres de privilegio del contrato (seccion 3.3). La UI decide por privilegio, nunca por rol. */
object Privilegios {
    const val LOTE_LEER = "LOTE_LEER"
    const val LABOR_LEER = "LABOR_LEER"
    const val LABOR_CREAR = "LABOR_CREAR"
    const val LABOR_EDITAR = "LABOR_EDITAR"
    const val LABOR_ELIMINAR = "LABOR_ELIMINAR"
    const val CAMPANIA_CERRAR = "CAMPANIA_CERRAR"
}
