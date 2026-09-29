package br.com.anunciaai.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.anunciaai.R

// ── Sistema de design v3 (spec 29/09): tema escuro como base, UMA cor de destaque ──
// Nenhum elemento usa cor própria: destaque só em CTA, aba ativa, ícone conectado, status positivo.
val Destaque = Color(0xFF00C896)      // verde-menta
val CorFundo = Color(0xFF12151A)      // fundo base
val CorSuperficie = Color(0xFF1B1F26) // superfície elevada (cards, barra) — UM tom acima do fundo
val CorTexto = Color(0xFFF2F2F0)      // texto principal
val CorTextoSec = Color(0xFF8B909A)   // texto secundário/legenda
val CorNeutro = Color(0xFF4B505B)     // status neutro (desconectado, pendente)
val CorErro = Color(0xFFFF5470)       // erro

// Cores de marca reais — usadas APENAS no monograma da plataforma CONECTADA
val CorPlataforma = mapOf(
    "MERCADO_LIVRE" to Color(0xFFFFE600),
    "EBAY" to Color(0xFFE53238),
    "SHOPEE" to Color(0xFFEE4D2D),
    "OLX" to Color(0xFF9D6CFF),
    "FACEBOOK_MARKETPLACE" to Color(0xFF4C9BFF),
    "ENJOEI" to Color(0xFFFF6E9C)
)

private val EsquemaEscuro = darkColorScheme(
    primary = Destaque,
    onPrimary = Color(0xFF06231B),
    primaryContainer = Color(0xFF0E3D30),
    onPrimaryContainer = Destaque,
    secondary = Destaque,
    onSecondary = Color(0xFF06231B),
    secondaryContainer = Color(0xFF0E3D30),
    onSecondaryContainer = Destaque,
    tertiary = Destaque,
    onTertiary = Color(0xFF06231B),
    background = CorFundo,
    onBackground = CorTexto,
    surface = CorFundo,
    onSurface = CorTexto,
    surfaceVariant = CorSuperficie,
    onSurfaceVariant = CorTextoSec,
    surfaceContainerLowest = CorFundo,
    surfaceContainerLow = CorFundo,
    surfaceContainer = CorSuperficie,
    surfaceContainerHigh = CorSuperficie,
    surfaceContainerHighest = CorSuperficie,
    outline = CorNeutro,
    outlineVariant = CorSuperficie,   // divisor fino de 1px entre linhas de lista
    error = CorErro,
    onError = Color(0xFF2B0710),
    errorContainer = Color(0xFF3A1622),
    onErrorContainer = Color(0xFFFFB3C2)
)

// Tema claro recebe a MESMA identidade (o app é escuro por design — não há "modo claro" visual)
private val EsquemaClaro = lightColorScheme(
    primary = Destaque,
    onPrimary = Color(0xFF06231B),
    primaryContainer = Color(0xFF0E3D30),
    onPrimaryContainer = Destaque,
    secondary = Destaque,
    onSecondary = Color(0xFF06231B),
    secondaryContainer = Color(0xFF0E3D30),
    onSecondaryContainer = Destaque,
    tertiary = Destaque,
    onTertiary = Color(0xFF06231B),
    background = CorFundo,
    onBackground = CorTexto,
    surface = CorFundo,
    onSurface = CorTexto,
    surfaceVariant = CorSuperficie,
    onSurfaceVariant = CorTextoSec,
    surfaceContainerLowest = CorFundo,
    surfaceContainerLow = CorFundo,
    surfaceContainer = CorSuperficie,
    surfaceContainerHigh = CorSuperficie,
    surfaceContainerHighest = CorSuperficie,
    outline = CorNeutro,
    outlineVariant = CorSuperficie,
    error = CorErro,
    onError = Color(0xFF2B0710),
    errorContainer = Color(0xFF3A1622),
    onErrorContainer = Color(0xFFFFB3C2)
)

// Manrope: família única. 400 (corpo), 600 (subtítulo), 800 (título) — pesos da fonte variável.
@OptIn(androidx.compose.ui.text.ExperimentalTextApi::class)
val Manrope = FontFamily(
    Font(R.font.manrope, FontWeight.Normal,
        variationSettings = FontVariation.Settings(FontVariation.weight(400))),
    Font(R.font.manrope, FontWeight.Medium,
        variationSettings = FontVariation.Settings(FontVariation.weight(500))),
    Font(R.font.manrope, FontWeight.SemiBold,
        variationSettings = FontVariation.Settings(FontVariation.weight(600))),
    Font(R.font.manrope, FontWeight.Bold,
        variationSettings = FontVariation.Settings(FontVariation.weight(700))),
    Font(R.font.manrope, FontWeight.ExtraBold,
        variationSettings = FontVariation.Settings(FontVariation.weight(800)))
)

// Tipografia da spec: título de tela 28/800, título de linha 16/600, corpo 14/400 — sem caixa alta.
private val Tipografia = Typography(
    displaySmall = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.ExtraBold, fontSize = 34.sp),
    headlineLarge = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.ExtraBold, fontSize = 32.sp),
    headlineMedium = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.ExtraBold, fontSize = 30.sp),
    headlineSmall = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.ExtraBold, fontSize = 28.sp),
    titleLarge = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp),
    titleMedium = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.SemiBold, fontSize = 16.sp),
    titleSmall = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.SemiBold, fontSize = 14.sp),
    bodyLarge = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.Normal, fontSize = 16.sp),
    bodyMedium = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.Normal, fontSize = 14.sp),
    bodySmall = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.Normal, fontSize = 12.sp),
    labelLarge = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.SemiBold, fontSize = 14.sp),
    labelMedium = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.SemiBold, fontSize = 12.sp),
    labelSmall = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.Medium, fontSize = 11.sp)
)
// Raios por função (não um raio genérico em tudo)
private val Formas = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun AnunciaAITheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) EsquemaEscuro else EsquemaClaro,
        typography = Tipografia,
        shapes = Formas,
        content = content
    )
}
