package pe.upeu.agrosierra

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import pe.upeu.agrosierra.data.local.BaseLocal
import pe.upeu.agrosierra.db.AgroDatabase

/** Controlador de SQLDelight para Android (archivo agrosierra.db). */
class BaseLocalAndroid(private val context: Context) : BaseLocal {
    override fun crearDriver(): SqlDriver = AndroidSqliteDriver(AgroDatabase.Schema, context, "agrosierra.db")
}
