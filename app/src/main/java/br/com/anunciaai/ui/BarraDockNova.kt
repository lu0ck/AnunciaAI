package br.com.anunciaai.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import br.com.anunciaai.ui.theme.CorSuperficie
import br.com.anunciaai.ui.theme.CorTextoSec
import br.com.anunciaai.ui.theme.Destaque

/**
 * ══════════════════════════════════════════════════════════════════
 *  BarraDockNova (v6.1 — correção de estabilidade)
 * ══════════════════════════════════════════════════════════════════
 *  BottomAppBar OFICIAL do Material 3 (estável) + FAB "Vender" central.
 *
 *  ▸ O FAB é ancorado pelo SCAFFOLD da tela
 *    (floatingActionButtonPosition = FabPosition.Center), que o posiciona
 *    sobrepondo o centro desta barra — o BottomAppBar só reserva o vão
 *    central com Spacer(weight 1f). Sem Canvas/Path custom (causa do
 *    crash da v6.0 — removido).
 *  ▸ 4 itens distribuídos com weight: Início | Vitrine | [vão] | Mensagens | Perfil
 *  ▸ Animações: escala spring no ícone ativo (1.0→1.15) + cor 300ms
 *  ▸ PASSO 3: aba ativa em rememberSaveable sincronizada com a rota
 *    (fonte única de verdade) — sem recomposição infinita.
 */
@Composable
fun BarraDockNova(atual: String, onNav: (String) -> Unit, modifier: Modifier = Modifier) {
    // PASSO 3: estado da aba salvo; a rota é a fonte da verdade e o
    // LaunchedEffect só sincroniza quando ela muda (sem loop).
    val abaAtiva = rememberSaveable { mutableStateOf(atual) }
    LaunchedEffect(atual) { abaAtiva.value = atual }

    BottomAppBar(
        modifier = modifier,
        containerColor = CorSuperficie,
        contentColor = CorTextoSec,
        tonalElevation = 0.dp
    ) {
        ItemDock(Rotas.LISTA, abaAtiva.value, "Início", Icons.Default.Home, onNav, Modifier.weight(1f))
        ItemDock(Rotas.VITRINE, abaAtiva.value, "Vitrine", Icons.Default.GridView, onNav, Modifier.weight(1f))
        Spacer(Modifier.weight(1f)) // vão central reservado ao FAB do Scaffold
        ItemDock(Rotas.MENSAGENS, abaAtiva.value, "Mensagens", Icons.Default.ChatBubbleOutline, onNav, Modifier.weight(1f))
        ItemDock(Rotas.PERFIL, abaAtiva.value, "Perfil", Icons.Default.Person, onNav, Modifier.weight(1f))
    }
}

/** Item do dock: ícone + label com escala/cor animadas. */
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
