package br.com.anunciaai.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import br.com.anunciaai.AnunciaAIApp
import br.com.anunciaai.ui.BarraInferior
import br.com.anunciaai.ui.Rotas
import br.com.anunciaai.ui.foto.FotoPrimeira
import br.com.anunciaai.ui.theme.Destaque

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
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "AnunciaAI",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold
                    )
                },
                actions = {
                    Text(
                        "v${br.com.anunciaai.BuildConfig.VERSION_NAME}",
                        Modifier.padding(end = 20.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            )
        },
        bottomBar = { BarraInferior(Rotas.LISTA, onNavBottom) },
        // v3: FAB redondo, canto inferior direito, ACIMA da barra (Scaffold já posiciona acima do bottomBar)
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNovoItem,
                containerColor = Destaque,
                contentColor = androidx.compose.material3.MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Publicar item")
            }
        },
        floatingActionButtonPosition = FabPosition.End
    ) { pad ->
        Column(Modifier.padding(pad).fillMaxSize()) {
            // Busca: borda fina 1px na cor de superfície, sem preenchimento chapado
            OutlinedTextField(
                value = busca, onValueChange = { busca = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                shape = CircleShape,
                placeholder = { Text("Buscar anúncios") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (busca.isNotEmpty()) IconButton(onClick = { busca = "" }) {
                        Icon(Icons.Default.Close, contentDescription = "Limpar")
                    }
                },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.surfaceContainer,
                    unfocusedBorderColor = MaterialTheme.colorScheme.surfaceContainer,
                    focusedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                    unfocusedContainerColor = androidx.compose.ui.graphics.Color.Transparent
                )
            )
            if (itens.isEmpty()) {
                // Empty state: ícone de câmera em linha fina (outline), sem botão — a ação é só o FAB
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(horizontal = 40.dp)
                    ) {
                        Icon(
                            Icons.Outlined.PhotoCamera,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            if (busca.isBlank()) "Sua vitrine está vazia"
                            else "Nada para \"$busca\"",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            if (busca.isBlank()) "Toque no + para fotografar um item"
                            else "Tente buscar por outro nome.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(0.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)
                ) {
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

// Linha de lista: borda fina 1px (superfície), sem sombra, sem "flutuar"
@Composable
private fun CardItem(
    item: br.com.anunciaai.dados.Item,
    status: String,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.small,
        colors = CardDefaults.cardColors(containerColor = androidx.compose.ui.graphics.Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(62.dp).clip(MaterialTheme.shapes.small)) {
                FotoPrimeira(item.id, tamanho = 62)
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    item.titulo.ifEmpty { "(sem título)" },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                val preco = item.precoFinal.takeIf { it > 0 } ?: item.precoSugerido
                Text(
                    "R$ ${"%.2f".format(preco)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = Destaque
                )
                if (status.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        status,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
