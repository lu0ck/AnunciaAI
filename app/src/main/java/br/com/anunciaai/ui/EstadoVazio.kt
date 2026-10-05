package br.com.anunciaai.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import br.com.anunciaai.ui.theme.CorTexto
import br.com.anunciaai.ui.theme.CorTextoSec
import br.com.anunciaai.ui.theme.CorNeutro

/**
 * Empty state (v12.0, tema escuro): conteúdo centralizado, ícone grande
 * discreto, título em destaque, subtítulo explicativo, botão secundário
 * largo (fundo neutro #23272E + texto principal).
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
            tint = CorTextoSec
        )
        Spacer(Modifier.height(16.dp))
        Text(
            titulo,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = CorTexto,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(6.dp))
        Text(
            subtitulo,
            style = MaterialTheme.typography.bodyMedium,
            color = CorTextoSec,
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
                    containerColor = CorNeutro,
                    contentColor = Color(0xFFF2F2F0)
                )
            ) {
                Text(textoBotao, style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}
