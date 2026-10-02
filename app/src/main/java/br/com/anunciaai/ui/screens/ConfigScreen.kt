package br.com.anunciaai.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import br.com.anunciaai.ui.BarraDockComBadge
import br.com.anunciaai.ui.Rotas
import br.com.anunciaai.ui.theme.CorSuperficie

/**
 * v9 — Configurações: hub com Perfil, Conexões (absorvidas daqui), IA e Sobre.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfigScreen(
    onNavBottom: (String) -> Unit = {},
    onAbrirPerfil: () -> Unit = {},
    onAbrirConexoes: () -> Unit = {}
) {
    Scaffold(
        topBar = { TopAppBar(title = { Text("Configurações") }) },
        bottomBar = { BarraDockComBadge(Rotas.CONFIG, onNavBottom) },
        floatingActionButton = { FabCentral(onNavBottom) },
        floatingActionButtonPosition = androidx.compose.material3.FabPosition.Center
    ) { pad ->
        Column(
            Modifier.padding(pad).fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            Spacer(Modifier.height(8.dp))
            LinhaConfig(icon = Icons.Default.Person, titulo = "Perfil", sub = "Foto, nome, nick, bio e redes sociais", onClick = onAbrirPerfil)
            LinhaConfig(icon = Icons.Default.Link, titulo = "Conexões", sub = "Mercado Livre, OLX, Shopee e outras", onClick = onAbrirConexoes)
            Spacer(Modifier.height(16.dp))
            // IA (estado embutido, sem navegação)
            ConexoesIAEmb() // cartão da IA reutilizado (sem interação)
            Spacer(Modifier.height(16.dp))
            LinhaConfig(icon = Icons.Outlined.Info, titulo = "Sobre", sub = "AnunciaAI v${br.com.anunciaai.BuildConfig.VERSION_NAME}", onClick = {})
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun LinhaConfig(icon: androidx.compose.ui.graphics.vector.ImageVector, titulo: String, sub: String, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CorSuperficie),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = br.com.anunciaai.ui.theme.Destaque)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(titulo, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(sub, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.AutoMirrored.Filled.ArrowForwardIos, contentDescription = null,
                modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** Cartão do estado da IA (reaproveitado da ConexoesScreen, sem menu). */
@Composable
private fun ConexoesIAEmb() {
    val provedor = br.com.anunciaai.ia.FabricaIA.nomeAtivo()
    Card(
        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CorSuperficie),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                androidx.compose.material.icons.Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = if (provedor != null) br.com.anunciaai.ui.theme.Destaque else Color(0xFF8A93AD)
            )
            Spacer(Modifier.width(14.dp))
            Column {
                Text("Inteligência artificial", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    if (provedor != null) "Ativa — $provedor" else "Sem chave configurada",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/** FAB central padrão (reuso nas telas com dock). */
@Composable
fun FabCentral(onNavBottom: (String) -> Unit) {
    FloatingActionButton(
        onClick = { onNavBottom(Rotas.CAPTURA) },
        containerColor = br.com.anunciaai.ui.theme.Destaque,
        contentColor = Color(0xFF12092B),
        shape = androidx.compose.foundation.shape.CircleShape,
        modifier = Modifier.size(58.dp)
    ) {
        Icon(androidx.compose.material.icons.Icons.Default.Add, contentDescription = "Vender")
    }
}
