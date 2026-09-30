package br.com.anunciaai.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.BottomAppBarDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import br.com.anunciaai.ui.theme.CorSuperficie
import br.com.anunciaai.ui.theme.CorTextoSec
import br.com.anunciaai.ui.theme.Destaque

/**
 * ══════════════════════════════════════════════════════════════════
 *  PILAR 1 — "Doc" Premium com entalhe circular (v8.0)
 * ══════════════════════════════════════════════════════════════════
 *  BottomAppBar OFICIAL do Material 3 com SHAPE customizada que recorta
 *  um círculo central (NotchShape) para o FAB "Vender" se encaixar
 *  perfeitamente no centro — ancorado pelo Scaffold
 *  (floatingActionButtonPosition = FabPosition.Center).
 *
 *  • A geometria é uma Shape de verdade (criaOutline) — o layout do
 *    BottomAppBar respeita, sem Canvas flutuante (causa do crash v6.0).
 *  • Itens com animateFloatAsState: ativo cresce 15% (spring bouncy).
 *  • 4 itens: Início | Vitrine | [entalhe/FAB] | Mensagens | Perfil.
 */
@Composable
fun BarraDockNova(atual: String, onNav: (String) -> Unit, modifier: Modifier = Modifier) {
    BottomAppBar(
        // PILAR 1: entalhe circular central recortando a barra (o FAB do
        // Scaffold, FabPosition.Center, se encaixa perfeitamente no círculo)
        modifier = modifier.clip(NotchShape(30.dp)),
        containerColor = CorSuperficie,
        contentColor = CorTextoSec,
        tonalElevation = 0.dp,
        windowInsets = BottomAppBarDefaults.windowInsets
    ) {
        ItemDock(Rotas.LISTA, atual, "Início", Icons.Default.Home, onNav, Modifier.weight(1f))
        ItemDock(Rotas.VITRINE, atual, "Vitrine", Icons.Default.GridView, onNav, Modifier.weight(1f))
        Spacer(Modifier.weight(1f)) // entalhe do FAB central
        ItemDock(Rotas.MENSAGENS, atual, "Mensagens", Icons.Default.ChatBubbleOutline, onNav, Modifier.weight(1f))
        ItemDock(Rotas.PERFIL, atual, "Perfil", Icons.Default.Person, onNav, Modifier.weight(1f))
    }
}

/**
 * Entalhe circular central (Notch): recorta um semicírculo no topo da barra.
 * O FAB do Scaffold (FabPosition.Center) se encaixa nesse recorte.
 */
data class NotchShape(private val raio: Dp) : Shape {
    override fun createOutline(size: androidx.compose.ui.geometry.Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val r = with(density) { raio.toPx() }
        val cx = size.width / 2f
        val path = Path().apply {
            // contorno superior com o semicírculo do entalhe
            moveTo(0f, 0f)
            lineTo(cx - r - 16f, 0f)
            // curva de entrada suave
            cubicTo(cx - r, 0f, cx - r, 0f, cx - r, 10f)
            // semicírculo (arc para cima)
            arcTo(
                rect = androidx.compose.ui.geometry.Rect(cx - r, -r, cx + r, r),
                startAngleDegrees = 180f,
                sweepAngleDegrees = 180f,
                forceMoveTo = false
            )
            // curva de saída
            cubicTo(cx + r, 0f, cx + r, 0f, cx + r + 16f, 0f)
            lineTo(size.width, 0f)
            lineTo(size.width, size.height)
            lineTo(0f, size.height)
            close()
        }
        return Outline.Generic(path)
    }
}

/** Item do dock: ícone + label; ativo cresce 15% com spring e muda cor em 300ms. */
@Composable
private fun ItemDock(
    rota: String,
    atual: String,
    rotulo: String,
    icone: ImageVector,
    onNav: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val ativo = rota == atual
    val escala by animateFloatAsState(
        targetValue = if (ativo) 1.15f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "escala_$rotulo"
    )
    val cor by animateColorAsState(
        targetValue = if (ativo) Destaque else CorTextoSec,
        animationSpec = tween(300),
        label = "cor_$rotulo"
    )
    Column(
        modifier
            .clickable { onNav(rota) }
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            icone,
            contentDescription = rotulo,
            tint = cor,
            modifier = Modifier.size(24.dp).scale(escala)
        )
        Text(
            rotulo,
            style = MaterialTheme.typography.labelMedium,
            color = cor,
            maxLines = 1
        )
    }
}
