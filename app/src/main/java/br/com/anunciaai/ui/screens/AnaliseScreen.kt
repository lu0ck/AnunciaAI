package br.com.anunciaai.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Title
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.compose.foundation.layout.statusBarsPadding
import br.com.anunciaai.AnunciaAIApp
import br.com.anunciaai.ia.FabricaIA
import br.com.anunciaai.ui.foto.FotoUtil
import br.com.anunciaai.ui.theme.Destaque
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Análise da IA (v4): pilha de cartões estilo "leque de Polaroid", um por campo,
 * na ordem Categoria → Título → Descrição → Preço → Condição.
 * - Card ainda não preenchido: neutro/apagado
 * - Campo sendo preenchido AGORA: na frente, maior, fundo mais claro
 * - Preenchido: check verde #00C896 à direita + valor gerado
 * Ao final: botão "Ver anúncio" → revisão com campos preenchidos.
 */
@Composable
fun AnaliseScreen(
    itemId: Long,
    onVerAnuncio: (Long) -> Unit,
    onVoltar: () -> Unit
) {
    val contexto = LocalContext.current
    val app = contexto.applicationContext as AnunciaAIApp
    val escopo = rememberCoroutineScope()

    val item by app.repositorio.item(itemId).collectAsState(initial = null)
    val fotos by app.repositorio.fotosDoItem(itemId).collectAsState(initial = emptyList())

    // Campos que a IA preenche (estado da animação)
    data class Campo(val nome: String, val icone: ImageVector)
    val campos = listOf(
        Campo("Categoria", Icons.Default.Category),
        Campo("Título", Icons.Default.Title),
        Campo("Descrição", Icons.Outlined.Description),
        Campo("Preço", Icons.Default.Payments),
        Campo("Condição", Icons.Default.LocalOffer)
    )
    var preenchidos by remember { mutableStateOf(mapOf<String, String>()) }
    var indiceAtivo by remember { mutableStateOf(0) }
    var terminado by remember { mutableStateOf(false) }
    var erro by remember { mutableStateOf<String?>(null) }
    var iniciado by remember { mutableStateOf(false) }

    // Executa a IA uma vez e "revela" os campos na ordem conforme o preenchimento
    LaunchedEffect(itemId, fotos.size) {
        if (iniciado || fotos.isEmpty()) return@LaunchedEffect
        iniciado = true
        val fotosBytes = fotos.sortedBy { it.ordem }.mapNotNull { FotoUtil.lerBytes(contexto, it.uri) }
        if (fotosBytes.isEmpty()) {
            erro = "Não consegui ler as fotos."
            return@LaunchedEffect
        }
        val servico = FabricaIA.criar()
        if (servico == null) {
            erro = "Sem chave de IA neste build — toque em Ver anúncio e preencha manualmente."
            terminado = true
            return@LaunchedEffect
        }
        val resultado = servico.gerarAnuncioMulti(fotosBytes)
        resultado
            .onSuccess { s ->
                // salva no item — a revisão abre já preenchida (não chama a IA de novo)
                item?.let { atual ->
                    app.repositorio.salvarItem(
                        atual.copy(
                            titulo = s.titulo,
                            descricao = s.descricao,
                            categoria = s.categoria_sugerida,
                            precoSugerido = s.melhorPreco,
                            precoComparativoMercado = s.precoComparativoMercado
                        )
                    )
                }
                val valores = listOf(
                    "Categoria" to s.categoria_sugerida,
                    "Título" to s.titulo,
                    "Descrição" to s.descricao,
                    "Preço" to (if (s.precoSugeridoReais > 0) "R$ ${"%.2f".format(s.precoSugeridoReais)}" else "—"),
                    "Condição" to s.condicao
                )
                // revela um card por vez (efeito de preenchimento sequencial)
                for ((i, par) in valores.withIndex()) {
                    indiceAtivo = i
                    delay(650)
                    preenchidos = preenchidos + (par.first to par.second)
                }
                indiceAtivo = campos.size
                terminado = true
            }
            .onFailure { e ->
                erro = "IA: ${e.message}"
                terminado = true
            }
    }

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(
            Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = 24.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Analisando fotos", style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold)
            Spacer(Modifier.height(4.dp))
            Text(
                if (!terminado) "A IA está escrevendo seu anúncio"
                else "Pronto — toque em Ver anúncio",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(36.dp))

            // ── Pilha de cartões (leque) ──
            Box(Modifier.fillMaxWidth().height(300.dp), contentAlignment = Alignment.TopCenter) {
                campos.forEachIndexed { i, campo ->
                    val preenchido = preenchidos.containsKey(campo.nome)
                    val ativo = i == indiceAtivo && !terminado
                    val escala by animateFloatAsState(
                        targetValue = if (ativo) 1.06f else 0.97f,
                        animationSpec = tween(350), label = "escala"
                    )
                    CardAnalise(
                        campo = campo.nome,
                        icone = campo.icone,
                        valor = preenchidos[campo.nome],
                        ativo = ativo,
                        preenchido = preenchido,
                        modifier = Modifier
                            .width(250.dp)
                            .offset(y = (i * 48).dp)
                            .rotate((i - 2) * 3f)
                            .scale(escala)
                            .zIndex(if (ativo) 10f else i.toFloat())
                    )
                }
            }

            erro?.let {
                Spacer(Modifier.height(12.dp))
                Text(it, color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            }

            Spacer(Modifier.height(24.dp))
            if (terminado) {
                Button(
                    onClick = { onVerAnuncio(itemId) },
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Destaque, contentColor = Color(0xFF06231B)
                    )
                ) {
                    Text("Ver anúncio", style = MaterialTheme.typography.titleMedium)
                }
            } else {
                CircularProgressIndicator(Modifier.size(28.dp), color = Destaque, strokeWidth = 3.dp)
            }
            Spacer(Modifier.height(12.dp))
            TextButton(onClick = onVoltar) { Text("Cancelar", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
    }
}

@Composable
private fun CardAnalise(
    campo: String,
    icone: ImageVector,
    valor: String?,
    ativo: Boolean,
    preenchido: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (ativo) MaterialTheme.colorScheme.surfaceContainerHigh
            else MaterialTheme.colorScheme.surfaceContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (ativo) 8.dp else 2.dp)
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icone, contentDescription = null,
                tint = if (preenchido) Destaque else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(campo, style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    valor ?: (if (ativo) "escrevendo…" else "aguardando"),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                    color = if (preenchido) MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (preenchido) {
                Icon(Icons.Default.Check, contentDescription = null, tint = Destaque,
                    modifier = Modifier.size(20.dp))
            }
        }
    }
}
