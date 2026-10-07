package br.com.anunciaai.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import br.com.anunciaai.ui.theme.CorTexto
import br.com.anunciaai.ui.theme.CorTextoSec
import br.com.anunciaai.ui.theme.Destaque

/**
 * CONFIGURAÇÕES (v12.0): hub Perfil/Conexões/IA/Sobre — tema escuro,
 * linhas com separador (spec: lista simples), SEM FAB (o "+" vive na cápsula raiz).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfigScreen(
    onAbrirPerfil: () -> Unit = {},
    onAbrirConexoes: () -> Unit = {}
) {
    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Configurações", style = MaterialTheme.typography.headlineSmall, color = CorTexto) }
        )
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .background(br.com.anunciaai.ui.theme.CorFundo)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(Modifier.height(8.dp))
            LinhaConfig(Icons.Default.Person, "Perfil", "Foto, nome, nick, bio e redes sociais", onAbrirPerfil)
            LinhaConfig(Icons.Default.Link, "Conexões", "Mercado Livre, OLX, Shopee e outras", onAbrirConexoes)
            Spacer(Modifier.height(16.dp))
            ConexoesIAEmb()
            Spacer(Modifier.height(16.dp))
            LinhaConfig(Icons.Outlined.Info, "Sobre", "AnunciaAI v${br.com.anunciaai.BuildConfig.VERSION_NAME}", {})
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun LinhaConfig(
    icon: ImageVector,
    titulo: String,
    sub: String,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)
    ) {
        androidx.compose.foundation.layout.Row(
            Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = Destaque)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    titulo,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = CorTexto
                )
                Text(sub, style = MaterialTheme.typography.bodySmall, color = CorTextoSec)
            }
            Icon(
                Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = CorTextoSec
            )
        }
    }
}

/** Cartão do estado da IA (sem navegação). */
@Composable
private fun ConexoesIAEmb() {
    val provedor = br.com.anunciaai.ia.FabricaIA.nomeAtivo()
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        androidx.compose.foundation.layout.Row(
            Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = if (provedor != null) Destaque else CorTextoSec
            )
            Spacer(Modifier.width(14.dp))
            Column {
                Text(
                    "Inteligência artificial",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = CorTexto
                )
                Text(
                    if (provedor != null) "Ativa — $provedor" else "Sem chave configurada",
                    style = MaterialTheme.typography.bodySmall,
                    color = CorTextoSec
                )
            }
        }
    }
}
