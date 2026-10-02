package br.com.anunciaai.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import br.com.anunciaai.ui.theme.CorSuperficie
import br.com.anunciaai.ui.theme.CorTextoSec
import br.com.anunciaai.ui.theme.Destaque

/**
 * v9.0 — Dock com 5 itens: Início | Vitrine | [entalhe/FAB] | Chat | Config.
 * Config absorve as Conexões. Badge de não-lidas no Chat quando há perguntas.
 * Toque com micro-feedback de escala (press = 0.9, solta = spring de volta).
 */
@Composable
fun BarraDockNova(
    atual: String,
    onNav: (String) -> Unit,
    naoLidas: Int = 0,
    modifier: Modifier = Modifier
) {
    androidx.compose.material3.BottomAppBar(
        modifier = modifier.clip(NotchShape(30.dp)),
        containerColor = CorSuperficie,
        contentColor = CorTextoSec,
        tonalElevation = 0.dp
    ) {
        ItemDock(Rotas.LISTA, atual, "Início", Icons.Default.Home, onNav, Modifier.weight(1f))
        ItemDock(Rotas.VITRINE, atual, "Vitrine", Icons.Default.GridView, onNav, Modifier.weight(1f))
        Spacer(Modifier.weight(1f))
        ItemDock(Rotas.MENSAGENS, atual, "Chat", Icons.Default.ChatBubbleOutline, onNav, Modifier.weight(1f), badge = naoLidas)
        ItemDock(Rotas.CONFIG, atual, "Config", Icons.Outlined.Settings, onNav, Modifier.weight(1f))
    }
}

/** Wrapper com badge automático: lê EstadoInbox (v9) — usar nos Scaffolds. */
@Composable
fun BarraDockComBadge(atual: String, onNav: (String) -> Unit, modifier: Modifier = Modifier) {
    val naoLidas by EstadoInboxGlobal.collectAsState()
    BarraDockNova(atual = atual, onNav = onNav, naoLidas = naoLidas, modifier = modifier)
}

/** Acesso ao EstadoInbox sem import circular. */
val EstadoInboxGlobal = br.com.anunciaai.ui.screens.EstadoInbox.naoLidas

/** Item com escala animada no ativo (+15%) e micro-press (0.9) no toque. */
@Composable
private fun ItemDock(
    rota: String,
    atual: String,
    rotulo: String,
    icone: ImageVector,
    onNav: (String) -> Unit,
    modifier: Modifier = Modifier,
    badge: Int = 0
) {
    val ativo = rota == atual
    val interacao = remember { MutableInteractionSource() }
    val escala by animateFloatAsState(
        targetValue = if (ativo) 1.15f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "escala_$rotulo"
    )
    val cor by animateColorAsState(
        targetValue = if (ativo) Destaque else CorTextoSec,
        animationSpec = tween(300),
        label = "cor_$rotulo"
    )
    Column(
        modifier
            .clickable(interactionSource = interacao, indication = null) { onNav(rota) }
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        BadgedBox(badge = {
            if (badge > 0) Badge(containerColor = Destaque, contentColor = androidx.compose.ui.graphics.Color(0xFF12092B)) {
                Text(if (badge > 9) "9+" else "$badge")
            }
        }) {
            Icon(icone, contentDescription = rotulo, tint = cor,
                modifier = Modifier.size(24.dp).scale(escala))
        }
        Text(rotulo, style = MaterialTheme.typography.labelMedium, color = cor, maxLines = 1)
    }
}

/** Entalhe circular central (v8) — mantido. */
data class NotchShape(private val raio: androidx.compose.ui.unit.Dp) : androidx.compose.ui.graphics.Shape {
    override fun createOutline(
        size: androidx.compose.ui.geometry.Size,
        layoutDirection: androidx.compose.ui.unit.LayoutDirection,
        density: androidx.compose.ui.unit.Density
    ): androidx.compose.ui.graphics.Outline {
        val r = with(density) { raio.toPx() }
        val cx = size.width / 2f
        val path = androidx.compose.ui.graphics.Path().apply {
            moveTo(0f, 0f)
            lineTo(cx - r - 16f, 0f)
            cubicTo(cx - r, 0f, cx - r, 0f, cx - r, 10f)
            arcTo(
                rect = androidx.compose.ui.geometry.Rect(cx - r, -r, cx + r, r),
                startAngleDegrees = 180f,
                sweepAngleDegrees = 180f,
                forceMoveTo = false
            )
            cubicTo(cx + r, 0f, cx + r, 0f, cx + r + 16f, 0f)
            lineTo(size.width, 0f)
            lineTo(size.width, size.height)
            lineTo(0f, size.height)
            close()
        }
        return androidx.compose.ui.graphics.Outline.Generic(path)
    }
}
