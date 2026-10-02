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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.anunciaai.R

// ═════════════════════════════════════════════════════════════════
//  v11.0 — REBOOT VISUAL (padrão Figma "Online Bike Shopping App")
//  Claro, desportivo, premium. Adeus tema escuro/índigo.
// ═════════════════════════════════════════════════════════════════
val VerdeNeon = Color(0xFFA3E635)   // lime neon — FAB, chips ativos, destaques de superfície
val Destaque = Color(0xFF65A30D)    // lime-600 — textos/links/ícones sobre fundo claro (legível)
val CapsulaEscura = Color(0xFF1E1E24) // cápsula da navegação flutuante
val CorFundo = Color(0xFFF4F4F7)    // fundo claro (negative space)
val CorSuperficie = Color(0xFFFFFFFF) // cards brancos
val CorTexto = Color(0xFF17171F)    // quase-preto premium
val CorTextoSec = Color(0xFF8E8E9C)
val CorNeutro = Color(0xFFDCDCE6)  // bordas finas
val CorErro = Color(0xFFFF3B30)
val Petroleo = Color(0xFF3F6212)    // fim do gradiente (lime profundo)

// Cores de marca REAIS — badges por plataforma
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

private val EsquemaClaroBike = lightColorScheme(
    primary = VerdeNeon,
    onPrimary = Color(0xFF101601),
    primaryContainer = Color(0xFFECFCCB),
    onPrimaryContainer = Color(0xFF365314),
    secondary = Destaque,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFECFCCB),
    onSecondaryContainer = Color(0xFF365314),
    tertiary = Destaque,
    onTertiary = Color.White,
    background = CorFundo,
    onBackground = CorTexto,
    surface = CorFundo,
    onSurface = CorTexto,
    surfaceVariant = Color(0xFFE9E9F0),
    onSurfaceVariant = CorTextoSec,
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFFAFAFC),
    surfaceContainer = CorSuperficie,          // cards brancos
    surfaceContainerHigh = Color(0xFFF1F1F5),
    surfaceContainerHighest = Color(0xFFE9E9F0),
    outline = CorNeutro,
    outlineVariant = Color(0xFFE7E7EE),
    error = CorErro,
    onError = Color.White,
    errorContainer = Color(0xFFFFE4E1),
    onErrorContainer = Color(0xFF7A1B12)
)

// O app é CLARO por design agora; o "escuro" recebe o mesmo mapa (nunca herdou tema do sistema)
private val EsquemaEscuro = EsquemaClaroBike

// Manrope em pesos ESTÁTICOS (fix do bug de shaping "l ivros")
val Manrope = FontFamily(
    Font(R.font.manrope_regular, FontWeight.Normal),
    Font(R.font.manrope_medium, FontWeight.Medium),
    Font(R.font.manrope_semibold, FontWeight.SemiBold),
    Font(R.font.manrope_bold, FontWeight.Bold),
    Font(R.font.manrope_extrabold, FontWeight.ExtraBold)
)

// Tipografia bike-shop: preços GIGANTES em ExtraBold; corpo limpo
private val Tipografia = Typography(
    displaySmall = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.ExtraBold, fontSize = 40.sp),
    headlineLarge = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.ExtraBold, fontSize = 34.sp),
    headlineMedium = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.ExtraBold, fontSize = 30.sp),
    headlineSmall = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.ExtraBold, fontSize = 26.sp),
    titleLarge = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp),
    titleMedium = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.Bold, fontSize = 16.sp),
    titleSmall = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.SemiBold, fontSize = 14.sp),
    bodyLarge = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.Normal, fontSize = 16.sp),
    bodyMedium = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.Normal, fontSize = 14.sp),
    bodySmall = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.Normal, fontSize = 12.sp),
    labelLarge = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.Bold, fontSize = 14.sp),
    labelMedium = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.SemiBold, fontSize = 12.sp),
    labelSmall = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
)

private val Formas = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

@Composable
fun AnunciaAITheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = EsquemaClaroBike,
        typography = Tipografia,
        shapes = Formas,
        content = content
    )
}
