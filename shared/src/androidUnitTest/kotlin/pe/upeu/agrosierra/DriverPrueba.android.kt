package pe.upeu.agrosierra

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import pe.upeu.agrosierra.db.AgroDatabase

/** Base SQLite EN MEMORIA para las pruebas (se ejecutan en la JVM local). */
actual fun crearDriverPrueba(): SqlDriver =
    JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY).also { AgroDatabase.Schema.create(it) }
