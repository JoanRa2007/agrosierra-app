package pe.upeu.agrosierra.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Verde = Color(0xFF2E7D32)
private val VerdeClaro = Color(0xFFA5D6A7)
private val Tierra = Color(0xFF8D6E63)
private val Cosecha = Color(0xFFF9A825)

private val Claro = lightColorScheme(
    primary = Verde,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFC8E6C9),
    onPrimaryContainer = Color(0xFF0B3D0F),
    secondary = Tierra,
    tertiary = Cosecha,
    tertiaryContainer = Color(0xFFFFF3C4),
)

private val Oscuro = darkColorScheme(
    primary = VerdeClaro,
    onPrimary = Color(0xFF0B3D0F),
    primaryContainer = Color(0xFF1B5E20),
    secondary = Color(0xFFBCAAA4),
    tertiary = Color(0xFFFFD54F),
)

@Composable
fun AgroTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (isSystemInDarkTheme()) Oscuro else Claro, content = content)
}
