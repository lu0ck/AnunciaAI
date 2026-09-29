package br.com.anunciaai.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import br.com.anunciaai.AnunciaAIApp
import br.com.anunciaai.ia.FabricaIA
import br.com.anunciaai.ui.BarraDockNova
import br.com.anunciaai.ui.Rotas
import br.com.anunciaai.ui.theme.CorSuperficie
import br.com.anunciaai.ui.theme.Destaque

/**
 * Perfil (v6): centro de configuração — IA ativa, versão e as CONEXÕES
 * (absorvidas daqui pra fora: a aba deixa de existir na barra inferior).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PerfilScreen(onNavBottom: (String) -> Unit = {}) {
    val contexto = LocalContext.current
    val app = contexto.applicationContext as AnunciaAIApp

    Scaffold(
        topBar = { TopAppBar(title = { Text("Perfil") }) },
        bottomBar = { BarraDockNova(Rotas.PERFIL, onNavBottom) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onNavBottom(Rotas.CAPTURA) },
                containerColor = Destaque,
                contentColor = androidx.compose.ui.graphics.Color(0xFF06231B),
                shape = androidx.compose.foundation.shape.CircleShape,
                modifier = Modifier.size(58.dp)
            ) {
                Icon(
                    androidx.compose.material.icons.Icons.Default.Add,
                    contentDescription = "Vender"
                )
            }
        },
        floatingActionButtonPosition = FabPosition.Center
    ) { pad ->
        Column(
            Modifier
                .padding(pad)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Cartão da IA
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = CorSuperficie,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.AutoAwesome, contentDescription = null,
                        tint = if (FabricaIA.nomeAtivo() != null) Destaque else Color(0xFF8B909A)
                    )
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("Inteligência artificial", style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold)
                        Text(
                            if (FabricaIA.nomeAtivo() != null)
                                "Ativa — descrições geradas pela IA"
                            else "Sem chave configurada — preenchimento manual",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF8B909A)
                        )
                    }
                }
            }
            Spacer(Modifier.height(12.dp))

            // Conexões (tela inteira da antiga aba, embutida)
            Text("Conexões", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            ConexoesScreen(embutida = true)
        }
    }
}
