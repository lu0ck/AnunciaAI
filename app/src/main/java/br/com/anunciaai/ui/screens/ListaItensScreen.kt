package br.com.anunciaai.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import br.com.anunciaai.AnunciaAIApp
import br.com.anunciaai.ui.BarraInferior
import br.com.anunciaai.ui.IconePlataforma
import br.com.anunciaai.ui.Plataforma
import br.com.anunciaai.ui.Rotas
import br.com.anunciaai.ui.foto.FotoPrimeira
import br.com.anunciaai.ui.theme.CorTextoMarca
import br.com.anunciaai.ui.theme.Destaque
import br.com.anunciaai.ui.theme.Petroleo
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Início (v4): dashboard — resumo em gradiente, vendas por plataforma,
 * itens recentes com badges de marca em pilha.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListaItensScreen(
    onNovoItem: () -> Unit,
    onAbrirItem: (Long) -> Unit,
    onNavBottom: (String) -> Unit = {}
) {
    val app = LocalContext.current.applicationContext as AnunciaAIApp
    val itens by app.repositorio.itens().collectAsState(initial = emptyList())
    val pubs by app.repositorio.publicacoes().collectAsState(initial = emptyList())
    val contas by app.repositorio.contas().collectAsState(initial = emptyList())
    var busca by remember { mutableStateOf("") }

    // Valor em estoque: soma do preço de itens ainda não vendidos
    val vendidosIds = pubs.filter { it.status == "VENDIDO" }.map { it.itemId }.toSet()
    val valorEstoque = itens.filter { it.id !in vendidosIds }
        .sumOf { it.precoFinal.takeIf { p -> p > 0 } ?: it.precoSugerido }

    // Vendas por plataforma: itens VENDIDO por plataforma (valor e variação vs semana anterior)
    val conectadas = contas.map { it.plataforma }.toSet()
    val agora = System.currentTimeMillis()
    val semana = 7L * 24 * 3600 * 1000
    data class Venda(val total: Double, val anterior: Double)
    val vendasPorPlataforma = Plataforma.entries
        .filter { it.name in conectadas }
        .associate { plat ->
            val doPlat = pubs.filter { it.plataforma == plat.name && it.status == "VENDIDO" }
            val atual = doPlat.filter { (it.dataPublicacao ?: 0) > agora - semana }
                .sumOf { p -> valorDoItem(itens, p.itemId) }
            val anterior = doPlat.filter {
                val d = it.dataPublicacao ?: 0
                d > agora - 2 * semana && d <= agora - semana
            }.sumOf { p -> valorDoItem(itens, p.itemId) }
            plat.name to Venda(atual, anterior)
        }

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
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNovoItem,
                containerColor = Destaque,
                contentColor = Color(0xFF06231B)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Novo item")
            }
        },
        floatingActionButtonPosition = FabPosition.End
    ) { pad ->
        LazyColumn(
            Modifier.padding(pad).fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // ── Card de resumo: gradiente verde-menta → azul-petróleo, botão + dentro ──
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        Modifier
                            .background(Brush.linearGradient(listOf(Destaque, Petroleo)))
                            .padding(20.dp)
                            .fillMaxWidth()
                    ) {
                        Column {
                            Text(
                                "Valor em estoque",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFF06231B).copy(alpha = 0.8f)
                            )
                            Text(
                                "R$ ${"%,.2f".format(valorEstoque).let { it } }",
                                style = MaterialTheme.typography.displaySmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF04150F)
                            )
                            Text(
                                "${itens.size} ${if (itens.size == 1) "item" else "itens"} · ${pubs.count { it.status == "VENDIDO" }} vendidos",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF06231B).copy(alpha = 0.7f)
                            )
                        }
                        TextButton(
                            onClick = onNovoItem,
                            modifier = Modifier.align(Alignment.BottomEnd),
                            colors = ButtonDefaults.textButtonColors(
                                containerColor = Color(0xFF04150F).copy(alpha = 0.25f),
                                contentColor = Color.White
                            ),
                            shape = CircleShape
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Novo item", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }

            // ── Vendas por plataforma ──
            if (conectadas.isNotEmpty()) {
                item {
                    Text("Vendas por plataforma", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                }
                items(vendasPorPlataforma.entries.toList(), key = { it.key }) { (plat, v) ->
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconePlataforma(plat, tamanho = 36.dp)
                        Spacer(Modifier.width(12.dp))
                        Text(
                            Plataforma.doNome(plat)?.rotulo ?: plat,
                            Modifier.weight(1f),
                            style = MaterialTheme.typography.titleMedium
                        )
                        if (v.total > 0 || v.anterior > 0) {
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    "R$ ${"%,.2f".format(v.total)}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                if (v.anterior > 0) {
                                    val pct = ((v.total - v.anterior) / v.anterior * 100).toInt()
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            if (pct >= 0) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                                            contentDescription = null,
                                            Modifier.size(14.dp),
                                            tint = if (pct >= 0) Destaque else MaterialTheme.colorScheme.error
                                        )
                                        Text(
                                            "${if (pct >= 0) "+" else ""}$pct%",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (pct >= 0) Destaque else MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }
                        } else {
                            Text(
                                "Sem vendas ainda",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // ── Itens recentes ──
            item {
                Text("Itens recentes", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
            }
            item {
                OutlinedTextField(
                    value = busca, onValueChange = { busca = it },
                    modifier = Modifier.fillMaxWidth(),
                    shape = CircleShape,
                    placeholder = { Text("Buscar em itens recentes") },
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
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent
                    )
                )
                Spacer(Modifier.height(10.dp))
            }
            val filtrados = itens.filter {
                busca.isBlank() || it.titulo.contains(busca, ignoreCase = true)
            }.sortedByDescending { it.dataCriacao }
            if (filtrados.isEmpty()) {
                item {
                    Column(
                        Modifier.fillMaxWidth().padding(vertical = 40.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Outlined.PhotoCamera,
                            contentDescription = null,
                            Modifier.size(44.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            if (busca.isBlank()) "Sua vitrine está vazia"
                            else "Nada para \"$busca\"",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            if (busca.isBlank()) "Toque no + para fotografar um item"
                            else "Tente outro nome.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                items(filtrados, key = { "it_${it.id}" }) { item ->
                    val pubsDoItem = pubs.filter { it.itemId == item.id }
                    Card(
                        onClick = { onAbrirItem(item.id) },
                        shape = MaterialTheme.shapes.small,
                        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceContainer),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(56.dp).clip(MaterialTheme.shapes.small)) {
                                FotoPrimeira(item.id, tamanho = 56)
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    item.titulo.ifEmpty { "(sem título)" },
                                    style = MaterialTheme.typography.titleMedium,
                                    maxLines = 1, overflow = TextOverflow.Ellipsis
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    "R$ ${"%.2f".format(item.precoFinal.takeIf { it > 0 } ?: item.precoSugerido)}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Destaque
                                )
                                Spacer(Modifier.height(4.dp))
                                PilhaBadges(pubsDoItem.map { it.plataforma })
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun valorDoItem(itens: List<br.com.anunciaai.dados.Item>, itemId: Long): Double =
    itens.firstOrNull { it.id == itemId }?.let {
        it.precoFinal.takeIf { p -> p > 0 } ?: it.precoSugerido
    } ?: 0.0

/** Minibadges das plataformas em pilha sobreposta, como moedas. */
@Composable
private fun PilhaBadges(plataformas: List<String>) {
    if (plataformas.isEmpty()) return
    val mostradas = plataformas.distinct().take(4)
    Box(Modifier.height(24.dp)) {
        mostradas.forEachIndexed { i, plat ->
            Box(Modifier.offset(x = (i * 14).dp)) {
                IconePlataforma(plat, tamanho = 22.dp)
            }
        }
    }
}
