package br.com.anunciaai.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import br.com.anunciaai.ui.theme.CorPlataforma

/**
 * Monograma da plataforma: círculo com as iniciais (ML, EB, SH, OLX, FB, EJ).
 * Conectada → cor real da marca (pequena, só no ícone).
 * Desconectada → escala de cinza. A cor NUNCA comunica estado — o texto comunica.
 */
@Composable
fun IconePlataforma(nomePlataforma: String, conectada: Boolean, tamanho: Dp = 40.dp) {
    val inicial = br.com.anunciaai.ui.Plataforma.doNome(nomePlataforma)?.iniciais
        ?: nomePlataforma.take(2).uppercase()
    val cor = if (conectada) (CorPlataforma[nomePlataforma] ?: MaterialTheme.colorScheme.primary)
    else MaterialTheme.colorScheme.onSurfaceVariant
    Box(
        Modifier
            .size(tamanho)
            .background(MaterialTheme.colorScheme.surfaceContainer, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            inicial,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.ExtraBold,
            color = cor
        )
    }
}
