package br.com.anunciaai.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.material3.*
import br.com.anunciaai.AnunciaAIApp
import br.com.anunciaai.ui.BarraDockComBadge
import br.com.anunciaai.ui.IconePlataforma
import br.com.anunciaai.ui.Plataforma
import br.com.anunciaai.ui.Rotas
import br.com.anunciaai.ui.foto.FotoPrimeira
import br.com.anunciaai.ui.theme.Destaque
import br.com.anunciaai.ui.theme.Petroleo
import br.com.anunciaai.ui.theme.brilhoNeon

/**
 * Início (PASSO 1 — dashboard VendeAi):
 * - FAB verde-menta "+" no canto, acima da bottom bar (único ponto de entrada)
 * - Card de resumo com gradiente #00C896 → #0A5C6E, cantos 20dp e BRILHO GLOSSY,
 *   mostrando "Valor em estoque" (soma real dos itens não vendidos) + "+ Novo item"
 * - "Vendas por plataforma": TODAS as plataformas com badge quadrado da marca,
 *   valor vendido e selo % vs semana anterior (seta verde/vermelha)
 * - "Itens recentes": scroll HORIZONTAL com cards de foto, preço e selos empilhados
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

    // Valor em estoque: soma do preço dos itens ainda não vendidos
    val vendidosIds = pubs.filter { it.status == "VENDIDO" }.map { it.itemId }.toSet()
    val valorEstoque = itens.filter { it.id !in vendidosIds }
        .sumOf { it.precoFinal.takeIf { p -> p > 0 } ?: it.precoSugerido }

    // v5.4: count-up animado do valor (0 → valor real, 900ms spring suave)
    var animIniciou by remember { mutableStateOf(false) }
    LaunchedEffect(valorEstoque) {
        animIniciou = true
    }
    val proporcao by animateFloatAsState(
        targetValue = if (animIniciou) 1f else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow),
        label = "countup"
    )
    val valorAnimado = valorEstoque * proporcao

    // Vendas por plataforma: TODAS, com valor vendido e variação vs semana anterior
    val agora = System.currentTimeMillis()
    val semana = 7L * 24 * 3600 * 1000
    data class Venda(val total: Double, val anterior: Double)
    val vendasPorPlataforma = Plataforma.entries.associate { plat ->
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
        bottomBar = { BarraDockComBadge(Rotas.LISTA, onNavBottom) },
        // v6.1: FAB "Vender" CENTRAL ancorado pelo Scaffold sobre o vão do dock.
        // (O FAB antigo do canto foi removido — PASSO 1.)
        floatingActionButton = { FabCentral(onNavBottom) },
        floatingActionButtonPosition = FabPosition.Center
    ) { pad ->
        LazyColumn(
            Modifier.padding(pad).fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // ── Card de resumo: gradiente + brilho glossy + GLOW NEON (PILAR 2) ──
            item {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .brilhoNeon() // PILAR 2: glow verde-menta 10% por trás
                        .clip(RoundedCornerShape(20.dp))
                        .background(Brush.linearGradient(listOf(Destaque, Petroleo)))
                ) {
                    // gloss: faixa de luz diagonal no topo do card
                    Box(
                        Modifier
                            .matchParentSize()
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        Color.White.copy(alpha = 0.22f),
                                        Color.White.copy(alpha = 0.05f),
                                        Color.Transparent
                                    ),
                                    start = androidx.compose.ui.geometry.Offset(0f, 0f),
                                    end = androidx.compose.ui.geometry.Offset(0f, 220f)
                                )
                            )
                    )
                    Row(
                        Modifier.padding(20.dp).fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                "Valor em estoque",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFF04150F).copy(alpha = 0.75f)
                            )
                            Text(
                                "R$ ${"%,.2f".format(valorAnimado)}",
                                style = MaterialTheme.typography.displaySmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF04150F)
                            )
                            Text(
                                "${itens.size} ${if (itens.size == 1) "item" else "itens"} · " +
                                    "${pubs.count { it.status == "VENDIDO" }} vendidos",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF04150F).copy(alpha = 0.7f)
                            )
                        }
                        // v7: botão "+ Novo item" do topo REMOVIDO — o único ponto
                        // de entrada é o FAB central do Scaffold.
                    }
                }
            }

            // ── Vendas por plataforma: TODAS as marcas ──
            item {
                Text("Vendas por plataforma", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                Plataforma.entries.forEach { plat ->
                    val v = vendasPorPlataforma[plat.name] ?: Venda(0.0, 0.0)
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconePlataforma(plat.name, tamanho = 36.dp)
                        Spacer(Modifier.width(12.dp))
                        Text(
                            plat.rotulo,
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
                                            tint = if (pct >= 0) Color(0xFF65A30D) else Color(0xFFFF5470)
                                        )
                                        Spacer(Modifier.width(2.dp))
                                        Text(
                                            "${if (pct >= 0) "+" else ""}$pct%",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (pct >= 0) Color(0xFF65A30D) else Color(0xFFFF5470)
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

            // ── Vitrine: GRID de 2 colunas (ITEM 4) ──
            item {
                Spacer(Modifier.height(4.dp))
                Text("Itens recentes", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(10.dp))
            }
            item {
                if (itens.isEmpty()) {
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        br.com.anunciaai.ui.EstadoVazio(
                            icone = androidx.compose.material.icons.Icons.Outlined.Inventory2,
                            titulo = "Sua vitrine está vazia",
                            subtitulo = "Fotografe um item e a IA escreve o anúncio inteiro pra você.",
                            textoBotao = "Explorar itens",
                            onBotao = onNovoItem
                        )
                    }
                } else {
                    val ordenados = itens.sortedByDescending { it.dataCriacao }
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.height(((ordenados.size + 1) / 2 * 260).dp)
                    ) {
                        items(ordenados, key = { "it_${it.id}" }) { item ->
                            CardRecente(
                                item = item,
                                plataformas = pubs.filter { it.itemId == item.id }.map { it.plataforma },
                                onClick = { onAbrirItem(item.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Card do grid (ITEM 4): foto proeminente com selos SOBREPOSTOS no canto da foto,
 *  abaixo Nome (cinza claro) e Preço (branco, negrito). */
@Composable
private fun CardRecente(
    item: br.com.anunciaai.dados.Item,
    plataformas: List<String>,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            ) {
                FotoPrimeira(item.id, tamanho = 260)
                // selos das plataformas sobrepostos no canto da própria foto
                Box(Modifier.align(Alignment.BottomStart).padding(8.dp)) {
                    PilhaBadges(plataformas)
                }
            }
            Column(Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
                Text(
                    item.titulo.ifEmpty { "(sem título)" },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, // cinza claro (linha 1)
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    "R$ ${"%.2f".format(item.precoFinal.takeIf { it > 0 } ?: item.precoSugerido)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = androidx.compose.ui.graphics.Color(0xFFEDEFF7) // branco (linha 2)
                )
            }
        }
    }
}

private fun valorDoItem(itens: List<br.com.anunciaai.dados.Item>, itemId: Long): Double =
    itens.firstOrNull { it.id == itemId }?.let {
        it.precoFinal.takeIf { p -> p > 0 } ?: it.precoSugerido
    } ?: 0.0

/** Minibadges das plataformas empilhados, como moedas. */
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
