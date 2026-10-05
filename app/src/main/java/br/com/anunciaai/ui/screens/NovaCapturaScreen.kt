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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import br.com.anunciaai.AnunciaAIApp
import br.com.anunciaai.ui.foto.FotoUtil
import br.com.anunciaai.ui.theme.CorTexto
import br.com.anunciaai.ui.theme.CorTextoSec
import br.com.anunciaai.ui.theme.Destaque
import kotlinx.coroutines.launch
import java.io.File

/**
 * TELA 2 — VENDER / CAPTURA (v12.0, reescrita do zero).
 * - Moldura de captura com BORDA PONTILHADA, preview de câmera real atrás
 * - Botão primário "Tirar foto" (sólido #00C896), secundário "Escolher da
 *   galeria (até 10)" como TEXTO SUBLINHADO (sem pill vazado)
 * - Tema escuro integral (fundo #12151A em toda parte)
 * - Scanner EAN (ML Kit) no botão QrCodeScanner — EAN vai junto pra análise
 */
@Composable
fun NovaCapturaScreen(
    onVoltar: () -> Unit,
    onItemCriado: (Long, String?) -> Unit
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
    var gatilhoCaptura by remember { mutableStateOf<(() -> Unit)?>(null) }
    // scanner EAN
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

    Box(Modifier.fillMaxSize().background(Color(0xFF12151A))) {
        if (temPermissao) {
            CameraComMoldura(
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
                    color = CorTexto, style = MaterialTheme.typography.titleMedium
                )
            }
        }

        // fechar
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

        // chip EAN lido
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
                    color = Color(0xFF04150F),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // ── base: botões da spec ──
        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 28.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (eanLido == null) {
                Text(
                    "Posicione o item na moldura",
                    style = MaterialTheme.typography.bodyMedium,
                    color = CorTextoSec
                )
                Spacer(Modifier.height(10.dp))
            }
            // primário: sólido destaque
            Button(
                onClick = { gatilhoCaptura?.invoke() },
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Destaque,
                    contentColor = Color(0xFF04150F)
                ),
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .height(52.dp)
            ) {
                Text("Tirar foto", style = MaterialTheme.typography.titleMedium)
            }
            Spacer(Modifier.height(12.dp))
            // secundário: texto sublinhado (sem pill vazado)
            Text(
                "Escolher da galeria (até 10)",
                style = MaterialTheme.typography.bodyMedium.copy(
                    textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline
                ),
                color = Destaque,
                modifier = Modifier.clickable {
                    galeria.launch(
                        PickVisualMediaRequest(
                            ActivityResultContracts.PickVisualMedia.ImageOnly
                        )
                    )
                }
            )
            Spacer(Modifier.height(16.dp))
            // scanner EAN (ML Kit) — terceiro controle
            OutlinedButton(
                onClick = { escanerAtivo = !escanerAtivo },
                shape = CircleShape,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = if (escanerAtivo) Color(0xFF04150F) else CorTexto,
                    containerColor = if (escanerAtivo) Destaque else Color.Transparent
                ),
                modifier = Modifier.size(52.dp)
            ) {
                Icon(
                    Icons.Default.QrCodeScanner,
                    contentDescription = "Ler Código de Barras (EAN)",
                    modifier = Modifier.size(22.dp)
                )
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

/** Câmera real com moldura de borda PONTILHADA por cima (spec TELA 2). */
@Composable
private fun CameraComMoldura(
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
                                    codigos.firstOrNull()?.rawValue?.let { ean -> onEanLido(ean) }
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

    Box(Modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                PreviewView(ctx).apply {
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                }
            },
            update = { view -> reconstruirBind(view) }
        )
        // moldura pontilhada central (spec TELA 2: borda pontilhada sobre a câmera)
        androidx.compose.foundation.Canvas(
            Modifier
                .align(Alignment.Center)
                .size(280.dp)
        ) {
            drawRoundRect(
                color = Color.White.copy(alpha = 0.85f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(24.dp.toPx()),
                style = androidx.compose.ui.graphics.drawscope.Stroke(
                    width = 2.dp.toPx(),
                    pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(
                        floatArrayOf(14f, 12f)
                    )
                )
            )
        }
    }

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
