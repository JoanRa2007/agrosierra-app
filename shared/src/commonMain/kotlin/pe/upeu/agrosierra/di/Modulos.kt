package pe.upeu.agrosierra.di

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.module
import pe.upeu.agrosierra.data.local.BaseLocal
import pe.upeu.agrosierra.data.local.EstadoSesion
import pe.upeu.agrosierra.data.local.abrir
import pe.upeu.agrosierra.data.remote.AgroApi
import pe.upeu.agrosierra.data.remote.crearClienteHttp
import pe.upeu.agrosierra.data.repository.LaborRepositoryImpl
import pe.upeu.agrosierra.data.repository.LoteRepositoryImpl
import pe.upeu.agrosierra.data.repository.SesionRepositoryImpl
import pe.upeu.agrosierra.domain.repository.LaborRepository
import pe.upeu.agrosierra.domain.repository.LoteRepository
import pe.upeu.agrosierra.domain.repository.SesionRepository
import pe.upeu.agrosierra.domain.usecase.CerrarCampaniaUseCase
import pe.upeu.agrosierra.domain.usecase.CerrarSesionUseCase
import pe.upeu.agrosierra.domain.usecase.EliminarLaborUseCase
import pe.upeu.agrosierra.domain.usecase.GuardarLaborUseCase
import pe.upeu.agrosierra.domain.usecase.IniciarSesionUseCase
import pe.upeu.agrosierra.domain.usecase.ObservarLaboresUseCase
import pe.upeu.agrosierra.domain.usecase.ObservarLoteUseCase
import pe.upeu.agrosierra.domain.usecase.ObservarLotesUseCase
import pe.upeu.agrosierra.domain.usecase.ObservarSesionUseCase
import pe.upeu.agrosierra.domain.usecase.ObservarTotalPendientesUseCase
import pe.upeu.agrosierra.domain.usecase.ObservarUltimaSincronizacionUseCase
import pe.upeu.agrosierra.domain.usecase.ObservarVallesUseCase
import pe.upeu.agrosierra.domain.usecase.ObtenerLaborUseCase
import pe.upeu.agrosierra.domain.usecase.RestaurarSesionUseCase
import pe.upeu.agrosierra.domain.usecase.SincronizarUseCase
import pe.upeu.agrosierra.presentation.Avisos
import pe.upeu.agrosierra.presentation.detalle.DetalleViewModel
import pe.upeu.agrosierra.presentation.detalle.FormularioLaborViewModel
import pe.upeu.agrosierra.presentation.listado.ListadoViewModel
import pe.upeu.agrosierra.presentation.navigation.Navegador
import pe.upeu.agrosierra.presentation.perfil.PerfilViewModel
import pe.upeu.agrosierra.presentation.sesion.LoginViewModel
import pe.upeu.agrosierra.presentation.sesion.RaizViewModel

val CALIFICADOR_IO = named("io")

/** Red: UNA sola instancia de HttpClient para toda la app. */
val redModule = module {
    single { EstadoSesion() }
    single { crearClienteHttp(motor = get(), almacen = get(), estadoSesion = get(), config = get()) }
    single { AgroApi(get()) }
}

val dataModule = module {
    single(CALIFICADOR_IO) { Dispatchers.IO }
    single { get<BaseLocal>().abrir() }
    single<SesionRepository> { SesionRepositoryImpl(get(), get(), get()) }
    single<LoteRepository> { LoteRepositoryImpl(get(), get(), get(CALIFICADOR_IO)) }
    single<LaborRepository> { LaborRepositoryImpl(get(), get(), get(CALIFICADOR_IO)) }
}

val domainModule = module {
    factory { IniciarSesionUseCase(get()) }
    factory { RestaurarSesionUseCase(get()) }
    factory { ObservarSesionUseCase(get()) }
    factory { CerrarSesionUseCase(get()) }
    factory { ObservarLotesUseCase(get()) }
    factory { ObservarLoteUseCase(get()) }
    factory { ObservarVallesUseCase(get()) }
    factory { ObservarUltimaSincronizacionUseCase(get()) }
    factory { ObservarTotalPendientesUseCase(get()) }
    factory { CerrarCampaniaUseCase(get()) }
    factory { ObservarLaboresUseCase(get()) }
    factory { ObtenerLaborUseCase(get()) }
    factory { GuardarLaborUseCase(get()) }
    factory { EliminarLaborUseCase(get()) }
    factory { SincronizarUseCase(get(), get()) }
}

val presentationModule = module {
    single { Navegador() }
    single { Avisos() }
    viewModel { RaizViewModel(get(), get(), get()) }
    viewModel { LoginViewModel(get()) }
    viewModel { ListadoViewModel(get(), get(), get(), get(), get(), get(), get()) }
    viewModel { p -> DetalleViewModel(p.get(), get(), get(), get(), get(), get(), get()) }
    viewModel { p -> FormularioLaborViewModel(p.get(), get(), get(), get(), get(), get()) }
    viewModel { PerfilViewModel(get(), get()) }
}

/** Arranque de Koin; [plataforma] es el platformModule que aporta cada plataforma. */
fun iniciarKoin(plataforma: Module, declaracion: KoinAppDeclaration = {}) = startKoin {
    declaracion()
    modules(plataforma, redModule, dataModule, domainModule, presentationModule)
}
