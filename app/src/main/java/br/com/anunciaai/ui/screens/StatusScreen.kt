package br.com.anunciaai.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import br.com.anunciaai.AnunciaAIApp
import br.com.anunciaai.publica.webview.SessaoPublicacaoWeb
import br.com.anunciaai.ui.BarraInferior
import br.com.anunciaai.ui.Plataforma
import br.com.anunciaai.ui.Rotas
import br.com.anunciaai.ui.theme.CorPlataforma

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatusScreen(
    itemId: Long,
    onVoltar: () -> Unit,
    onVerDetalhe: () -> Unit,
    onNavBottom: (String) -> Unit = {}
) {
    val app = LocalContext.current.applicationContext as AnunciaAIApp
    val pubs by app.repositorio.publicacoesDoItem(itemId).collectAsState(initial = emptyList())
    val resultadoWeb by SessaoPublicacaoWeb.resultado.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Status") },
                navigationIcon = { TextButton(onClick = onVoltar) { Text("←") } }
            )
        },
        bottomBar = { BarraInferior(Rotas.LISTA, onNavBottom) }
    ) { pad ->
        Column(Modifier.padding(pad).padding(16.dp)) {
            resultadoWeb?.let { r ->
                Text(
                    buildString {
                        append(Plataforma.doNome(r.plataforma)?.rotulo ?: r.plataforma)
                        append(": ")
                        append(if (r.ok) "Publicado" else "erro")
                    },
                    style = MaterialTheme.typography.titleMedium,
                    color = if (r.ok) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                )
                if (r.msg.isNotBlank() && !r.ok) {
                    Text(r.msg, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(Modifier.height(16.dp))
            }

            Text("Resultado por plataforma", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(10.dp))

            if (pubs.isEmpty()) {
                Text(
                    "Nada publicado ainda. As APIs publicam direto; nas demais o app abre a tela da plataforma.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(pubs, key = { it.id }) { pub ->
                        val (cor, estado) = quando(pub.status)
                        Row(
                            Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // indicador inline: ponto colorido + texto
                            Box(Modifier.size(10.dp).background(cor, CircleShape))
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    "${Plataforma.doNome(pub.plataforma)?.rotulo ?: pub.plataforma} — $estado",
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                pub.urlAnuncio?.let {
                                    Text(it, style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                                }
                                pub.mensagemErro?.let {
                                    Text(it, style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.error, maxLines = 2)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            OutlinedButton(onClick = onVerDetalhe, Modifier.fillMaxWidth()) {
                Text("Ver detalhe")
            }
        }
    }
}

fun quando(status: String): Pair<androidx.compose.ui.graphics.Color, String> =
    when (status) {
        "PUBLICADO" -> androidx.compose.ui.graphics.Color(0xFF1F7A5C) to "Publicado"
        "VENDIDO" -> androidx.compose.ui.graphics.Color(0xFFD9A441) to "Vendido"
        "ENCERRADO" -> androidx.compose.ui.graphics.Color(0xFF8A8A8A) to "Encerrado"
        "ERRO" -> androidx.compose.ui.graphics.Color(0xFFB33A3A) to "Erro"
        else -> androidx.compose.ui.graphics.Color(0xFF8A8A8A) to "pendente"
    }
