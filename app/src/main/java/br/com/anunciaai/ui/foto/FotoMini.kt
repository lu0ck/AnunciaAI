package br.com.anunciaai.ui.foto

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import br.com.anunciaai.dados.FotoItem

/**
 * Miniatura da foto real (sem Coil na v1): decodifica com inSampleSize pra
 * não estourar memória. Mostra borda quando selecionada (carrossel).
 */
@Composable
fun FotoMini(
    fotoUri: String?,
    tamanho: Int = 96,
    selecionada: Boolean = false,
    modifier: Modifier = Modifier
) {
    val bmp: Bitmap? by remember(fotoUri) {
        mutableStateOf(fotoUri?.let { decodificar(Uri.parse(it).path ?: "", tamanho) })
    }
    val borda = if (selecionada)
        Modifier.border(3.dp, MaterialTheme.colorScheme.primary, MaterialTheme.shapes.small)
    else Modifier
    if (bmp != null) {
        Image(
            bitmap = bmp!!.asImageBitmap(),
            contentDescription = "Foto do item",
            modifier = modifier.size(tamanho.dp).then(borda),
            contentScale = ContentScale.Crop
        )
    } else {
        Box(
            modifier = modifier
                .size(tamanho.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.shapes.small)
                .then(borda),
            contentAlignment = Alignment.Center
        ) {
            Text("📷", style = MaterialTheme.typography.titleLarge)
        }
    }
}

/** Primeira foto do item (pra lista de anúncios). */
@Composable
fun FotoPrimeira(itemId: Long, tamanho: Int = 72, modifier: Modifier = Modifier) {
    val app = androidx.compose.ui.platform.LocalContext.current.applicationContext as br.com.anunciaai.AnunciaAIApp
    val fotos by app.repositorio.fotosDoItem(itemId).collectAsState(initial = emptyList())
    FotoMini(fotos.firstOrNull()?.uri, tamanho, modifier = modifier)
}

private fun decodificar(caminho: String, tamanhoAlvo: Int): Bitmap? = try {
    val opcoes = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(caminho, opcoes)
    var amostra = 1
    while (opcoes.outWidth / (amostra * 2) >= tamanhoAlvo) amostra *= 2
    BitmapFactory.decodeFile(caminho, BitmapFactory.Options().apply { inSampleSize = amostra })
} catch (e: Exception) {
    null
}
