package pe.upeu.agrosierra.presentation.sesion

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pe.upeu.agrosierra.domain.model.ErrorApi
import pe.upeu.agrosierra.domain.model.Resultado
import pe.upeu.agrosierra.domain.usecase.IniciarSesionUseCase

data class LoginUiState(
    val usuario: String = "",
    val contrasena: String = "",
    val cargando: Boolean = false,
    val errorUsuario: String? = null,
    val errorContrasena: String? = null,
    val mensaje: String? = null,
)

class LoginViewModel(private val iniciarSesion: IniciarSesionUseCase) : ViewModel() {
    private val _estado = MutableStateFlow(LoginUiState())
    val estado: StateFlow<LoginUiState> = _estado.asStateFlow()

    fun onUsuario(valor: String) = _estado.update { it.copy(usuario = valor, errorUsuario = null, mensaje = null) }
    fun onContrasena(valor: String) = _estado.update { it.copy(contrasena = valor, errorContrasena = null, mensaje = null) }

    fun ingresar() {
        val actual = _estado.value
        if (actual.cargando) return
        _estado.update { it.copy(cargando = true, mensaje = null) }
        viewModelScope.launch {
            when (val r = iniciarSesion(actual.usuario, actual.contrasena)) {
                is Resultado.Exito -> _estado.value = LoginUiState()
                is Resultado.Fallo -> _estado.update { s ->
                    val e = r.error
                    when (e) {
                        is ErrorApi.Validacion -> s.copy(
                            cargando = false,
                            errorUsuario = e.campos["usuario"],
                            errorContrasena = e.campos["contrasena"],
                        )
                        // Mensajes DISTINTOS para credenciales incorrectas y para falta de conexion.
                        is ErrorApi.NoAutorizado -> s.copy(cargando = false, mensaje = "Usuario o contrasena incorrectos")
                        ErrorApi.SinConexion, ErrorApi.TiempoAgotado ->
                            s.copy(cargando = false, mensaje = "No hay conexion con el servidor. Revisa tu red e intenta de nuevo")
                        else -> s.copy(cargando = false, mensaje = e.message)
                    }
                }
            }
        }
    }
}
