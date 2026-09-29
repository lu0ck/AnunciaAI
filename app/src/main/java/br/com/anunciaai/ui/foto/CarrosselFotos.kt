package br.com.anunciaai.ui.foto

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.size
import br.com.anunciaai.dados.FotoItem
import br.com.anunciaai.ui.theme.Destaque

/**
 * Carrossel horizontal de fotos do item (v4.1): miniaturas GRANDES (96dp), alinhadas
 * na mesma altura, indicador em DOTS abaixo (não número "1/1"), botão adicionar do
 * mesmo tamanho das fotos. Ações por foto: remover (X) e reordenar (setas).
 */
@Composable
fun CarrosselFotos(
    fotos: List<FotoItem>,
    onAdicionar: () -> Unit,
    onRemover: (FotoItem) -> Unit,
    onMover: (de: Int, para: Int) -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            itemsIndexed(fotos, key = { _, f -> f.id }) { i, foto ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box {
                        FotoMini(foto.uri, tamanho = 96)
                        // remover: X pequeno no canto da foto
                        Surface(
                            onClick = { onRemover(foto) },
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(4.dp)
                        ) {
                            Icon(Icons.Default.Close, "Remover foto", Modifier.padding(4.dp).size(14.dp),
                                tint = MaterialTheme.colorScheme.error)
                        }
                    }
                    if (fotos.size > 1) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("‹", style = MaterialTheme.typography.titleMedium,
                                color = if (i > 0) MaterialTheme.colorScheme.onSurface
                                else MaterialTheme.colorScheme.surfaceContainerHigh,
                                modifier = Modifier
                                    .clickable(enabled = i > 0) { if (i > 0) onMover(i, i - 1) }
                                    .padding(horizontal = 8.dp))
                            Text("›", style = MaterialTheme.typography.titleMedium,
                                color = if (i < fotos.size - 1) MaterialTheme.colorScheme.onSurface
                                else MaterialTheme.colorScheme.surfaceContainerHigh,
                                modifier = Modifier
                                    .clickable(enabled = i < fotos.size - 1) { onMover(i, i + 1) }
                                    .padding(horizontal = 8.dp))
                        }
                    }
                }
            }
            item {
                if (fotos.size < 10) {
                    // botão adicionar do MESMO tamanho das fotos, alinhado na mesma altura
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            Modifier
                                .size(96.dp)
                                .background(
                                    MaterialTheme.colorScheme.surfaceContainer,
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable { onAdicionar() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Add, "Adicionar foto",
                                Modifier.size(32.dp), tint = Destaque
                            )
                        }
                    }
                } else {
                    Text(
                        "limite: 10 fotos",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        // indicador em DOTS (um por foto), não número
        if (fotos.size > 1) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                repeat(fotos.size) { i ->
                    Box(
                        Modifier
                            .size(6.dp)
                            .background(
                                if (i == 0) Destaque else MaterialTheme.colorScheme.surfaceContainerHigh,
                                CircleShape
                            )
                    )
                }
            }
        }
    }
}
