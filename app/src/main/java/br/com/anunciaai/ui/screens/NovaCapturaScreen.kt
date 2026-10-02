package br.com.anunciaai.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview as CameraPreview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import br.com.anunciaai.AnunciaAIApp
import br.com.anunciaai.ui.foto.FotoUtil
import br.com.anunciaai.ui.theme.Destaque
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
    onItemCriado: (Long, String?) -> Unit,
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
    // PILAR 4: scanner de código de barras (ML Kit) — cliente EAN-13
    var escanerAtivo by remember { mutableStateOf(false) }
    var eanLido by remember { mutableStateOf<String?>(null) }
    val analisadorEan = remember {
        com.google.mlkit.vision.barcode.BarcodeScanning.getClient(
            com.google.mlkit.vision.barcode.BarcodeScannerOptions.Builder()
                .setBarcodeFormats(com.google.mlkit.vision.barcode.common.Barcode.FORMAT_EAN_13)
                .build()
        )
    }
    DisposableEffect(Unit) { onDispose { analisadorEan.close() } }

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
                if (ok) onItemCriado(id, eanLido)
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
                    if (copiou) onItemCriado(id, eanLido)
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
                onErro = { erro = it },
                escanerAtivo = escanerAtivo,
                onEanLido = { ean ->
                    if (eanLido == null && escanerAtivo) {
                        eanLido = ean
                        escanerAtivo = false
                    }
                },
                analisadorEan = analisadorEan
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

        // PILAR 4: EAN lido — chip verde no topo, acima do balão
        eanLido?.let { ean ->
            Surface(
                color = Destaque.copy(alpha = 0.92f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(top = 8.dp)
                    .align(Alignment.TopCenter)
            ) {
                Text(
                    "EAN $ean ✓",
                    Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    color = Color(0xFF12092B),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
            }
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
                // PILAR 4: botão do scanner de código de barras (EAN)
                IconButton(
                    onClick = { escanerAtivo = !escanerAtivo },
                    modifier = Modifier
                        .size(52.dp)
                        .background(
                            if (escanerAtivo) Destaque else Color.Black.copy(alpha = 0.4f),
                            CircleShape
                        )
                ) {
                    Icon(
                        Icons.Default.QrCodeScanner,
                        contentDescription = "Ler Código de Barras (EAN)",
                        tint = if (escanerAtivo) Color(0xFF12092B) else Color.White,
                        modifier = Modifier.size(22.dp)
                    )
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

/** Preview CameraX full-screen; registra o gatilho de captura no estado do pai.
 *  PILAR 4: quando escanerAtivo, analisa os frames com ML Kit (EAN-13). */
@Composable
private fun CameraImersiva(
    onGatilhoPronto: (() -> Unit) -> Unit,
    onFotoTirada: (File) -> Unit,
    onErro: (String) -> Unit,
    escanerAtivo: Boolean = false,
    onEanLido: (String) -> Unit = {},
    analisadorEan: com.google.mlkit.vision.barcode.BarcodeScanner? = null
) {
    val contexto = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val imageCapture = remember { ImageCapture.Builder().build() }
    val analiseEan = remember { ImageAnalysis.Builder().build() }

    // PILAR 4: (re)bind da câmera — roda no factory e a cada mudança do scanner
    fun reconstruirBind(view: PreviewView) {
        val providerFuture = ProcessCameraProvider.getInstance(view.context)
        providerFuture.addListener({
            val provider = providerFuture.get()
            val preview = CameraPreview.Builder()
                .build()
                .also { it.setSurfaceProvider(view.surfaceProvider) }
            try {
                provider.unbindAll()
                val casos = mutableListOf<androidx.camera.core.UseCase>(preview, imageCapture)
                if (escanerAtivo && analisadorEan != null) {
                    analiseEan.setAnalyzer(ContextCompat.getMainExecutor(view.context)) { proxy ->
                        @androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
                        val media = proxy.image
                        if (media != null) {
                            val entrada = com.google.mlkit.vision.common.InputImage.fromMediaImage(
                                media, proxy.imageInfo.rotationDegrees
                            )
                            analisadorEan.process(entrada)
                                .addOnSuccessListener { codigos ->
                                    codigos.firstOrNull()?.rawValue?.let { ean ->
                                        onEanLido(ean)
                                    }
                                }
                                .addOnCompleteListener { proxy.close() }
                        } else proxy.close()
                    }
                    casos.add(analiseEan)
                }
                provider.bindToLifecycle(
                    lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA,
                    *casos.toTypedArray()
                )
            } catch (e: Exception) {
                onErro("Câmera indisponível: ${e.message}")
            }
        }, ContextCompat.getMainExecutor(view.context))
    }

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { ctx ->
            PreviewView(ctx).apply {
                scaleType = PreviewView.ScaleType.FILL_CENTER
            }
        },
        // PILAR 4: re-bind quando o scanner liga/desliga (update roda na recomposição)
        update = { view -> reconstruirBind(view) }
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

/** Balão branco arredondado com seta pra baixo — flutua suavemente (v5.4). */
@Composable
private fun BalaoDica() {
    // flutuação sutil: sobe/desce 4dp em loop
    val transicao = rememberInfiniteTransition(label = "balao")
    val flutua by transicao.animateFloat(
        initialValue = -4f,
        targetValue = 4f,
        animationSpec = infiniteRepeatable(tween(1400), RepeatMode.Reverse),
        label = "flutua"
    )
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.graphicsLayer { translationY = flutua }
    ) {
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
                    color = Color(0xFF17171F)
                )
                Text(
                    "A IA fará a avaliação e a precificação",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF4B5570)
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
