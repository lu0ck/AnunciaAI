package br.com.anunciaai.ui.theme

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * PILAR 2 — Neon Glassmorphism: brilho verde-menta suave (10% de opacidade)
 * emanando por trás do card — desenhado ANTES do conteúdo, com gradientes
 * radiais discretos nas diagonais.
 */
fun Modifier.brilhoNeon(cor: Color = Destaque): Modifier = this.drawBehind {
    // glow radial sutil atrás do card (10% de opacidade)
    val glow = cor.copy(alpha = 0.10f)
    drawCircle(
        color = glow,
        radius = size.maxDimension * 0.75f,
        center = androidx.compose.ui.geometry.Offset(size.width * 0.85f, size.height * 0.15f)
    )
    drawCircle(
        color = glow.copy(alpha = 0.06f),
        radius = size.maxDimension * 0.6f,
        center = androidx.compose.ui.geometry.Offset(size.width * 0.1f, size.height * 0.9f)
    )
}

/** Fundo com gradiente escuro sutil (substitui cinza liso). */
fun Modifier.fundoGradienteEscuro(
    topo: Color = Color(0xFF161A20),
    base: Color = CorFundo
): Modifier = this.drawBehind {
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(topo, base),
            startY = 0f,
            endY = size.height
        )
    )
}
