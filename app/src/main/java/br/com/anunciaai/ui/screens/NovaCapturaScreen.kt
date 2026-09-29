package br.com.anunciaai.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import br.com.anunciaai.AnunciaAIApp
import br.com.anunciaai.ui.BarraInferior
import br.com.anunciaai.ui.Rotas
import br.com.anunciaai.ui.foto.FotoUtil
import br.com.anunciaai.ui.theme.Destaque
import kotlinx.coroutines.launch

/**
 * Captura v2: tira 1+ fotos (câmera repetida) ou escolhe até 10 da galeria.
 * O item é criado vazio e as fotos entram na tabela FotoItem.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NovaCapturaScreen(
    onVoltar: () -> Unit,
    onItemCriado: (Long) -> Unit,
    onNavBottom: (String) -> Unit = {}
) {
    val contexto = LocalContext.current
    val app = contexto.applicationContext as AnunciaAIApp
    val escopo = rememberCoroutineScope()
    var erro by remember { mutableStateOf<String?>(null) }
    var itemIdAtual by remember { mutableStateOf<Long?>(null) }
    var fotoCameraUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var arquivoCamera by remember { mutableStateOf<java.io.File?>(null) }
    var contagem by remember { mutableStateOf(0) }

    // cria o item uma vez (vazio); fotos entram depois
    fun itemAtualOuNovo(onPronto: (Long) -> Unit) {
        val atual = itemIdAtual
        if (atual != null) { onPronto(atual); return }
        escopo.launch {
            try {
                val id = app.repositorio.salvarItem(
                    br.com.anunciaai.dados.Item(titulo = "", descricao = "", categoria = "",
                        precoSugerido = 0.0, precoFinal = 0.0)
                )
                itemIdAtual = id
                onPronto(id)
            } catch (e: Exception) {
                erro = "Erro ao salvar: ${e.message}"
            }
        }
    }

    fun adicionarFoto(uriCopiada: String) {
        itemAtualOuNovo { id ->
            escopo.launch {
                val ok = app.repositorio.adicionarFoto(id, uriCopiada)
                if (ok) {
                    contagem++
                    if (contagem >= 1) {
                        // 1ª foto: já vai pra revisão (momento principal)
                        onItemCriado(id)
                    }
                } else {
                    erro = "Limite de 10 fotos por item."
                }
            }
        }
    }

    val tirarFoto = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        val arquivo = arquivoCamera
        if (ok && arquivo != null) {
            FotoUtil.corrigirRotacao(contexto, arquivo)
            val copiada = FotoUtil.copiarParaInterno(contexto, android.net.Uri.fromFile(arquivo))
            if (copiada != null) adicionarFoto(copiada) else erro = "Não consegui salvar a foto."
        }
    }

    val galeria = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(maxItems = 10)
    ) { uris ->
        if (uris.isNotEmpty()) {
            itemAtualOuNovo { id ->
                escopo.launch {
                    var primeira = true
                    for (uri in uris) {
                        val copiada = FotoUtil.copiarParaInterno(contexto, uri)
                        if (copiada != null) {
                            val ok = app.repositorio.adicionarFoto(id, copiada)
                            if (ok && primeira) { primeira = false; contagem = 1 }
                        }
                    }
                    if (!primeira || contagem >= 1) onItemCriado(id)
                    else erro = "Não consegui copiar as fotos."
                }
            }
        }
    }

    val pedirPermissao = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { concedida ->
        if (concedida) {
            val arquivo = FotoUtil.novoArquivoFoto(contexto)
            arquivoCamera = arquivo
            fotoCameraUri = androidx.core.content.FileProvider.getUriForFile(
                contexto, "${contexto.packageName}.fileprovider", arquivo
            )
            fotoCameraUri?.let { tirarFoto.launch(it) }
        } else {
            erro = "Sem permissão de câmera. Use a galeria ou conceda a permissão."
        }
    }

    fun iniciarCamera() {
        val concedida = ContextCompat.checkSelfPermission(contexto, Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED
        if (concedida) {
            val arquivo = FotoUtil.novoArquivoFoto(contexto)
            arquivoCamera = arquivo
            fotoCameraUri = androidx.core.content.FileProvider.getUriForFile(
                contexto, "${contexto.packageName}.fileprovider", arquivo
            )
            fotoCameraUri?.let { tirarFoto.launch(it) }
        } else {
            pedirPermissao.launch(Manifest.permission.CAMERA)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Vender", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = onVoltar) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                }
            )
        },
        bottomBar = { BarraInferior(Rotas.CAPTURA, onNavBottom) }
    ) { pad ->
        Column(
            Modifier.padding(pad).padding(horizontal = 28.dp).fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Moldura com cantos pontilhados: "encaixe o item aqui" (sem círculo colorido)
            Box(
                Modifier
                    .size(190.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .dashedBorder(
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(24.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Outlined.PhotoCamera, contentDescription = null,
                    modifier = Modifier.size(52.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(24.dp))
            Text(
                "Fotografe o item",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Várias fotos ajudam a IA a ver o estado real — defeito vira desconto no preço sugerido.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(Modifier.height(32.dp))
            // Primário: sólido, cor de destaque única
            Button(
                onClick = { iniciarCamera() },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Destaque,
                    contentColor = Color(0xFF06231B)
                )
            ) {
                Icon(Icons.Default.PhotoCamera, contentDescription = null, Modifier.size(20.dp))
                Spacer(Modifier.width(10.dp))
                Text("Tirar foto", style = MaterialTheme.typography.titleMedium)
            }
            Spacer(Modifier.height(16.dp))
            // Secundário: texto sublinhado simples — não outro pill vazado
            Text(
                "Escolher da galeria (até 10)",
                style = MaterialTheme.typography.titleSmall,
                color = Destaque,
                textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable {
                        galeria.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    }
                    .padding(8.dp)
            )
            erro?.let {
                Spacer(Modifier.height(16.dp))
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

/** Borda pontilhada (moldura de "encaixe o item aqui"). */
private fun Modifier.dashedBorder(color: Color, shape: RoundedCornerShape): Modifier =
    this.then(
        Modifier.drawBehind {
            val stroke = 2.dp.toPx()
            val path = androidx.compose.ui.graphics.Path().apply {
                addRoundRect(
                    androidx.compose.ui.geometry.RoundRect(
                        rect = androidx.compose.ui.geometry.Rect(
                            left = stroke / 2, top = stroke / 2,
                            right = size.width - stroke / 2, bottom = size.height - stroke / 2
                        ),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(
                            24.dp.toPx() / 2, 24.dp.toPx() / 2
                        )
                    )
                )
            }
            drawPath(
                path = path,
                color = color,
                style = androidx.compose.ui.graphics.drawscope.Stroke(
                    width = stroke,
                    pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(18f, 14f))
                )
            )
        }
    )
