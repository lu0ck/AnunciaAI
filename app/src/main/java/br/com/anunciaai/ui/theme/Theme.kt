package br.com.anunciaai.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import br.com.anunciaai.R

// Paleta v2: verde profundo (valor/dinheiro) + papel/tinta + mostarda com moderação
private val Papel = Color(0xFFF7F5F1)
private val Tinta = Color(0xFF14171C)
private val Verde = Color(0xFF1F7A5C)
private val VerdeClaro = Color(0xFF4C9E80)
private val Mostarda = Color(0xFFD9A441)
private val ErroVermelho = Color(0xFFB33A3A)
private val TextoTinta = Color(0xFF22201D)

// Dots por plataforma (indicador inline)
val CorPlataforma = mapOf(
    "MERCADO_LIVRE" to Color(0xFFFFE600),
    "EBAY" to Color(0xFFE53238),
    "SHOPEE" to Color(0xFFEE4D2D),
    "OLX" to Color(0xFF6E0AD6),
    "FACEBOOK_MARKETPLACE" to Color(0xFF1877F2),
    "ENJOEI" to Color(0xFFFF4081)
)

private val EsquemaClaro = lightColorScheme(
    primary = Verde,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD8EAE0),
    onPrimaryContainer = Color(0xFF0B3D2C),
    secondary = Mostarda,
    onSecondary = Color(0xFF2E2410),
    secondaryContainer = Color(0xFFF5EBD3),
    onSecondaryContainer = Color(0xFF4A3A12),
    tertiary = Verde,
    background = Papel,
    onBackground = TextoTinta,
    surface = Papel,
    onSurface = TextoTinta,
    surfaceVariant = Color(0xFFECE8E0),
    onSurfaceVariant = Color(0xFF5C574F),
    outline = Color(0xFFC9C2BA),
    error = ErroVermelho,
    onError = Color.White,
    errorContainer = Color(0xFFF5DBD8),
    onErrorContainer = Color(0xFF4A1512)
)

private val EsquemaEscuro = darkColorScheme(
    primary = VerdeClaro,
    onPrimary = Color(0xFF0B3D2C),
    primaryContainer = Color(0xFF1C4A38),
    onPrimaryContainer = Color(0xFFC9E8DC),
    secondary = Mostarda,
    onSecondary = Color(0xFF2E2410),
    secondaryContainer = Color(0xFF52431E),
    onSecondaryContainer = Color(0xFFF0DDB0),
    tertiary = VerdeClaro,
    background = Tinta,
    onBackground = Color(0xFFE8E4E0),
    surface = Color(0xFF1B1F25),
    onSurface = Color(0xFFE8E4E0),
    surfaceVariant = Color(0xFF262B32),
    onSurfaceVariant = Color(0xFFA8A29A),
    outline = Color(0xFF4A4F57),
    error = Color(0xFFE07B7B),
    onError = Color(0xFF3D0F0F),
    errorContainer = Color(0xFF52201F),
    onErrorContainer = Color(0xFFF5C9C2)
)

// Manrope: família ÚNICA, pesos instanciados da fonte variável (minSdk 26)
@OptIn(androidx.compose.ui.text.ExperimentalTextApi::class)
val Manrope = FontFamily(
    Font(R.font.manrope, FontWeight.Normal,
        variationSettings = FontVariation.Settings(FontVariation.weight(400))),
    Font(R.font.manrope, FontWeight.Medium,
        variationSettings = FontVariation.Settings(FontVariation.weight(500))),
    Font(R.font.manrope, FontWeight.SemiBold,
        variationSettings = FontVariation.Settings(FontVariation.weight(600))),
    Font(R.font.manrope, FontWeight.Bold,
        variationSettings = FontVariation.Settings(FontVariation.weight(700)))
)

private fun tipografia(base: Typography) = base.copy(
    displaySmall = base.displaySmall.copy(fontFamily = Manrope, fontWeight = FontWeight.Bold),
    headlineLarge = base.headlineLarge.copy(fontFamily = Manrope, fontWeight = FontWeight.Bold),
    headlineMedium = base.headlineMedium.copy(fontFamily = Manrope, fontWeight = FontWeight.Bold),
    headlineSmall = base.headlineSmall.copy(fontFamily = Manrope, fontWeight = FontWeight.SemiBold),
    titleLarge = base.titleLarge.copy(fontFamily = Manrope, fontWeight = FontWeight.SemiBold),
    titleMedium = base.titleMedium.copy(fontFamily = Manrope, fontWeight = FontWeight.SemiBold),
    titleSmall = base.titleSmall.copy(fontFamily = Manrope, fontWeight = FontWeight.Medium),
    bodyLarge = base.bodyLarge.copy(fontFamily = Manrope, fontWeight = FontWeight.Normal),
    bodyMedium = base.bodyMedium.copy(fontFamily = Manrope, fontWeight = FontWeight.Normal),
    bodySmall = base.bodySmall.copy(fontFamily = Manrope, fontWeight = FontWeight.Normal),
    labelLarge = base.labelLarge.copy(fontFamily = Manrope, fontWeight = FontWeight.Medium),
    labelMedium = base.labelMedium.copy(fontFamily = Manrope, fontWeight = FontWeight.Medium),
    labelSmall = base.labelSmall.copy(fontFamily = Manrope, fontWeight = FontWeight.Medium)
)

// Raios de borda distintos por papel (não um raio único genérico)
private val Formas = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun AnunciaAITheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) EsquemaEscuro else EsquemaClaro,
        typography = tipografia(Typography()),
        shapes = Formas,
        content = content
    )
}
