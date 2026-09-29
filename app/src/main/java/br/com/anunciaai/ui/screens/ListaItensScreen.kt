package br.com.anunciaai.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import br.com.anunciaai.AnunciaAIApp
import br.com.anunciaai.ui.BarraInferior
import br.com.anunciaai.ui.Rotas
import br.com.anunciaai.ui.foto.FotoPrimeira

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
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                // Marcador de build: prova visual de qual APK está instalado.
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
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onNovoItem,
                shape = RoundedCornerShape(18.dp),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.AddAPhoto, contentDescription = null, Modifier.size(20.dp))
                Spacer(Modifier.width(10.dp))
                Text("Publicar item", style = MaterialTheme.typography.titleMedium)
            }
        }
    ) { pad ->
        Column(Modifier.padding(pad).fillMaxSize()) {
            // Busca estilo marketplace: pill com ícone, fundo suave, sem borda dura
            OutlinedTextField(
                value = busca, onValueChange = { busca = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
                    .height(56.dp),
                shape = CircleShape,
                placeholder = { Text("Buscar anúncios") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (busca.isNotEmpty()) IconButton(onClick = { busca = "" }) {
                        Icon(Icons.Default.Close, contentDescription = "Limpar busca")
                    }
                },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                )
            )
            if (itens.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(horizontal = 40.dp)
                    ) {
                        Box(
                            Modifier
                                .size(112.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.AddAPhoto, contentDescription = null,
                                modifier = Modifier.size(44.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(Modifier.height(20.dp))
                        Text(
                            if (busca.isBlank()) "Sua vitre está vazia" else "Nada para \"$busca\"",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            if (busca.isBlank())
                                "Fotografe um item e a IA escreve o anúncio inteiro pra você."
                            else "Tente buscar por outro nome.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        if (busca.isBlank()) {
                            Spacer(Modifier.height(24.dp))
                            Button(
                                onClick = onNovoItem,
                                shape = RoundedCornerShape(16.dp),
                                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 14.dp)
                            ) {
                                Icon(Icons.Default.AddAPhoto, contentDescription = null, Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Começar a vender")
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
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
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(64.dp).clip(RoundedCornerShape(14.dp))) {
                FotoPrimeira(item.id, tamanho = 64)
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    item.titulo.ifEmpty { "(sem título)" },
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                val preco = item.precoFinal.takeIf { it > 0 } ?: item.precoSugerido
                Text(
                    "R$ ${"%.2f".format(preco)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                if (status.isNotEmpty()) {
                    Spacer(Modifier.height(6.dp))
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            status,
                            Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }
    }
}
