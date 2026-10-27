package pe.upeu.agrosierra.domain.usecase

import pe.upeu.agrosierra.domain.model.ErrorApi
import pe.upeu.agrosierra.domain.model.Resultado
import pe.upeu.agrosierra.domain.model.Sesion
import pe.upeu.agrosierra.domain.repository.SesionRepository

class IniciarSesionUseCase(private val repo: SesionRepository) {
    suspend operator fun invoke(usuario: String, contrasena: String): Resultado<Sesion> {
        val campos = buildMap {
            if (usuario.isBlank()) put("usuario", "Ingresa tu usuario")
            if (contrasena.isBlank()) put("contrasena", "Ingresa tu contrasena")
        }
        if (campos.isNotEmpty()) return Resultado.Fallo(ErrorApi.Validacion("Completa los campos", campos))
        return repo.iniciarSesion(usuario.trim(), contrasena)
    }
}

class RestaurarSesionUseCase(private val repo: SesionRepository) {
    suspend operator fun invoke(): Sesion? = repo.restaurar()
}

class ObservarSesionUseCase(private val repo: SesionRepository) {
    operator fun invoke() = repo.sesion
}

class CerrarSesionUseCase(private val repo: SesionRepository) {
    suspend operator fun invoke() = repo.cerrarSesion()
}
