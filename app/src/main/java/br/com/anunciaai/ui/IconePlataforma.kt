package br.com.anunciaai.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import br.com.anunciaai.ui.theme.CorPlataforma
import br.com.anunciaai.ui.theme.CorTextoMarca

/**
 * Badge de plataforma (v4): quadrado com cantos 12dp, fundo na COR DA MARCA,
 * inicial em branco (ML: azul #2D3277 sobre o amarelo). A cor de marca é a
 * identidade — o estado (conectado/não) comunica por TEXTO, não pela cor.
 * eBay: logo multicor original (azul/vermelho/amarelo/verde), não achatado.
 */
@Composable
fun IconePlataforma(nomePlataforma: String, conectada: Boolean = true, tamanho: Dp = 40.dp) {
    if (nomePlataforma == "EBAY") {
        Box(
            Modifier
                .size(tamanho)
                .clip(RoundedCornerShape(12.dp))
                .drawBehind {
                    val w = size.width / 2
                    drawRect(Color(0xFFE53238), size = Size(w, w))                            // e
                    drawRect(Color(0xFF0064D2), topLeft = Offset(w, 0f), size = Size(w, w))  // b
                    drawRect(Color(0xFFF5AF02), topLeft = Offset(0f, w), size = Size(w, w))  // a
                    drawRect(Color(0xFF86B817), topLeft = Offset(w, w), size = Size(w, w))   // y
                }
        )
        return
    }
    val iniciais = Plataforma.doNome(nomePlataforma)?.iniciais
        ?: nomePlataforma.take(2).uppercase()
    val fundo = CorPlataforma[nomePlataforma] ?: MaterialTheme.colorScheme.primary
    val corTexto = CorTextoMarca[nomePlataforma] ?: Color.White
    Box(
        Modifier
            .size(tamanho)
            .clip(RoundedCornerShape(12.dp))
            .background(fundo),
        contentAlignment = Alignment.Center
    ) {
        Text(
            iniciais,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.ExtraBold,
            color = corTexto
        )
    }
}
