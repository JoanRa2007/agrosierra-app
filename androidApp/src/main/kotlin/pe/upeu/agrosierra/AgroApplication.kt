package pe.upeu.agrosierra

import android.app.Application
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.logger.Level
import pe.upeu.agrosierra.di.iniciarKoin

/** AgroCampo - Autor: Joan Rodriguez. */
class AgroApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        iniciarKoin(platformModule) {
            androidLogger(if (BuildConfig.DEBUG) Level.INFO else Level.NONE)
            androidContext(this@AgroApplication)
        }
    }
}
