package pe.upeu.agrosierra

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import pe.upeu.agrosierra.data.local.AlmacenSeguro
import pe.upeu.agrosierra.domain.model.Sesion

/**
 * Token y sesion en preferencias CIFRADAS (AES-256). La clave maestra vive en el
 * Android Keystore (almacen de claves del sistema), no en el archivo.
 * En preferencias sin cifrar, cualquiera con acceso al archivo (root, backup) leeria el token.
 */
class AlmacenSeguroAndroid(context: Context) : AlmacenSeguro {

    private val prefs: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        "agro_sesion_segura",
        MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )

    override fun guardarSesion(sesion: Sesion) = prefs.edit {
        putString(TOKEN, sesion.token)
        putString(USUARIO, sesion.usuario)
        putString(NOMBRE, sesion.nombre)
        putString(ROL, sesion.rol)
        putStringSet(PRIVILEGIOS, sesion.privilegios)
        putLong(EXPIRA, sesion.expiraEn)
    }

    override fun leerSesion(): Sesion? {
        val token = prefs.getString(TOKEN, null) ?: return null
        return Sesion(
            token = token,
            usuario = prefs.getString(USUARIO, "").orEmpty(),
            rol = prefs.getString(ROL, "").orEmpty(),
            privilegios = prefs.getStringSet(PRIVILEGIOS, emptySet()).orEmpty().toSet(),
            nombre = prefs.getString(NOMBRE, "").orEmpty(),
            expiraEn = prefs.getLong(EXPIRA, 0),
        )
    }

    override fun leerToken(): String? = prefs.getString(TOKEN, null)

    override fun borrar() = prefs.edit { clear() }

    private companion object {
        const val TOKEN = "token"
        const val USUARIO = "usuario"
        const val NOMBRE = "nombre"
        const val ROL = "rol"
        const val PRIVILEGIOS = "privilegios"
        const val EXPIRA = "expira_en"
    }
}
