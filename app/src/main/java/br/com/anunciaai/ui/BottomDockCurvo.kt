package br.com.anunciaai.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import br.com.anunciaai.ui.theme.CorFundo
import br.com.anunciaai.ui.theme.CorTextoSec
import br.com.anunciaai.ui.theme.Destaque

/**
 * ═══════════════════════════════════════════════════════════════════
 *  CustomBottomDock (v6 — PONTO 1 da spec premium)
 * ═══════════════════════════════════════════════════════════════════
 * Barra de navegação CUSTOM — NavigationBar padrão do Material PROIBIDA.
 *
 *  ▸ Forma: retângulo com VAÇÃO curva no centro (arco desenhado via Path
 *    custom em Canvas) que acomoda o FAB "Vender" flutuando no vão.
 *  ▸ Itens (esq→dir): Início | Vitrine | [VÃO/FAB] | Mensagens | Perfil
 *  ▸ Animações:
 *      – ícone ativo: escala 1.0 → 1.15 com spring bouncy
 *      – cor do ícone: animateColorAsState 300ms (cinza → verde-menta)
 *      – transição de tela: o NavHost já faz fade+slide (v5.4)
 *  ▸ FAB central: verde-menta #00C896, círculo 64dp, elevation flutuante,
 *    sombra suave; toca → câmera (Rotas.CAPTURA).
 */
@Composable
fun BottomDockCurvo(
    atual: String,
    onNav: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val alturaBarra = 64.dp
    val raioVao = 34.dp          // raio do semicírculo do vão central
    val alturaFab = 64.dp

    Box(modifier.fillMaxWidth().height(alturaBarra + 26.dp)) {
        // ── 1. Desenha a barra com o vão curvo (Path custom) ──
        Canvas(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(alturaBarra)
        ) {
            val w = size.width
            val h = size.height
            val cx = w / 2f                       // centro do vão
            val r = raioVao.toPx()
            val path = Path().apply {
                // começa no canto esquerdo-topo da barra
                moveTo(0f, 0f)
                // vai até a borda esquerda do vão (com "raio de fuga" suave)
                lineTo(cx - r - 14.dp.toPx(), 0f)
                // curva de aproximação (cúbica suave subindo pro vão)
                cubicTo(
                    cx - r, 0f,
                    cx - r, 0f,
                    cx - r, 6.dp.toPx()
                )
                // semicírculo do vão (sobe e desce)
                arcTo(
                    rect = androidx.compose.ui.geometry.Rect(
                        left = cx - r, top = -r, right = cx + r, bottom = r
                    ),
                    startAngleDegrees = 180f,
                    sweepAngleDegrees = -180f,
                    forceMoveTo = false
                )
                // curva de saída descendo de volta pra barra
                cubicTo(
                    cx + r, 0f,
                    cx + r, 0f,
                    cx + r + 14.dp.toPx(), 0f
                )
                // segue até a direita e fecha
                lineTo(w, 0f)
                lineTo(w, h)
                lineTo(0f, h)
                close()
            }
            drawPath(path, br.com.anunciaai.ui.theme.CorSuperficie)
        }

        // ── 2. FAB "Vender" flutuando no vão central ──
        Box(
            Modifier
                .align(Alignment.TopCenter)
                .padding(top = 0.dp)   // fica levemente sobreposto à barra
                .size(alturaFab)
                .background(Destaque, CircleShape)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { onNav(Rotas.CAPTURA) },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.AddAPhoto,
                contentDescription = "Vender",
                tint = Color(0xFF06231B),
                modifier = Modifier.size(26.dp)
            )
        }

        // ── 3. Itens da barra (Início | Vitrine | vão | Mensagens | Perfil) ──
        Row(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(alturaBarra),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ItemDock(
                rota = Rotas.LISTA, atual = atual, rotulo = "Início",
                icone = Icons.Default.Home, onNav = onNav,
                modifier = Modifier.weight(1f)
            )
            ItemDock(
                rota = Rotas.VITRINE, atual = atual, rotulo = " " + "Vitrine",
                icone = Icons.Default.GridOn, onNav = onNav,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.weight(1.2f))   // espaço do vão/FAB
            ItemDock(
                rota = Rotas.MENSAGENS, atual = atual, rotulo = "Mensagens",
                icone = Icons.Default.ChatBubbleOutline, onNav = onNav,
                modifier = Modifier.weight(1f)
            )
            ItemDock(
                rota = Rotas.PERFIL, atual = atual, rotulo = "Perfil",
                icone = Icons.Default.Person, onNav = onNav,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/** Um item do dock: ícone + label, escala animada quando ativo. */
@Composable
private fun ItemDock(
    rota: String,
    atual: String,
    rotulo: String,
    icone: androidx.compose.ui.graphics.vector.ImageVector,
    onNav: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val ativo = atual == rota
    // ── escala animada (PONTO 1: leve efeito de escala na troca de aba) ──
    val escala by animateFloatAsState(
        targetValue = if (ativo) 1.15f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "escala_$rotulo"
    )
    // ── cor animada 300ms (cinza-secundário → verde-menta) ──
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
        Icon(
            icone,
            contentDescription = rotulo,
            tint = cor,
            modifier = Modifier.size(24.dp).scale(escala)
        )
        Text(
            rotulo.trim(),
            style = MaterialTheme.typography.labelMedium,
            color = cor
        )
    }
}
