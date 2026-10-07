package br.com.anunciaai.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import br.com.anunciaai.AnunciaAIApp
import br.com.anunciaai.ui.EstadoVazio
import br.com.anunciaai.ui.IconePlataforma
import br.com.anunciaai.ui.foto.FotoPrimeira
import br.com.anunciaai.ui.theme.CorTexto
import br.com.anunciaai.ui.theme.CorTextoSec

/**
 * VITRINE (v12.0): grid 2 colunas do catálogo inteiro — tema escuro.
 * (Tela de apoio; as 6 da spec são as reescritas. Reescrita pro tema v12.)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VitrineScreen(
    onAbrirItem: (Long) -> Unit = {},
    onNovoItem: () -> Unit = {}
) {
    val app = androidx.compose.ui.platform.LocalContext.current.applicationContext as AnunciaAIApp
    val itens by app.repositorio.itens().collectAsState(initial = emptyList())
    val pubs by app.repositorio.publicacoes().collectAsState(initial = emptyList())

    Column(
        Modifier
            .fillMaxSize()
            .background(br.com.anunciaai.ui.theme.CorFundo)
    ) {
        TopAppBar(
            title = { Text("Vitrine", style = MaterialTheme.typography.headlineSmall, color = CorTexto) }
        )
        if (itens.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
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
                modifier = Modifier.fillMaxSize()
            ) {
                items(ordenados, key = { it.id }) { item ->
                    Card(
                        onClick = { onAbrirItem(item.id) },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainer
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(6.dp, RoundedCornerShape(16.dp))
                    ) {
                        Column {
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .height(150.dp)
                                    .padding(8.dp)
                                    .clip(RoundedCornerShape(12.dp))
                            ) {
                                FotoPrimeira(item.id, tamanho = 280)
                                Box(Modifier.align(Alignment.BottomStart).padding(8.dp)) {
                                    val plats = pubs.filter { it.itemId == item.id }.map { it.plataforma }
                                    plats.distinct().take(4).forEachIndexed { i, p ->
                                        Box(Modifier.padding(start = (i * 14).dp)) {
                                            IconePlataforma(p, tamanho = 22.dp)
                                        }
                                    }
                                }
                            }
                            Column(Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                                Text(
                                    item.titulo.ifEmpty { "(sem título)" },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = CorTextoSec,
                                    maxLines = 1, overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    "R$ ${"%.2f".format(item.precoFinal.takeIf { it > 0 } ?: item.precoSugerido)}",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = CorTexto
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
