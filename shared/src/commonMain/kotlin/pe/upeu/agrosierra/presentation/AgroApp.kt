package pe.upeu.agrosierra.presentation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import pe.upeu.agrosierra.presentation.navigation.NavegacionAgro
import pe.upeu.agrosierra.presentation.navigation.Navegador
import pe.upeu.agrosierra.presentation.sesion.EstadoRaiz
import pe.upeu.agrosierra.presentation.sesion.LoginScreen
import pe.upeu.agrosierra.presentation.sesion.RaizViewModel
import pe.upeu.agrosierra.presentation.theme.AgroTheme

/** Raiz de AgroCampo: decide entre login y area protegida segun la sesion. */
@Composable
fun AgroApp() {
    AgroTheme {
        val raiz = koinViewModel<RaizViewModel>()
        val navegador = koinInject<Navegador>()
        val avisos = koinInject<Avisos>()
        val snackbar = remember { SnackbarHostState() }
        val estado by raiz.estado.collectAsState()

        LaunchedEffect(avisos) {
            avisos.flujo.collect { aviso ->
                val r = snackbar.showSnackbar(
                    message = aviso.texto,
                    actionLabel = aviso.accion,
                    duration = if (aviso.accion != null) SnackbarDuration.Long else SnackbarDuration.Short,
                )
                if (r == SnackbarResult.ActionPerformed) aviso.alAccionar?.invoke()
            }
        }

        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Box(Modifier.fillMaxSize()) {
                when (estado) {
                    EstadoRaiz.Cargando -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                    EstadoRaiz.SinSesion -> LoginScreen()
                    is EstadoRaiz.ConSesion -> NavegacionAgro(navegador)
                }
                SnackbarHost(
                    snackbar,
                    Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 72.dp),
                )
            }
        }
    }
}
