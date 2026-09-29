package br.com.anunciaai.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview as CameraPreview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import br.com.anunciaai.AnunciaAIApp
import br.com.anunciaai.ui.foto.FotoUtil
import kotlinx.coroutines.launch
import java.io.File

/**
 * Vender (v5.2 — ITEM 1 da spec visual): câmera IMERSIVA full-screen.
 * - Preview CameraX ocupando a tela toda (moldura tracejada removida)
 * - 4 cantoneiras brancas finas de enquadramento no centro
 * - Balão branco com seta pra baixo flutuando acima do botão de captura:
 *   "Fotografe o item" (negrito) / "A IA fará a avaliação e a precificação"
 * - Botão de captura grande + galeria; foto tirada vai direto pra análise da IA
 */
@Composable
fun NovaCapturaScreen(
    onVoltar: () -> Unit,
    onItemCriado: (Long) -> Unit,
    onNavBottom: (String) -> Unit = {}
) {
    val contexto = LocalContext.current
    val app = contexto.applicationContext as AnunciaAIApp
    val escopo = rememberCoroutineScope()

    var temPermissao by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(contexto, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    var erro by remember { mutableStateOf<String?>(null) }
    // gatilho de captura registrado pela câmera (hoisted state)
    var gatilhoCaptura by remember { mutableStateOf<(() -> Unit)?>(null) }

    // cria o item vazio uma vez; fotos entram na tabela FotoItem
    var itemIdAtual by remember { mutableStateOf<Long?>(null) }
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
                if (ok) onItemCriado(id)
                else erro = "Limite de 10 fotos por item."
            }
        }
    }

    val pedirPermissao = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { concedida ->
        temPermissao = concedida
        if (!concedida) erro = "Sem permissão de câmera — use a galeria."
    }
    LaunchedEffect(Unit) {
        if (!temPermissao) pedirPermissao.launch(Manifest.permission.CAMERA)
    }

    val galeria = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(maxItems = 10)
    ) { uris ->
        if (uris.isNotEmpty()) {
            itemAtualOuNovo { id ->
                escopo.launch {
                    var copiou = false
                    for (uri in uris) {
                        val copiada = FotoUtil.copiarParaInterno(contexto, uri)
                        if (copiada != null && app.repositorio.adicionarFoto(id, copiada)) copiou = true
                    }
                    if (copiou) onItemCriado(id)
                    else erro = "Não consegui copiar as fotos."
                }
            }
        }
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        if (temPermissao) {
            CameraImersiva(
                onGatilhoPronto = { gatilhoCaptura = it },
                onFotoTirada = { arquivo ->
                    FotoUtil.corrigirRotacao(contexto, arquivo)
                    val copiada = FotoUtil.copiarParaInterno(
                        contexto, android.net.Uri.fromFile(arquivo)
                    )
                    if (copiada != null) adicionarFoto(copiada)
                    else erro = "Não consegui salvar a foto."
                },
                onErro = { erro = it }
            )
        } else {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    "Precisamos da permissão da câmera",
                    color = Color.White, style = MaterialTheme.typography.titleMedium
                )
            }
        }

        // ── Camada de UI sobre a câmera ──
        // Fechar (topo esquerdo)
        IconButton(
            onClick = onVoltar,
            modifier = Modifier
                .statusBarsPadding()
                .padding(8.dp)
                .size(44.dp)
                .background(Color.Black.copy(alpha = 0.4f), CircleShape)
        ) {
            Icon(Icons.Default.Close, "Fechar", tint = Color.White)
        }

        // Cantoneiras de enquadramento (centro)
        CantoneirasEnquadramento()

        // Balão de dica + controles (base)
        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            BalaoDica()
            Spacer(Modifier.height(18.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(34.dp)
            ) {
                IconButton(
                    onClick = {
                        galeria.launch(
                            PickVisualMediaRequest(
                                ActivityResultContracts.PickVisualMedia.ImageOnly
                            )
                        )
                    },
                    modifier = Modifier
                        .size(52.dp)
                        .background(Color.Black.copy(alpha = 0.4f), CircleShape)
                ) {
                    Icon(Icons.Default.Image, "Galeria", tint = Color.White,
                        modifier = Modifier.size(22.dp))
                }
                // Botão de captura grande — dispara o takePicture da CameraX
                Box(
                    Modifier
                        .size(76.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.25f), CircleShape)
                        .padding(6.dp)
                        .background(Color.White, CircleShape)
                        .clickable { gatilhoCaptura?.invoke() },
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        Modifier
                            .size(26.dp)
                            .background(Color.Black.copy(alpha = 0.85f), CircleShape)
                    )
                }
                // peso simétrico à esquerda (troca de lente numa próxima iteração)
                Box(Modifier.size(52.dp))
            }
        }

        erro?.let {
            Surface(
                color = MaterialTheme.colorScheme.errorContainer,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(24.dp)
            ) {
                Text(
                    it, Modifier.padding(14.dp),
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

/** Preview CameraX full-screen; registra o gatilho de captura no estado do pai. */
@Composable
private fun CameraImersiva(
    onGatilhoPronto: (() -> Unit) -> Unit,
    onFotoTirada: (File) -> Unit,
    onErro: (String) -> Unit
) {
    val contexto = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val imageCapture = remember { ImageCapture.Builder().build() }

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { ctx ->
            val previewView = PreviewView(ctx).apply {
                scaleType = PreviewView.ScaleType.FILL_CENTER
            }
            val providerFuture = ProcessCameraProvider.getInstance(ctx)
            providerFuture.addListener({
                val provider = providerFuture.get()
                val preview = CameraPreview.Builder()
                    .build()
                    .also { it.setSurfaceProvider(previewView.surfaceProvider) }
                try {
                    provider.unbindAll()
                    provider.bindToLifecycle(
                        lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA,
                        preview, imageCapture
                    )
                } catch (e: Exception) {
                    onErro("Câmera indisponível: ${e.message}")
                }
            }, ContextCompat.getMainExecutor(ctx))
            previewView
        }
    )

    DisposableEffect(Unit) {
        onGatilhoPronto {
            val arquivo = FotoUtil.novoArquivoFoto(contexto)
            val opcoes = ImageCapture.OutputFileOptions.Builder(arquivo).build()
            imageCapture.takePicture(
                opcoes,
                ContextCompat.getMainExecutor(contexto),
                object : ImageCapture.OnImageSavedCallback {
                    override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                        onFotoTirada(arquivo)
                    }
                    override fun onError(exception: ImageCaptureException) {
                        onErro("Falha ao capturar: ${exception.message}")
                    }
                }
            )
        }
        onDispose { }
    }
}

/** 4 cantoneiras brancas finas no centro da tela (enquadramento sutil). */
@Composable
private fun CantoneirasEnquadramento() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        val lado = 280.dp
        val espessura = 3.dp
        val comprimento = 36.dp
        Canvas(Modifier.size(lado)) {
            val w = this.size.width
            val h = this.size.height
            val stroke = espessura.toPx()
            val len = comprimento.toPx()
            val cor = Color.White.copy(alpha = 0.9f)
            listOf(
                0f to 0f, w to 0f, 0f to h, w to h
            ).forEach { (x, y) ->
                val dx = if (x == 0f) 1f else -1f
                val dy = if (y == 0f) 1f else -1f
                drawLine(cor, Offset(x, y), Offset(x + dx * len, y), strokeWidth = stroke, cap = StrokeCap.Round)
                drawLine(cor, Offset(x, y), Offset(x, y + dy * len), strokeWidth = stroke, cap = StrokeCap.Round)
            }
        }
    }
}

/** Balão branco arredondado com seta pra baixo (aponta pro botão de captura). */
@Composable
private fun BalaoDica() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box {
            Column(
                Modifier
                    .background(Color.White, RoundedCornerShape(16.dp))
                    .padding(horizontal = 18.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Fotografe o item",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF12151A)
                )
                Text(
                    "A IA fará a avaliação e a precificação",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF4B505B)
                )
            }
            // seta do balão (triângulo branco logo abaixo do corpo, centralizada)
            Canvas(
                Modifier
                    .align(Alignment.BottomCenter)
                    .offset(y = 9.dp)
                    .size(width = 18.dp, height = 10.dp)
            ) {
                val path = Path().apply {
                    moveTo(0f, 0f)
                    lineTo(size.width, 0f)
                    lineTo(size.width / 2f, size.height)
                    close()
                }
                drawPath(path, Color.White)
            }
        }
    }
}
