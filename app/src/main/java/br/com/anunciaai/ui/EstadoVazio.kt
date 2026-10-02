package br.com.anunciaai.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * Empty state premium (spec ITEM 3): conteúdo centralizado, ícone grande e
 * minimalista, título em destaque, subtítulo explicativo e botão de ação
 * secundário largo — fundo cinza, texto branco.
 */
@Composable
fun EstadoVazio(
    icone: ImageVector,
    titulo: String,
    subtitulo: String,
    textoBotao: String? = null,
    onBotao: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 36.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            icone,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = Color(0xFF4B5570) // cinza discreto, minimalista
        )
        Spacer(Modifier.height(16.dp))
        Text(
            titulo,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(6.dp))
        Text(
            subtitulo,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        if (textoBotao != null && onBotao != null) {
            Spacer(Modifier.height(22.dp))
            Button(
                onClick = onBotao,
                modifier = Modifier
                    .fillMaxWidth(0.78f)
                    .height(50.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF1B2440), // cinza
                    contentColor = Color.White
                )
            ) {
                Text(textoBotao, style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}
