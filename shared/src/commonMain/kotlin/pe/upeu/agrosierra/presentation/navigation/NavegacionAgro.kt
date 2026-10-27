package pe.upeu.agrosierra.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.key
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import pe.upeu.agrosierra.presentation.detalle.DetalleScreen
import pe.upeu.agrosierra.presentation.detalle.FormularioLaborScreen
import pe.upeu.agrosierra.presentation.listado.ListadoScreen
import pe.upeu.agrosierra.presentation.perfil.PerfilScreen

/** Area protegida: solo se compone cuando hay sesion. */
@Composable
fun NavegacionAgro(navegador: Navegador) {
    val entrada = navegador.actual
    key(entrada) {
        CompositionLocalProvider(LocalViewModelStoreOwner provides entrada) {
            when (val destino = entrada.destino) {
                Destino.Lotes -> ListadoScreen(
                    alAbrirLote = { navegador.ir(Destino.DetalleLote(it)) },
                    alAbrirPerfil = { navegador.ir(Destino.Perfil) },
                )
                is Destino.DetalleLote -> DetalleScreen(
                    loteId = destino.loteId,
                    alVolver = { navegador.atras() },
                    alRegistrar = { navegador.ir(Destino.FormularioLabor(destino.loteId)) },
                    alEditar = { navegador.ir(Destino.FormularioLabor(destino.loteId, it)) },
                )
                is Destino.FormularioLabor -> FormularioLaborScreen(
                    destino = destino,
                    alTerminar = { navegador.atras() },
                )
                Destino.Perfil -> PerfilScreen(alVolver = { navegador.atras() })
            }
        }
    }
}
