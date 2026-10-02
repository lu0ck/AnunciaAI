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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import br.com.anunciaai.ui.screens.EstadoInbox
import br.com.anunciaai.ui.theme.CapsulaEscura
import br.com.anunciaai.ui.theme.CorTextoSec
import br.com.anunciaai.ui.theme.VerdeNeon

/**
 * ═════════════════════════════════════════════════════════════════
 *  v11.0 — NAV CÁPSULA FLUTUANTE (padrão Figma bike shop)
 * ═════════════════════════════════════════════════════════════════
 *  Barra em CÁPSULA (RoundedCornerShape 50%) flutuando acima do fundo
 *  (padding 24dp bottom / 16dp laterais), fundo #1E1E24 com sombra.
 *  O FAB verde-lime é o ÍCONE CENTRAL, integrado dentro do shape,
 *  maior que os demais, sem notch torto.
 *  Uso: NÃO é bottomBar — é overlay no Box da tela: { Conteudo();
 *    CapsulaFlutuante(...) alinhada BottomCenter }
 */
@Composable
fun CapsulaFlutuante(
    atual: String,
    onNav: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val naoLidas by EstadoInbox.naoLidas.collectAsState()

    Box(
        modifier
            .padding(start = 16.dp, end = 16.dp, bottom = 24.dp)
            .fillMaxWidth()
            .height(64.dp)
            .clip(RoundedCornerShape(50))
            .background(CapsulaEscura)
            .shadowAdvanced()
    ) {
        Row(
            Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ItemCapsula(Rotas.LISTA, atual, "Início", Icons.Default.Home, onNav, Modifier.weight(1f))
            ItemCapsula(Rotas.VITRINE, atual, "Vitrine", Icons.Default.GridView, onNav, Modifier.weight(1f))
            // FAB integrado: círculo verde-lime MAIOR no centro da cápsula
            Box(
                Modifier
                    .weight(1.2f)
                    .fillMaxHeight()
                    .padding(vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(VerdeNeon)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onNav(Rotas.CAPTURA) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Vender",
                        tint = Color(0xFF101601),
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
            ItemCapsula(Rotas.MENSAGENS, atual, "Chat", Icons.Default.ChatBubbleOutline, onNav, Modifier.weight(1f), badge = naoLidas)
            ItemCapsula(Rotas.CONFIG, atual, "Config", Icons.Outlined.Settings, onNav, Modifier.weight(1f))
        }
    }
}

/** Sombra suave da cápsula (modifier avançado). */
private fun Modifier.shadowAdvanced(): Modifier = this
    .shadow(18.dp, RoundedCornerShape(50))

/** Item da cápsula: ícone + label pequeno, ativo cresce 12% e fica lime. */
@Composable
private fun ItemCapsula(
    rota: String,
    atual: String,
    rotulo: String,
    icone: ImageVector,
    onNav: (String) -> Unit,
    modifier: Modifier = Modifier,
    badge: Int = 0
) {
    val ativo = rota == atual
    val escala by animateFloatAsState(
        targetValue = if (ativo) 1.12f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "esc_$rotulo"
    )
    val cor by animateColorAsState(
        targetValue = if (ativo) VerdeNeon else CorTextoSec,
        animationSpec = tween(300),
        label = "cor_$rotulo"
    )
    Column(
        modifier
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onNav(rota) }
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        BadgedBox(badge = {
            if (badge > 0) Badge(containerColor = VerdeNeon, contentColor = Color(0xFF101601)) {
                Text(if (badge > 9) "9+" else "$badge")
            }
        }) {
            Icon(icone, contentDescription = rotulo, tint = cor,
                modifier = Modifier.size(24.dp).scale(escala))
        }
        Text(rotulo, style = MaterialTheme.typography.labelSmall, color = cor, maxLines = 1)
    }
}

/** Compatibilidade: cápsula flutuante usada no bottomBar dos Scaffolds.
 *  O Scaffold cede o espaço; a cápsula desenha flutuando com margens. */
@Composable
fun BarraDockComBadge(atual: String, onNav: (String) -> Unit, modifier: Modifier = Modifier) {
    CapsulaFlutuante(atual = atual, onNav = onNav, modifier = modifier)
}
