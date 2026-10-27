package pe.upeu.agrosierra

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module
import pe.upeu.agrosierra.data.local.AlmacenSeguro
import pe.upeu.agrosierra.data.local.BaseLocal
import pe.upeu.agrosierra.data.remote.ApiConfig
import pe.upeu.agrosierra.data.remote.ConfigPlataforma
import pe.upeu.agrosierra.domain.repository.MonitorConexion

/** Dependencias propias de Android: motor HTTP, almacen seguro, base local y estado de la red. */
val platformModule = module {
    single<HttpClientEngine> { OkHttp.create() }
    single { ConfigPlataforma(urlBase = ApiConfig.URL_BASE, depurar = BuildConfig.DEBUG) }
    single<AlmacenSeguro> { AlmacenSeguroAndroid(androidContext()) }
    single<BaseLocal> { BaseLocalAndroid(androidContext()) }
    single<MonitorConexion> { MonitorConexionAndroid(androidContext()) }
}

/** Observa la conectividad con ConnectivityManager para sincronizar al recuperar la senal. */
class MonitorConexionAndroid(context: Context) : MonitorConexion {
    private val cm = context.getSystemService(ConnectivityManager::class.java)
    private val redes = mutableSetOf<Network>()
    private val _conectado = MutableStateFlow(hayRed())
    override val conectado: StateFlow<Boolean> = _conectado.asStateFlow()

    init {
        val pedido = NetworkRequest.Builder().addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET).build()
        cm.registerNetworkCallback(pedido, object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                synchronized(redes) { redes += network }
                _conectado.value = true
            }

            override fun onLost(network: Network) {
                val quedan = synchronized(redes) { redes -= network; redes.isNotEmpty() }
                _conectado.value = quedan || hayRed()
            }
        })
    }

    private fun hayRed(): Boolean =
        cm.getNetworkCapabilities(cm.activeNetwork)?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
}
