package br.com.anunciaai.ui

import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import br.com.anunciaai.ui.screens.EstadoInbox
import br.com.anunciaai.ui.theme.CapsulaEscura
import br.com.anunciaai.ui.theme.CorTextoSec
import br.com.anunciaai.ui.theme.Destaque

/**
 * v12.0 — NAV CÁPSULA FLUTUANTE COM GLASSMORPHISM (estrutura RAIZ).
 * Componente próprio (não BottomAppBar esticado): Surface com
 * RoundedCornerShape(32.dp), descolada da borda (padding 16/12dp),
 * flutuando sobre o conteúdo.
 * Glass: camada de fundo translúcida + Modifier.blur(16.dp) no
 * Android 12+ (API 31). Abaixo disso: fallback sólido
 * Color.Black.copy(alpha = 0.6f), sem blur, para não ficar invisível.
 * FAB ÚNICO "+" no slot floatingActionButton da cápsula — mora aqui,
 * na estrutura de navegação raiz; nunca redeclarado em tela alguma.
 */
@Composable
fun NavCapsula(
    atual: String,
    onNav: (String) -> Unit,
    onVender: () -> Unit,
    modifier: Modifier = Modifier
) {
    val naoLidas by EstadoInbox.naoLidas.collectAsState()

    Surface(
        shape = RoundedCornerShape(32.dp),
        color = Color.Transparent,
        shadowElevation = 18.dp,
        modifier = modifier
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .fillMaxWidth()
            .height(64.dp)
    ) {
        Box {
            // glass: camada de fundo translúcida + blur (API 31+)
            Box(
                Modifier.matchParentSize().then(
                    if (Build.VERSION.SDK_INT >= 31)
                        Modifier
                            .blur(16.dp)
                            .background(CapsulaEscura.copy(alpha = 0.55f))
                    else
                        Modifier.background(Color.Black.copy(alpha = 0.6f))
                )
            )
            Row(
                Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ItemCapsula(Rotas.LISTA, atual, "Início", Icons.Default.Home, onNav, Modifier.weight(1f))
                ItemCapsula(Rotas.VITRINE, atual, "Vitrine", Icons.Default.GridView, onNav, Modifier.weight(1f))
                // slot floatingActionButton DA CÁPSULA — único "+" do app
                Box(
                    Modifier
                        .weight(1.2f)
                        .fillMaxHeight()
                        .padding(vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    FabDaCapsula(onVender)
                }
                ItemCapsula(Rotas.MENSAGENS, atual, "Chat", Icons.Default.ChatBubbleOutline, onNav, Modifier.weight(1f), badge = naoLidas)
                ItemCapsula(Rotas.CONFIG, atual, "Config", Icons.Outlined.Settings, onNav, Modifier.weight(1f))
            }
        }
    }
}

/** FAB "+" da cápsula — o ÚNICO do app (slot da cápsula, não do Scaffold). */
@Composable
private fun FabDaCapsula(onVender: () -> Unit) {
    Surface(
        onClick = onVender,
        shape = CircleShape,
        color = Destaque,
        contentColor = Color(0xFF04150F),
        shadowElevation = 8.dp,
        modifier = Modifier.size(52.dp)
    ) {
        Icon(Icons.Default.Add, contentDescription = "Vender", modifier = Modifier.size(28.dp))
    }
}

/** Item da cápsula: ícone + label, ativo cresce 12% e fica verde-menta. */
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
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "esc_$rotulo"
    )
    val cor by animateColorAsState(
        targetValue = if (ativo) Destaque else CorTextoSec,
        animationSpec = tween(300),
        label = "cor_$rotulo"
    )
    Column(
        modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onNav(rota) }
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        BadgedBox(badge = {
            if (badge > 0) Badge(containerColor = Destaque, contentColor = Color(0xFF04150F)) {
                Text(if (badge > 9) "9+" else "$badge")
            }
        }) {
            Icon(
                icone, contentDescription = rotulo, tint = cor,
                modifier = Modifier.size(24.dp).scale(escala)
            )
        }
        Text(rotulo, style = MaterialTheme.typography.labelSmall, color = cor, maxLines = 1)
    }
}
