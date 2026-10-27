package pe.upeu.agrosierra.data.remote.dto

import pe.upeu.agrosierra.domain.model.Labor
import pe.upeu.agrosierra.domain.model.Lote
import pe.upeu.agrosierra.domain.model.Sesion
import pe.upeu.agrosierra.domain.model.TipoLabor

/** Funciones de conversion DTO <-> dominio. El dominio no conoce anotaciones de serializacion. */
fun LoteDto.aDominio() = Lote(
    id = id,
    codigo = codigo,
    nombre = nombre,
    hectareas = hectareas,
    cultivo = cultivo,
    valle = valle,
    campaniaCerrada = campaniaCerrada,
    dosisMaximaPorHectarea = dosisMaxima,
)

fun LaborDto.aDominio() = Labor(
    id = id,
    loteId = loteId,
    tipo = TipoLabor.valueOf(tipo),
    fecha = fecha,
    responsable = responsable,
    cantidad = cantidad,
    observacion = observacion,
    pendiente = false,
)

fun Labor.aRequest() = LaborRequestDto(
    loteId = loteId,
    tipo = tipo.name,
    fecha = fecha,
    responsable = responsable,
    cantidad = cantidad,
    observacion = observacion,
)

fun LoginResponseDto.aSesion() = Sesion(
    token = token,
    usuario = usuario,
    rol = rol,
    privilegios = privilegios.toSet(),
    nombre = nombre,
    expiraEn = expiraEn,
)
