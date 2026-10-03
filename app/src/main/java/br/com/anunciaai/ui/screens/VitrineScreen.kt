package br.com.anunciaai.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import br.com.anunciaai.AnunciaAIApp
import br.com.anunciaai.ui.BarraDockComBadge
import br.com.anunciaai.ui.EstadoVazio
import br.com.anunciaai.ui.IconePlataforma
import br.com.anunciaai.ui.Rotas
import br.com.anunciaai.ui.foto.FotoPrimeira
import br.com.anunciaai.ui.theme.Destaque
import kotlinx.coroutines.delay

/**
 * Vitrine (v6): grid 2 colunas de TODOS os itens — a "loja" completa.
 * (O Início/dashboard mostra o resumo + recentes; aqui é o catálogo inteiro.)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VitrineScreen(
    onAbrirItem: (Long) -> Unit = {},
    onNovoItem: () -> Unit = {},
    onNavBottom: (String) -> Unit = {}
) {
    val app = LocalContext.current.applicationContext as AnunciaAIApp
    val itens by app.repositorio.itens().collectAsState(initial = emptyList())
    val pubs by app.repositorio.publicacoes().collectAsState(initial = emptyList())

    Scaffold(
        topBar = { TopAppBar(title = { Text("Vitrine") }) },
        bottomBar = { BarraDockComBadge(Rotas.VITRINE, onNavBottom) }
        // v11.1: FAB duplicado EXTINTO — o "+" vive UMA vez, integrado na cápsula (BarraDockNova).
    ) { pad ->
        if (itens.isEmpty()) {
            Box(Modifier.padding(pad).fillMaxSize(), contentAlignment = Alignment.Center) {
                EstadoVazio(
                    icone = Icons.Outlined.PhotoCamera,
                    titulo = "Sua vitrine está vazia",
                    subtitulo = "Fotografe um item e a IA escreve o anúncio inteiro pra você."
                )
            }
        } else {
            val ordenados = itens.sortedByDescending { it.dataCriacao }
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(16.dp),
                modifier = Modifier.padding(pad).fillMaxSize()
            ) {
                items(ordenados, key = { it.id }) { item ->
                    // PILAR 2: itens surgem um a um (baixo→cima + fade), escalonados
                    var visivel by remember { mutableStateOf(false) }
                    LaunchedEffect(item.id) {
                        delay((ordenados.indexOfFirst { it.id == item.id }.coerceAtMost(8) * 60).toLong())
                        visivel = true
                    }
                    AnimatedVisibility(
                        visible = visivel,
                        enter = slideInVertically(
                            initialOffsetY = { it / 3 },
                            animationSpec = tween(350)
                        ) + fadeIn(tween(350))
                    ) {
                    Card(
                        onClick = { onAbrirItem(item.id) },
                        shape = RoundedCornerShape(24.dp), // v11 bike: cantos 24dp
                        colors = CardDefaults.cardColors(
                            containerColor = androidx.compose.ui.graphics.Color.White // card branco
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(8.dp, RoundedCornerShape(24.dp))
                    ) {
                        Column {
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .height(150.dp)  // v11: imagem domina o card
                                    .padding(10.dp)
                                    .clip(RoundedCornerShape(18.dp))
                            ) {
                                FotoPrimeira(item.id, tamanho = 280)
                                Box(Modifier.align(Alignment.BottomStart).padding(8.dp)) {
                                    val plats = pubs.filter { it.itemId == item.id }.map { it.plataforma }
                                    plats.distinct().take(4).forEachIndexed { i, p ->
                                        Box(Modifier.offset(x = (i * 14).dp)) {
                                            IconePlataforma(p, tamanho = 22.dp)
                                        }
                                    }
                                }
                            }
                            Column(Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                                Text(
                                    item.titulo.ifEmpty { "(sem título)" },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1, overflow = TextOverflow.Ellipsis
                                )
                                // v11.1 (fix #4): categoria + preço na MESMA linha, mesma
                                // altura: categoria com weight(1f) truncada com reticências;
                                // preço GIGANTE ExtraBold na cor de texto principal.
                                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
                                    Text(
                                        item.categoria,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        "R$ ${"%.2f".format(item.precoFinal.takeIf { it > 0 } ?: item.precoSugerido)}",
                                        style = MaterialTheme.typography.headlineSmall, // v11: preço GIGANTE
                                        fontWeight = FontWeight.ExtraBold,
                                        color = androidx.compose.ui.graphics.Color(0xFF17171F)
                                    )
                                }
                            }
                        }
                    }
                    } // fim AnimatedVisibility
                }
            }
        }
    }
}
