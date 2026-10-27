package pe.upeu.agrosierra.data.local

import app.cash.sqldelight.db.SqlDriver
import pe.upeu.agrosierra.db.AgroDatabase

/** Cada plataforma aporta su controlador de SQLDelight (Android: BaseLocalAndroid). */
interface BaseLocal {
    fun crearDriver(): SqlDriver
}

fun BaseLocal.abrir(): AgroDatabase = AgroDatabase(crearDriver())

/** Conversiones fila local <-> dominio (INTEGER 0/1 -> Boolean). */
internal fun pe.upeu.agrosierra.Lote.aDominio() = pe.upeu.agrosierra.domain.model.Lote(
    id = id,
    codigo = codigo,
    nombre = nombre,
    hectareas = hectareas,
    cultivo = cultivo,
    valle = valle,
    campaniaCerrada = campaniaCerrada == 1L,
    dosisMaximaPorHectarea = dosisMaxima,
)

internal fun pe.upeu.agrosierra.domain.model.Lote.aFila() = pe.upeu.agrosierra.Lote(
    id = id,
    codigo = codigo,
    nombre = nombre,
    hectareas = hectareas,
    cultivo = cultivo,
    valle = valle,
    campaniaCerrada = if (campaniaCerrada) 1L else 0L,
    dosisMaxima = dosisMaximaPorHectarea,
)

internal fun pe.upeu.agrosierra.Labor.aDominio() = pe.upeu.agrosierra.domain.model.Labor(
    id = id,
    loteId = loteId,
    tipo = pe.upeu.agrosierra.domain.model.TipoLabor.valueOf(tipo),
    fecha = fecha,
    responsable = responsable,
    cantidad = cantidad,
    observacion = observacion,
    pendiente = pendiente == 1L,
)

internal fun pe.upeu.agrosierra.domain.model.Labor.aFila() = pe.upeu.agrosierra.Labor(
    id = id,
    loteId = loteId,
    tipo = tipo.name,
    fecha = fecha,
    responsable = responsable,
    cantidad = cantidad,
    observacion = observacion,
    pendiente = if (pendiente) 1L else 0L,
)
