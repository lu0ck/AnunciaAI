package br.com.anunciaai.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.anunciaai.R

// ═════════════════════════════════════════════════════════════════
//  v12.0 — DESIGN TOKENS (spec de reescrita total 03/10)
//  Tema escuro de volta: fundo #12151A, superfície #1B1F26,
//  destaque ÚNICO #00C896. Manrope 400/600/800.
// ═════════════════════════════════════════════!!!!!!!!════════════
val CorFundo = Color(0xFF12151A)
val CorSuperficie = Color(0xFF1B1F26)
val Destaque = Color(0xFF00C896)
val CorTexto = Color(0xFFF2F2F0)
val CorTextoSec = Color(0xFF8B909A)
val CorNeutro = Color(0xFF23272E)
val CorErro = Color(0xFFFF5470)
val Petroleo = Color(0xFF0A5C6E)
val VerdePositivo = Color(0xFF00A97A)

// Cores de marca REAIS — só em ícones/badges de plataforma
val CorPlataforma = mapOf(
    "MERCADO_LIVRE" to Color(0xFFFFE600),
    "EBAY" to Color(0xFF0064D2),
    "SHOPEE" to Color(0xFFEE4D2D),
    "OLX" to Color(0xFF7C1FD6),
    "FACEBOOK_MARKETPLACE" to Color(0xFF1877F2),
    "ENJOEI" to Color(0xFFFF2D78)
)
val CorTextoMarca = mapOf(
    "MERCADO_LIVRE" to Color(0xFF2D3277)
)

// Glassmorphism da cápsula
val CapsulaEscura = Color(0xFF1E1E24)

private val EsquemaEscuro = darkColorScheme(
    primary = Destaque,
    onPrimary = Color(0xFF04150F),
    secondary = Destaque,
    onSecondary = Color(0xFF04150F),
    background = CorFundo,
    onBackground = CorTexto,
    surface = CorSuperficie,
    onSurface = CorTexto,
    surfaceVariant = CorSuperficie,
    onSurfaceVariant = CorTextoSec,
    surfaceContainer = CorSuperficie,
    surfaceContainerHigh = Color(0xFF232830),
    surfaceContainerHighest = Color(0xFF2A2F3A),
    outline = Color(0xFF3A3F4B),
    outlineVariant = Color(0xFF23272E),
    error = CorErro,
    onError = Color.White
)

// Manrope pesos 400/600/800 (estáticos — fix do bug de shaping "l ivros")
val Manrope = FontFamily(
    Font(R.font.manrope_regular, FontWeight.Normal),
    Font(R.font.manrope_semibold, FontWeight.SemiBold),
    Font(R.font.manrope_extrabold, FontWeight.ExtraBold)
)

// Tipografia: título de tela 28/800, título de card 16/600, corpo 14/400
private val Tipografia = Typography(
    displaySmall = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.ExtraBold, fontSize = 34.sp),
    headlineLarge = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.ExtraBold, fontSize = 28.sp),
    headlineMedium = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.ExtraBold, fontSize = 24.sp),
    headlineSmall = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp),
    titleLarge = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp),
    titleMedium = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.SemiBold, fontSize = 16.sp),
    titleSmall = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.SemiBold, fontSize = 14.sp),
    bodyLarge = TextStyle(fontFamily = Manrope, fontSize = 16.sp, fontWeight = FontWeight.Normal),
    bodyMedium = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.Normal, fontSize = 14.sp),
    bodySmall = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.Normal, fontSize = 12.sp),
    labelLarge = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.SemiBold, fontSize = 14.sp),
    labelMedium = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.SemiBold, fontSize = 12.sp),
    labelSmall = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
)

private val Formas = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

@Composable
fun AnunciaAITheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = EsquemaEscuro,
        typography = Tipografia,
        shapes = Formas,
        content = content
    )
}
