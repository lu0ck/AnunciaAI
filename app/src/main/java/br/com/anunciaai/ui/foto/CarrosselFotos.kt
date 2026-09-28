package br.com.anunciaai.ui.foto

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Badge
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import br.com.anunciaai.dados.FotoItem

/**
 * Carrossel horizontal de fotos do item (v2): miniaturas, remover, reordenar
 * (subir/abaixar) e adicionar. Limite: 10.
 */
@Composable
fun CarrosselFotos(
    fotos: List<FotoItem>,
    onAdicionar: () -> Unit,
    onRemover: (FotoItem) -> Unit,
    onMover: (de: Int, para: Int) -> Unit
) {
    Column {
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 4.dp)
        ) {
            itemsIndexed(fotos, key = { _, f -> f.id }) { i, foto ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    FotoMini(foto.uri, tamanho = 84)
                    Text(
                        "${i + 1}/${fotos.size}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row {
                        IconButton(onClick = { if (i > 0) onMover(i, i - 1) }, enabled = i > 0) {
                            Icon(Icons.Default.ArrowBack, "Mover para trás", Modifier.size(16.dp))
                        }
                        IconButton(onClick = { onRemover(foto) }) {
                            Icon(Icons.Default.Close, "Remover foto", Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.error)
                        }
                        IconButton(
                            onClick = { if (i < fotos.size - 1) onMover(i, i + 1) },
                            enabled = i < fotos.size - 1
                        ) {
                            Icon(Icons.Default.ArrowForward, "Mover pra frente", Modifier.size(16.dp))
                        }
                    }
                }
            }
            item {
                if (fotos.size < 10) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        SmallFloatingActionButton(onClick = onAdicionar) {
                            Icon(Icons.Default.AddAPhoto, "Adicionar foto")
                        }
                        Text(
                            "adicionar",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    Badge { Text("10/10") }
                }
            }
        }
    }
}
