package br.com.anunciaai.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.PhotoCamera
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
import br.com.anunciaai.ui.BarraDockNova
import br.com.anunciaai.ui.EstadoVazio
import br.com.anunciaai.ui.IconePlataforma
import br.com.anunciaai.ui.Rotas
import br.com.anunciaai.ui.foto.FotoPrimeira
import br.com.anunciaai.ui.theme.Destaque

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
        bottomBar = { BarraDockNova(Rotas.VITRINE, onNavBottom) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNovoItem,
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
                    Card(
                        onClick = { onAbrirItem(item.id) },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainer
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Column {
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .height(140.dp)
                                    .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
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
                            Column(Modifier.padding(10.dp)) {
                                Text(
                                    item.titulo.ifEmpty { "(sem título)" },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1, overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    "R$ ${"%.2f".format(item.precoFinal.takeIf { it > 0 } ?: item.precoSugerido)}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFFF2F2F0)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
