package br.com.anunciaai.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.statusBarsPadding
import br.com.anunciaai.AnunciaAIApp
import br.com.anunciaai.ia.FabricaIA
import br.com.anunciaai.ui.foto.FotoUtil
import br.com.anunciaai.ui.theme.Destaque
import kotlinx.coroutines.delay

/**
 * Análise da IA (PILAR 3 — v8.0): FIM DAS SOBREPOSIÇÕES.
 * Os 5 cartões ficam numa Column com verticalArrangement = spacedBy(16.dp).
 * Cada campo preenchido SURGE com spring + check verde animado (scale).
 * Categoria vem como TAXONOMIA EXATA do prompt ("Eletrônicos > ... > Mouses").
 */
@Composable
fun AnaliseScreen(
    itemId: Long,
    ean: String? = null,
    onVerAnuncio: (Long) -> Unit,
    onVoltar: () -> Unit
) {
    val contexto = LocalContext.current
    val app = contexto.applicationContext as AnunciaAIApp

    val item by app.repositorio.item(itemId).collectAsState(initial = null)
    val fotos by app.repositorio.fotosDoItem(itemId).collectAsState(initial = emptyList())

    data class Campo(val nome: String, val icone: ImageVector)
    val campos = listOf(
        Campo("Categoria", Icons.Default.Category),
        Campo("Título", Icons.Default.Title),
        Campo("Descrição", Icons.Outlined.Description),
        Campo("Preço", Icons.Default.Payments),
        Campo("Condição", Icons.Default.LocalOffer)
    )
    var preenchidos by remember { mutableStateOf(mapOf<String, String>()) }
    var terminado by remember { mutableStateOf(false) }
    var erro by remember { mutableStateOf<String?>(null) }
    var iniciado by remember { mutableStateOf(false) }

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
        // PILAR 4: com EAN lido, a IA recebe o código exato do produto
        val resultado = if (!ean.isNullOrBlank())
            servico.gerarAnuncioComEan(fotosBytes, ean)
        else
            servico.gerarAnuncioMulti(fotosBytes)
        resultado
            .onSuccess { s ->
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
                    "Preço" to (if (s.melhorPreco > 0) "R$ ${"%.2f".format(s.melhorPreco)}" else "—"),
                    "Condição" to s.condicaoNormalizada
                )
                for ((i, par) in valores.withIndex()) {
                    preenchidos = preenchidos + (par.first to par.second)
                    delay(500)
                }
                terminado = true
            }
            .onFailure { e ->
                erro = "IA: ${e.message}"
                terminado = true
            }
    }

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 24.dp),
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
            Spacer(Modifier.height(24.dp))

            // ── PILAR 3: Column espaçada — SEM sobreposição ──
            Column(
                Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                campos.forEach { campo ->
                    val valor = preenchidos[campo.nome]
                    // cada card SURGE com spring (slide de baixo + fade)
                    AnimatedVisibility(
                        visible = true,
                        enter = slideInVertically(
                            initialOffsetY = { it / 2 },
                            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
                        ) + fadeIn()
                    ) {
                        CardAnalise(
                            campo = campo.nome,
                            icone = campo.icone,
                            valor = valor,
                            preenchido = valor != null,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            erro?.let {
                Spacer(Modifier.height(12.dp))
                Text(it, color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
            }

            Spacer(Modifier.height(24.dp))
            if (terminado) {
                Button(
                    onClick = { onVerAnuncio(itemId) },
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Destaque, contentColor = Color(0xFF12092B)
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
    preenchido: Boolean,
    modifier: Modifier = Modifier
) {
    // check com spring quando o campo é preenchido
    val escalaCheck by animateFloatAsState(
        targetValue = if (preenchido) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "check_$campo"
    )
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icone, contentDescription = null,
                tint = if (preenchido) Destaque else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(campo, style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    valor ?: (if (preenchido) "" else "aguardando a IA…"),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2, overflow = TextOverflow.Ellipsis,
                    color = if (preenchido) MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            // check verde ANIMADO (surge com spring)
            androidx.compose.foundation.layout.Box(Modifier.size(24.dp)) {
                Icon(
                    Icons.Default.Check, contentDescription = null, tint = Destaque,
                    modifier = Modifier
                        .scale(escalaCheck)
                        .align(Alignment.Center)
                )
            }
        }
    }
}
