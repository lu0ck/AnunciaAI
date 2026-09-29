package br.com.anunciaai.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import br.com.anunciaai.AnunciaAIApp
import br.com.anunciaai.publica.webview.SessaoPublicacaoWeb
import br.com.anunciaai.publica.webview.ScriptsWeb
import br.com.anunciaai.ui.BarraInferior
import br.com.anunciaai.ui.Plataforma
import br.com.anunciaai.ui.Rotas
import br.com.anunciaai.ui.foto.FotoPrimeira
import br.com.anunciaai.ui.foto.FotoMini
import br.com.anunciaai.ui.theme.CorPlataforma
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListaItensScreen(
    onNovoItem: () -> Unit,
    onAbrirItem: (Long) -> Unit,
    onNavBottom: (String) -> Unit = {}
) {
    val app = LocalContext.current.applicationContext as AnunciaAIApp
    val pubs by app.repositorio.publicacoes().collectAsState(initial = emptyList())
    var busca by remember { mutableStateOf("") }
    val itens by app.repositorio.itensComBusca(busca).collectAsState(initial = emptyList())

    Scaffold(
        topBar = { TopAppBar(
            title = { Text("AnunciaAI") },
            // Marcador de build: prova visual de qual APK está instalado.
            actions = { Text(
                "v${br.com.anunciaai.BuildConfig.VERSION_NAME}",
                Modifier.padding(horizontal = 16.dp),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            ) }
        ) },
        bottomBar = { BarraInferior(Rotas.LISTA, onNavBottom) },
        floatingActionButton = {
            ExtendedFloatingActionButton(onClick = onNovoItem) { Text("Publicar item") }
        }
    ) { pad ->
        Column(Modifier.padding(pad).fillMaxSize()) {
            OutlinedTextField(
                value = busca, onValueChange = { busca = it },
                placeholder = { Text("Buscar por nome") },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                shape = MaterialTheme.shapes.small,
                singleLine = true
            )
            if (itens.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        if (busca.isBlank())
                            "Nada anunciado ainda. Toque em Publicar item, fotografe e deixe a IA escrever."
                        else "Nada encontrado para \"$busca\".",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 32.dp)
                    )
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)) {
                    items(itens, key = { it.id }) { item ->
                        CardItem(
                            item,
                            statusDoItem(item.id, pubs),
                            onClick = { onAbrirItem(item.id) }
                        )
                    }
                }
            }
        }
    }
}

private fun statusDoItem(itemId: Long, pubs: List<br.com.anunciaai.dados.PublicacaoPlataforma>): String {
    val doItem = pubs.filter { it.itemId == itemId }
    if (doItem.isEmpty()) return ""
    val ok = doItem.count { it.status == "PUBLICADO" }
    val vendido = doItem.count { it.status == "VENDIDO" }
    val erro = doItem.count { it.status == "ERRO" }
    return buildString {
        if (ok > 0) append("$ok publicados")
        if (vendido > 0) { if (isNotEmpty()) append(" · "); append("vendido") }
        if (erro > 0) { if (isNotEmpty()) append(" · "); append("$erro com erro") }
    }
}

@Composable
private fun CardItem(
    item: br.com.anunciaai.dados.Item,
    status: String,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            FotoPrimeira(item.id, tamanho = 64)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(item.titulo.ifEmpty { "(sem título)" },
                    style = MaterialTheme.typography.titleMedium, maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                Spacer(Modifier.height(2.dp))
                Text(
                    "R$ ${"%.2f".format(item.precoFinal.takeIf { it > 0 } ?: item.precoSugerido)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (status.isNotEmpty()) {
                    Spacer(Modifier.height(2.dp))
                    Text(status, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}
