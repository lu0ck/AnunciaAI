package br.com.anunciaai.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.statusBarsPadding
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.zIndex
import br.com.anunciaai.AnunciaAIApp
import br.com.anunciaai.ia.FabricaIA
import br.com.anunciaai.ia.gerarComContrato
import br.com.anunciaai.ui.foto.FotoUtil
import br.com.anunciaai.ui.theme.CorFundo
import br.com.anunciaai.ui.theme.CorTexto
import br.com.anunciaai.ui.theme.CorTextoSec
import br.com.anunciaai.ui.theme.Destaque
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * TELA 2 (continuação) — ANÁLISE DA IA (v12.0, reescrita do zero).
 * Pilha de cards EMPILHADOS (spec: "cards empilhados, card atual em destaque
 * na frente da pilha, check verde ao concluir"): leque com offset vertical
 * fixo por índice ((i * 26).dp — nunca fórmula com índice ativo, que oscila),
 * card ativo ganha zIndex + scale + elevação, revelação sequencial com check
 * verde animado. Tema escuro integral (#12151A).
 * Lógica (mantida da v11.1 por ser correta): resultado persiste no Room
 * ANTES de navegar; gerarComContrato re-tenta condição fora das 4 exatas.
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
    var preenchidos by rememberSaveable { mutableStateOf(mapOf<String, String>()) }
    var terminado by rememberSaveable { mutableStateOf(false) }
    var erro by remember { mutableStateOf<String?>(null) }
    var iniciado by rememberSaveable { mutableStateOf(false) }
    // FASE 1: sinaliza falha REAL (sem resultado utilizável) — "Ver anúncio" some
    var falhou by rememberSaveable { mutableStateOf(false) }
    // FASE 1: condição veio vazia/estranha e foi estimada — aviso discreto
    var condicaoEstimada by rememberSaveable { mutableStateOf(false) }

    // índice do card "em foco" = último preenchido (fica na frente da pilha)
    val foco = if (preenchidos.isEmpty()) 0 else campos.indexOfLast { preenchidos[it.nome] != null } + 1

    // FASE 1: análise isolada em função — roda no 1º load E no "Tentar de novo"
    val escopo = rememberCoroutineScope()
    suspend fun rodarAnalise() {
        val fotosBytes = fotos.sortedBy { it.ordem }.mapNotNull { FotoUtil.lerBytes(contexto, it.uri) }
        if (fotosBytes.isEmpty()) {
            erro = "Não consegui ler as fotos."
            falhou = true
            return
        }
        val servico = FabricaIA.criar()
        if (servico == null) {
            erro = "Sem chave de IA neste build — preencha manualmente."
            falhou = true
            return
        }
        // FASE 1 (v6): retry só p/ JSON quebrado/campos essenciais faltando;
        // condição vazia vira "bom estado" + aviso, sem descartar a resposta
        val resultado = servico.gerarComContrato(
            fotos = fotosBytes,
            ean = ean.takeIf { !it.isNullOrBlank() }
        )
        resultado
            .onSuccess { s ->
                // persiste ANTES de navegar (fix v11.1: leitura direta suspend)
                val atual = item ?: app.repositorio.itemNow(itemId)
                if (atual != null) {
                    app.repositorio.salvarItem(
                        atual.copy(
                            titulo = s.titulo,
                            descricao = s.descricao,
                            categoria = s.categoria_sugerida,
                            precoSugerido = s.melhorPreco,
                            precoComparativoMercado = s.precoComparativoMercado,
                            condicao = s.condicaoNormalizada,
                            condicaoEstimada = s.condicaoEstimada,
                            ean = ean
                        )
                    )
                }
                condicaoEstimada = s.condicaoEstimada
                val valores = listOf(
                    "Categoria" to s.categoria_sugerida,
                    "Título" to s.titulo,
                    "Descrição" to s.descricao,
                    "Preço" to (if (s.melhorPreco > 0) "R$ ${"%.2f".format(s.melhorPreco)}" else "—"),
                    "Condição" to s.condicaoNormalizada
                )
                for (par in valores) {
                    preenchidos = preenchidos + par
                    delay(650)
                }
                terminado = true
            }
            .onFailure { e ->
                erro = "IA: ${e.message}"
                // FASE 1: falha REAL = sem "Ver anúncio" p/ revisão vazia
                falhou = true
            }
    }

    LaunchedEffect(itemId, fotos.size) {
        if (iniciado || fotos.isEmpty()) return@LaunchedEffect
        iniciado = true
        rodarAnalise()
    }

    Box(Modifier.fillMaxSize().background(CorFundo)) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Analisando fotos",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.ExtraBold,
                color = CorTexto
            )
            Spacer(Modifier.height(4.dp))
            Text(
                if (!terminado) "A IA está escrevendo seu anúncio" else "Pronto",
                style = MaterialTheme.typography.bodyMedium,
                color = CorTextoSec
            )
            // FASE 1: aviso discreto quando a condição foi estimada
            if (terminado && condicaoEstimada) {
                Spacer(Modifier.height(6.dp))
                Text(
                    "Condição estimada — confira na revisão",
                    style = MaterialTheme.typography.bodySmall,
                    color = CorTextoSec
                )
            }
            Spacer(Modifier.height(28.dp))

            // ── PILHA DE CARDS EMPILHADOS (leque) ──
            // offsets FIXOS por índice; card em foco: zIndex + scale + elevação.
            Box(
                Modifier.fillMaxWidth(),
                contentAlignment = Alignment.TopCenter
            ) {
                campos.forEachIndexed { i, campo ->
                    val valor = preenchidos[campo.nome]
                    val emFoco = i == foco.coerceAtMost(campos.size - 1)
                    Box(
                        Modifier
                            .zIndex(if (emFoco) campos.size.toFloat() else i.toFloat())
                            .padding(top = (i * 26).dp)
                    ) {
                        CardAnalise(
                            campo = campo.nome,
                            icone = campo.icone,
                            valor = valor,
                            preenchido = valor != null,
                            emFoco = emFoco,
                            modifier = Modifier
                                .fillMaxWidth()
                                .scale(
                                    animateFloatAsState(
                                        targetValue = if (emFoco) 1f else 0.96f,
                                        animationSpec = spring(
                                            dampingRatio = Spring.DampingRatioMediumBouncy,
                                            stiffness = Spring.StiffnessMedium
                                        ),
                                        label = "sc_$i"
                                    ).value
                                )
                        )
                    }
                }
            }

            erro?.let {
                Spacer(Modifier.height(16.dp))
                Text(
                    it, color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center
                )
            }

            Spacer(Modifier.height(28.dp))
            when {
                // FASE 1: falha real → botões de recuperação, sem "Ver anúncio"
                falhou -> {
                    Button(
                        onClick = {
                            // reseta o estado e roda a análise de novo
                            iniciado = false; falhou = false; erro = null
                            preenchidos = emptyMap(); terminado = false
                            escopo.launch { rodarAnalise() }
                        },
                        modifier = Modifier.fillMaxWidth().height(54.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Destaque, contentColor = Color(0xFF04150F)
                        )
                    ) {
                        Text("Tentar de novo", style = MaterialTheme.typography.titleMedium)
                    }
                    Spacer(Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = { onVerAnuncio(itemId) },
                        modifier = Modifier.fillMaxWidth().height(54.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("Preencher manualmente", style = MaterialTheme.typography.titleMedium,
                            color = CorTexto)
                    }
                }
                terminado -> {
                    Button(
                        onClick = { onVerAnuncio(itemId) },
                        modifier = Modifier.fillMaxWidth().height(54.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Destaque, contentColor = Color(0xFF04150F)
                        )
                    ) {
                        Text("Ver anúncio", style = MaterialTheme.typography.titleMedium)
                    }
                }
                else -> {
                    CircularProgressIndicator(Modifier.size(28.dp), color = Destaque, strokeWidth = 3.dp)
                }
            }
            Spacer(Modifier.height(12.dp))
            TextButton(onClick = onVoltar) { Text("Cancelar", color = CorTextoSec) }
            Spacer(Modifier.height(40.dp))
        }
    }
}

/** Card da pilha: campo + valor; check verde ANIMADO quando concluído. */
@Composable
private fun CardAnalise(
    campo: String,
    icone: ImageVector,
    valor: String?,
    preenchido: Boolean,
    emFoco: Boolean,
    modifier: Modifier = Modifier
) {
    val escalaCheck by animateFloatAsState(
        targetValue = if (preenchido) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "check_$campo"
    )
    // revelação: card surge de baixo + fade quando é o foco
    AnimatedVisibility(
        visible = emFoco || preenchido,
        enter = slideInVertically(
            initialOffsetY = { it / 2 },
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
        ) + fadeIn()
    ) {
        Card(
            modifier = modifier,
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = if (emFoco) 6.dp else 2.dp)
        ) {
            androidx.compose.foundation.layout.Row(
                Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    icone, contentDescription = null,
                    tint = if (preenchido) Destaque else CorTextoSec,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        campo, style = MaterialTheme.typography.labelMedium,
                        color = CorTextoSec
                    )
                    Text(
                        valor ?: (if (preenchido) "" else "aguardando a IA…"),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2, overflow = TextOverflow.Ellipsis,
                        color = if (preenchido) CorTexto else CorTextoSec
                    )
                }
                Box(Modifier.size(24.dp)) {
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
}
