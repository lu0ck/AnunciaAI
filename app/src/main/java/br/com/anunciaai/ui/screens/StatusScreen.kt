package br.com.anunciaai.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import br.com.anunciaai.AnunciaAIApp
import br.com.anunciaai.publica.webview.SessaoPublicacaoWeb
import br.com.anunciaai.ui.IconePlataforma
import br.com.anunciaai.ui.Plataforma
import br.com.anunciaai.ui.theme.CorFundo
import br.com.anunciaai.ui.theme.CorTexto
import br.com.anunciaai.ui.theme.CorTextoSec
import br.com.anunciaai.ui.theme.Destaque

/**
 * STATUS (v12.0): resultado da publicação por plataforma — tema escuro,
 * sem dock (tela de fluxo, cápsula não aparece).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatusScreen(
    itemId: Long,
    onVoltar: () -> Unit,
    onVerDetalhe: () -> Unit
) {
    val app = LocalContext.current.applicationContext as AnunciaAIApp
    val pubs by app.repositorio.publicacoesDoItem(itemId).collectAsState(initial = emptyList())
    val resultadoWeb by SessaoPublicacaoWeb.resultado.collectAsState()

    Scaffold(
        containerColor = CorFundo,
        topBar = {
            TopAppBar(
                title = { Text("Status", style = MaterialTheme.typography.headlineSmall, color = CorTexto) },
                navigationIcon = {
                    IconButton(onClick = onVoltar) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar", tint = CorTexto)
                    }
                }
            )
        }
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
                    color = if (r.ok) Destaque else MaterialTheme.colorScheme.error
                )
                if (r.msg.isNotBlank() && !r.ok) {
                    Text(r.msg, style = MaterialTheme.typography.bodySmall,
                        color = CorTextoSec)
                }
                Spacer(Modifier.height(16.dp))
            }

            Text("Resultado por plataforma", style = MaterialTheme.typography.titleMedium, color = CorTexto)
            Spacer(Modifier.height(10.dp))

            if (pubs.isEmpty()) {
                Text(
                    "Nada publicado ainda. As APIs publicam direto; nas demais o app abre a tela da plataforma.",
                    color = CorTextoSec
                )
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                    items(pubs, key = { it.id }) { pub ->
                        val estado = when (pub.status) {
                            "PUBLICADO" -> "Publicado"
                            "VENDIDO" -> "Vendido"
                            "ENCERRADO" -> "Encerrado"
                            "ERRO" -> "Erro"
                            else -> "pendente"
                        }
                        Row(
                            Modifier.fillMaxWidth().padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconePlataforma(pub.plataforma, conectada = pub.status == "PUBLICADO", tamanho = 32.dp)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    "${Plataforma.doNome(pub.plataforma)?.rotulo ?: pub.plataforma} — $estado",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = when (pub.status) {
                                        "PUBLICADO" -> Destaque
                                        "ERRO" -> MaterialTheme.colorScheme.error
                                        else -> CorTexto
                                    }
                                )
                                pub.urlAnuncio?.let {
                                    Text(it, style = MaterialTheme.typography.bodySmall,
                                        color = CorTextoSec, maxLines = 1)
                                }
                                pub.mensagemErro?.let {
                                    Text(it, style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.error, maxLines = 2)
                                }
                            }
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            TextButton(onClick = onVerDetalhe) { Text("Ver detalhe", color = Destaque) }
        }
    }
}
