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

// ── Sistema de design v10 "AURORA TECH": azul-abissal + índigo elétrico ──
// Ruptura total com o verde-menta/cinza anterior a pedido do Lucas.
val Destaque = Color(0xFF7C6BFF)      // índigo elétrico
val DestaqueCiano = Color(0xFF4FD8EB) // ciano aurora (links/acentos secundários)
val CorFundo = Color(0xFF0A0E1A)      // azul profundo (não cinza!)
val CorSuperficie = Color(0xFF141B2E) // superfície azulada
val CorTexto = Color(0xFFEDEFF7)      // texto com leve azul
val CorTextoSec = Color(0xFF8A93AD)   // secundário azulado
val CorNeutro = Color(0xFF4B5570)     // neutro azulado
val CorErro = Color(0xFFFF5470)

// Cores de marca REAIS (v4) — badge de cada plataforma em toda a lista, Mensagens e selos "publicado em"
val CorPlataforma = mapOf(
    "MERCADO_LIVRE" to Color(0xFFFFE600),
    "EBAY" to Color(0xFF0064D2), // badge eBay é multicor (ver IconePlataforma); fallback azul
    "SHOPEE" to Color(0xFFEE4D2D),
    "OLX" to Color(0xFF7C1FD6),
    "FACEBOOK_MARKETPLACE" to Color(0xFF1877F2),
    "ENJOEI" to Color(0xFFFF2D78)
)

// Detalhe azul do ML sobre o amarelo; as demais usam branco
val CorTextoMarca = mapOf(
    "MERCADO_LIVRE" to Color(0xFF2D3277)
)

// Fim do gradiente do card de resumo (índigo → violeta profundo)
val Petroleo = Color(0xFF2E1B6B)

private val EsquemaEscuro = darkColorScheme(
    primary = Destaque,
    onPrimary = Color(0xFF12092B),
    primaryContainer = Color(0xFF3A2E8C),
    onPrimaryContainer = Destaque,
    secondary = Destaque,
    onSecondary = Color(0xFF12092B),
    secondaryContainer = Color(0xFF3A2E8C),
    onSecondaryContainer = Destaque,
    tertiary = Destaque,
    onTertiary = Color(0xFF12092B),
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
    onPrimary = Color(0xFF12092B),
    primaryContainer = Color(0xFF3A2E8C),
    onPrimaryContainer = Destaque,
    secondary = Destaque,
    onSecondary = Color(0xFF12092B),
    secondaryContainer = Color(0xFF3A2E8C),
    onSecondaryContainer = Destaque,
    tertiary = Destaque,
    onTertiary = Color(0xFF12092B),
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

// Manrope em PESOS ESTÁTICOS (v4.1): a fonte variável + variationSettings tinha bug de
// shaping no Android que quebrava palavras com "L" maiúsculo ("Livros" → "l ivros").
// Cada peso é um arquivo próprio em res/font — sem variationSettings.
val Manrope = FontFamily(
    Font(R.font.manrope_regular, FontWeight.Normal),
    Font(R.font.manrope_medium, FontWeight.Medium),
    Font(R.font.manrope_semibold, FontWeight.SemiBold),
    Font(R.font.manrope_bold, FontWeight.Bold),
    Font(R.font.manrope_extrabold, FontWeight.ExtraBold)
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
