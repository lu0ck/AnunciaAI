package br.com.anunciaai.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import br.com.anunciaai.ui.IconePlataforma
import br.com.anunciaai.ui.Plataforma
import br.com.anunciaai.ui.foto.FotoPrimeira
import br.com.anunciaai.ui.theme.CorTexto
import br.com.anunciaai.ui.theme.CorTextoSec
import br.com.anunciaai.ui.theme.Destaque
import br.com.anunciaai.ui.theme.Petroleo
import br.com.anunciaai.ui.theme.VerdePositivo
import br.com.anunciaai.ui.theme.CorErro

/**
 * TELA 1 — INÍCIO (v12.0, reescrita do zero).
 * 1. Hero card topo: gradiente #00C896 → #0A5C6E, cantos 20dp, "Valor em
 *    estoque" + soma dos itens não vendidos + botão "+ Novo item" DENTRO do card.
 * 2. "Vendas por plataforma": uma linha por plataforma CONECTADA, badge quadrado
 *    12dp na cor real da marca, valor vendido (ou "Sem vendas ainda"), selo % verde/vermelho.
 * 3. "Itens recentes": LazyVerticalGrid(GridCells.Fixed(2)) DE NÍVEL SUPERIOR —
 *    nunca aninhado em LazyColumn. Foto real, nome, preço CorTexto peso 800,
 *    badges pequenos sobrepostos das plataformas publicadas.
 * 4. Zero decoração de fundo — fundo liso #12151A (via container do tema).
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun ListaItensScreen(
    onNovoItem: () -> Unit,
    onAbrirItem: (Long) -> Unit
) {
    val app = LocalContext.current.applicationContext as AnunciaAIApp
    val itens by app.repositorio.itens().collectAsState(initial = emptyList())
    val pubs by app.repositorio.publicacoes().collectAsState(initial = emptyList())
    val contas by app.repositorio.contas().collectAsState(initial = emptyList())

    // Valor em estoque: soma do preço dos itens ainda não vendidos
    val vendidosIds = pubs.filter { it.status == "VENDIDO" }.map { it.itemId }.toSet()
    val valorEstoque = itens.filter { it.id !in vendidosIds }
        .sumOf { it.precoFinal.takeIf { p -> p > 0 } ?: it.precoSugerido }

    // Vendas por plataforma — só CONECTADAS (spec v12)
    val conectadas = contas.map { it.plataforma }.toSet()
    val agora = System.currentTimeMillis()
    val semana = 7L * 24 * 3600 * 1000

    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = {
                Text(
                    "AnunciaAI",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = CorTexto
                )
            },
            actions = {
                Text(
                    "v${br.com.anunciaai.BuildConfig.VERSION_NAME}",
                    Modifier.padding(end = 20.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = CorTextoSec
                )
            }
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            // ── 1. Hero card (largura toda) ──
            item(span = { GridItemSpan(2) }) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(Brush.linearGradient(listOf(Destaque, Petroleo)))
                ) {
                    Column(Modifier.padding(20.dp)) {
                        Text(
                            "Valor em estoque",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF04150F).copy(alpha = 0.75f)
                        )
                        Text(
                            "R$ %,.2f".format(valorEstoque),
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
                        Spacer(Modifier.height(12.dp))
                        Button(
                            onClick = onNovoItem,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF04150F),
                                contentColor = Destaque
                            ),
                            modifier = Modifier.height(44.dp)
                        ) {
                            Text("+ Novo item", style = MaterialTheme.typography.titleSmall)
                        }
                    }
                }
            }

            // ── 2. Vendas por plataforma (só CONECTADAS) ──
            item(span = { GridItemSpan(2) }) {
                Column {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Vendas por plataforma",
                        style = MaterialTheme.typography.titleMedium,
                        color = CorTexto
                    )
                    Spacer(Modifier.height(6.dp))
                    val plataformasConectadas = Plataforma.entries
                        .filter { conectadas.contains(it.name) }
                    if (plataformasConectadas.isEmpty()) {
                        Text(
                            "Nenhuma plataforma conectada — conecte em Configurações.",
                            style = MaterialTheme.typography.bodySmall,
                            color = CorTextoSec
                        )
                    }
                    plataformasConectadas.forEach { plat ->
                        val doPlat = pubs.filter { it.plataforma == plat.name && it.status == "VENDIDO" }
                        val atual = doPlat.filter { (it.dataPublicacao ?: 0) > agora - semana }
                            .sumOf { p -> valorDoItem(itens, p.itemId) }
                        val anterior = doPlat.filter {
                            val d = it.dataPublicacao ?: 0
                            d > agora - 2 * semana && d <= agora - semana
                        }.sumOf { p -> valorDoItem(itens, p.itemId) }
                        Row(
                            Modifier.fillMaxWidth().padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconePlataforma(plat.name, conectada = true, tamanho = 32.dp)
                            Spacer(Modifier.width(12.dp))
                            Text(
                                plat.rotulo,
                                Modifier.weight(1f),
                                style = MaterialTheme.typography.titleMedium,
                                color = CorTexto
                            )
                            Column(horizontalAlignment = Alignment.End) {
                                if (atual > 0 || anterior > 0) {
                                    Text(
                                        "R$ %,.2f".format(atual),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = CorTexto
                                    )
                                    if (anterior > 0) {
                                        val pct = (((atual - anterior) / anterior) * 100).toInt()
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                if (pct >= 0) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                                                contentDescription = null,
                                                Modifier.size(14.dp),
                                                tint = if (pct >= 0) VerdePositivo else CorErro
                                            )
                                            Spacer(Modifier.width(2.dp))
                                            Text(
                                                "${if (pct >= 0) "+" else ""}$pct%",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = if (pct >= 0) VerdePositivo else CorErro
                                            )
                                        }
                                    }
                                } else {
                                    Text(
                                        "Sem vendas ainda",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = CorTextoSec
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ── 3. Itens recentes — grid 2 colunas ──
            item(span = { GridItemSpan(2) }) {
                Text(
                    "Itens recentes",
                    style = MaterialTheme.typography.titleMedium,
                    color = CorTexto
                )
                Spacer(Modifier.height(4.dp))
            }
            val ordenados = itens.sortedByDescending { it.dataCriacao }
            if (ordenados.isEmpty()) {
                item(span = { GridItemSpan(2) }) {
                    Text(
                        "Sua vitrine está vazia — toque em + para fotografar o primeiro item.",
                        style = MaterialTheme.typography.bodySmall,
                        color = CorTextoSec,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                }
            }
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

/** Card do item: foto real, nome, preço CorTexto peso 800, badges sobrepostos. */
@Composable
private fun CardRecente(
    item: br.com.anunciaai.dados.Item,
    plataformas: List<String>,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
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
                // badges pequenos das plataformas publicadas, sobrepostos na foto
                Box(Modifier.align(Alignment.BottomStart).padding(8.dp)) {
                    PilhaBadges(plataformas)
                }
            }
            Column(Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
                Text(
                    item.titulo.ifEmpty { "(sem título)" },
                    style = MaterialTheme.typography.bodyMedium,
                    color = CorTextoSec,
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    "R$ %.2f".format(item.precoFinal.takeIf { it > 0 } ?: item.precoSugerido),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = CorTexto
                )
            }
        }
    }
}

private fun valorDoItem(
    itens: List<br.com.anunciaai.dados.Item>,
    itemId: Long
): Double =
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
            Box(Modifier.padding(start = (i * 14).dp)) {
                IconePlataforma(plat, tamanho = 22.dp)
            }
        }
    }
}
