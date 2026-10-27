package pe.upeu.agrosierra.data.repository

import kotlinx.coroutines.flow.StateFlow
import kotlinx.datetime.Clock
import pe.upeu.agrosierra.data.local.AlmacenSeguro
import pe.upeu.agrosierra.data.local.EstadoSesion
import pe.upeu.agrosierra.data.remote.AgroApi
import pe.upeu.agrosierra.data.remote.dto.LoginRequestDto
import pe.upeu.agrosierra.data.remote.dto.aSesion
import pe.upeu.agrosierra.domain.model.Resultado
import pe.upeu.agrosierra.domain.model.Sesion
import pe.upeu.agrosierra.domain.repository.SesionRepository

class SesionRepositoryImpl(
    private val api: AgroApi,
    private val almacen: AlmacenSeguro,
    private val estado: EstadoSesion,
    private val reloj: Clock = Clock.System,
) : SesionRepository {

    override val sesion: StateFlow<Sesion?> = estado.sesion

    override suspend fun restaurar(): Sesion? {
        val guardada = almacen.leerSesion() ?: return null
        if (guardada.expiraEn <= reloj.now().toEpochMilliseconds()) {
            almacen.borrar() // token vencido: se vuelve al login sin mensajes tecnicos
            return null
        }
        estado.establecer(guardada)
        return guardada
    }

    override suspend fun iniciarSesion(usuario: String, contrasena: String): Resultado<Sesion> =
        when (val r = api.login(LoginRequestDto(usuario, contrasena))) {
            is Resultado.Fallo -> r
            is Resultado.Exito -> {
                var sesion = r.dato.aSesion()
                almacen.guardarSesion(sesion)
                // RF-B07: se confirman los privilegios con /usuarios/me para decidir que acciones ofrecer.
                val yo = api.usuarioActual()
                if (yo is Resultado.Exito) {
                    sesion = sesion.copy(rol = yo.dato.rol, privilegios = yo.dato.privilegios.toSet(), nombre = yo.dato.nombre)
                    almacen.guardarSesion(sesion)
                }
                estado.establecer(sesion)
                Resultado.Exito(sesion)
            }
        }

    override suspend fun cerrarSesion() {
        almacen.borrar()
        estado.limpiar()
    }
}
