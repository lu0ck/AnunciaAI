package br.com.anunciaai.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import br.com.anunciaai.AnunciaAIApp
import br.com.anunciaai.dados.FotoItem
import br.com.anunciaai.ia.FabricaIA
import br.com.anunciaai.ia.gerarComContrato
import br.com.anunciaai.ia.modelo.SugestaoIA
import br.com.anunciaai.plataformas.webview.LoginWebViewActivity
import br.com.anunciaai.publica.OrquestradorDePublicacao
import br.com.anunciaai.publica.webview.SessaoPublicacaoWeb
import br.com.anunciaai.ui.Plataforma
import br.com.anunciaai.ui.foto.CarrosselFotos
import br.com.anunciaai.ui.foto.FotoUtil
import br.com.anunciaai.ui.theme.CorFundo
import br.com.anunciaai.ui.theme.CorPlataforma
import br.com.anunciaai.ui.theme.CorTexto
import br.com.anunciaai.ui.theme.CorTextoSec
import br.com.anunciaai.ui.theme.Destaque
import kotlinx.coroutines.launch

/**
 * TELA 3 — REVISAR ANÚNCIO (v12.0, reescrita do zero).
 * - Campos fundo #1B1F26, sem borda visível, tom só muda no foco
 * - Estado inicial dos campos E DO CHIP DE CONDIÇÃO vem direto do item
 *   persistido (que carrega o JSON da IA): `remember(item?.id)` — se a IA
 *   disse "novo", o chip "novo" já vem marcado (testado em ContratoCondicaoTest)
 * - Chips de condição: 4 fixos, FlowRow (largura por conteúdo, quebra por palavra)
 * - Categoria+preço em Row com weight(1f), categoria maxLines=1 + Ellipsis
 * - Chips "Onde publicar": COR DA MARCA (preenchido seleção / contorno da marca)
 * - Carrossel com miniaturas alinhadas + indicador em DOTS
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun RevisaoScreen(
    itemId: Long,
    onVoltar: () -> Unit,
    onPublicado: () -> Unit
) {
    val contexto = LocalContext.current
    val app = contexto.applicationContext as AnunciaAIApp
    val escopo = rememberCoroutineScope()

    val item by app.repositorio.item(itemId).collectAsState(initial = null)
    val fotos by app.repositorio.fotosDoItem(itemId).collectAsState(initial = emptyList())
    val contas by app.repositorio.contas().collectAsState(initial = emptyList())

    // ── ESTADO INICIAL DIRETO DO RESULTADO DA IA (persistido no item) ──
    var titulo by remember(item?.id) { mutableStateOf(TextFieldValue(item?.titulo ?: "")) }
    var descricao by remember(item?.id) { mutableStateOf(TextFieldValue(item?.descricao ?: "")) }
    var categoria by remember(item?.id) { mutableStateOf(TextFieldValue(item?.categoria ?: "")) }
    var preco by remember(item?.id) {
        mutableStateOf(
            TextFieldValue(
                item?.let { it.precoFinal.takeIf { p -> p > 0 } ?: it.precoSugerido }
                    ?.let { v -> "%.2f".format(v) } ?: ""
            )
        )
    }
    // chip de condição: valor EXATO que a IA devolveu (não default)
    var condicao by remember(item?.id) {
        mutableStateOf(
            item?.condicao?.takeIf { it.isNotBlank() }?.let { SugestaoIA.normalizarCondicao(it) }
                ?: "bom estado"
        )
    }
    var marcadas by remember { mutableStateOf(setOf(Plataforma.MERCADO_LIVRE, Plataforma.OLX)) }
    var gerando by remember { mutableStateOf(false) }
    var publicando by remember { mutableStateOf(false) }
    var erro by remember { mutableStateOf<String?>(null) }

    fun preencher(s: SugestaoIA) {
        titulo = TextFieldValue(s.titulo)
        descricao = TextFieldValue(s.descricao)
        categoria = TextFieldValue(s.categoria_sugerida)
        preco = TextFieldValue(if (s.melhorPreco > 0) "%.2f".format(s.melhorPreco) else "")
        condicao = s.condicaoNormalizada
    }

    fun gerarComIA() {
        val it = item ?: return
        if (fotos.isEmpty()) {
            erro = "Adicione pelo menos uma foto."
            return
        }
        val servico = FabricaIA.criar()
        if (servico == null) {
            erro = "Nenhuma chave de IA configurada (ANUNCIAAI_NVIDIA_KEY no local.properties). Preencha os campos manualmente."
            return
        }
        gerando = true
        erro = null
        escopo.launch {
            try {
                val bytes = fotos.sortedBy { f -> f.ordem }.mapNotNull { f -> FotoUtil.lerBytes(contexto, f.uri) }
                if (bytes.isEmpty()) {
                    erro = "Não consegui ler as fotos."
                } else {
                    // v11.1: re-tenta quando a condição vem fora das 4 exatas
                    servico.gerarComContrato(bytes)
                        .onSuccess { s ->
                            preencher(s)
                            // persiste imediatamente (chip certo mesmo se publicar depois)
                            app.repositorio.itemNow(itemId)?.let { atual ->
                                app.repositorio.salvarItem(
                                    atual.copy(
                                        condicao = s.condicaoNormalizada,
                                        precoComparativoMercado = s.precoComparativoMercado
                                    )
                                )
                            }
                        }
                        .onFailure { e -> erro = "IA: ${e.message}" }
                }
            } catch (e: Exception) {
                erro = "IA: ${e.message}"
            }
            gerando = false
        }
    }

    fun publicar() {
        val it = item ?: return
        if (marcadas.isEmpty()) {
            erro = "Marque pelo menos uma plataforma."
            return
        }
        publicando = true
        erro = null
        escopo.launch {
            try {
                val precoNum = preco.text.replace(",", ".").toDoubleOrNull() ?: 0.0
                app.repositorio.salvarItem(
                    it.copy(
                        titulo = titulo.text.trim(),
                        descricao = descricao.text.trim(),
                        categoria = categoria.text.trim(),
                        precoSugerido = if (it.precoSugerido <= 0) precoNum else it.precoSugerido,
                        precoFinal = precoNum,
                        condicao = condicao
                    )
                )

                val viaApi = marcadas.filter { p -> p == Plataforma.MERCADO_LIVRE || p == Plataforma.EBAY }
                val viaWeb = marcadas - viaApi.toSet()

                if (viaApi.isNotEmpty()) {
                    OrquestradorDePublicacao(app).publicar(itemId, viaApi)
                }
                if (viaWeb.isNotEmpty()) {
                    SessaoPublicacaoWeb.enfileirar(app, itemId, viaWeb.toList(), it)
                    SessaoPublicacaoWeb.proximo()
                    contexto.startActivity(
                        android.content.Intent(contexto, LoginWebViewActivity::class.java)
                    )
                }
                onPublicado()
            } catch (e: Exception) {
                erro = "Erro ao publicar: ${e.message}"
            }
            publicando = false
        }
    }

    val conectadas = contas.map { it.plataforma }.toSet()

    val galeriaMais = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(maxItems = 10)
    ) { uris ->
        if (uris.isNotEmpty()) {
            escopo.launch {
                var adicionadas = 0
                for (uri in uris) {
                    val copiada = FotoUtil.copiarParaInterno(contexto, uri)
                    if (copiada != null && app.repositorio.adicionarFoto(itemId, copiada)) adicionadas++
                }
                if (adicionadas == 0) erro = "Limite de 10 fotos por item."
            }
        }
    }

    Scaffold(
        containerColor = CorFundo,
        topBar = {
            TopAppBar(
                title = { Text("Revisar anúncio", style = MaterialTheme.typography.headlineSmall, color = CorTexto) },
                navigationIcon = {
                    IconButton(onClick = onVoltar) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar", tint = CorTexto)
                    }
                }
            )
        }
    ) { pad ->
        Column(
            Modifier.padding(pad).fillMaxSize().verticalScroll(rememberScrollState())
        ) {
            Column(Modifier.padding(horizontal = 16.dp)) {
                CarrosselFotos(
                    fotos = fotos,
                    onAdicionar = {
                        galeriaMais.launch(
                            PickVisualMediaRequest(
                                ActivityResultContracts.PickVisualMedia.ImageOnly
                            )
                        )
                    },
                    onRemover = { foto -> escopo.launch { app.repositorio.removerFoto(foto.id) } },
                    onMover = { de, para ->
                        val lista = fotos.toMutableList()
                        val f = lista.removeAt(de)
                        lista.add(para, f)
                        escopo.launch { app.repositorio.reordenarFotos(lista) }
                    }
                )
            }

            if (gerando) {
                Row(
                    Modifier.padding(horizontal = 16.dp, vertical = 8.dp).fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp,
                        color = Destaque)
                    Spacer(Modifier.width(10.dp))
                    Text("Gerando anúncio...", style = MaterialTheme.typography.bodyMedium,
                        color = CorTextoSec)
                }
            }

            Column(Modifier.padding(horizontal = 16.dp)) {
                val coresCampo = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer
                )
                OutlinedTextField(
                    value = titulo, onValueChange = { titulo = it },
                    label = { Text("Título (até 60 caracteres)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.small,
                    colors = coresCampo
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = descricao, onValueChange = { descricao = it },
                    label = { Text("Descrição") },
                    modifier = Modifier.fillMaxWidth(), minLines = 3,
                    shape = MaterialTheme.shapes.small,
                    colors = coresCampo
                )
                Spacer(Modifier.height(10.dp))
                // ── categoria + preço na MESMA linha, mesma altura ──
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = preco, onValueChange = { preco = it },
                        label = { Text("Preço R$") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                        shape = MaterialTheme.shapes.small,
                        colors = coresCampo
                    )
                    OutlinedTextField(
                        value = categoria, onValueChange = { categoria = it },
                        label = { Text("Categoria") },
                        modifier = Modifier.weight(1f),
                        shape = MaterialTheme.shapes.small,
                        colors = coresCampo
                    )
                }
                Spacer(Modifier.height(14.dp))
                // ── chips de condição: 4 fixos, FlowRow, quebra por palavra ──
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    SugestaoIA.CONDICOES.forEach { valor ->
                        val selecionado = condicao == valor
                        Box(
                            Modifier
                                .clip(RoundedCornerShape(50))
                                .background(
                                    if (selecionado) Destaque
                                    else Color.Transparent
                                )
                                .border(
                                    width = 1.dp,
                                    color = if (selecionado) Destaque
                                    else MaterialTheme.colorScheme.outline,
                                    shape = RoundedCornerShape(50)
                                )
                                .clickable { condicao = valor }
                                .padding(horizontal = 18.dp, vertical = 10.dp)
                        ) {
                            Text(
                                valor,
                                style = MaterialTheme.typography.labelLarge,
                                color = if (selecionado) Color(0xFF04150F)
                                else CorTextoSec
                            )
                        }
                    }
                }
                // preço comparativo do mercado (quando a IA devolve)
                val comparativo = item?.precoComparativoMercado ?: 0.0
                if (comparativo > 0) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Preço médio no mercado: R$ ${"%.2f".format(comparativo)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = CorTextoSec
                    )
                }
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = { gerarComIA() },
                    enabled = !gerando && fotos.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Destaque,
                        contentColor = Color(0xFF04150F)
                    )
                ) {
                    if (gerando) {
                        CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp,
                            color = Color(0xFF04150F))
                        Spacer(Modifier.width(10.dp))
                        Text("Gerando anúncio...", style = MaterialTheme.typography.titleMedium)
                    } else {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Gerar com a IA", style = MaterialTheme.typography.titleMedium)
                    }
                }

                Spacer(Modifier.height(14.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(Modifier.height(14.dp))
                Text("Onde publicar", style = MaterialTheme.typography.titleMedium, color = CorTexto)

                Spacer(Modifier.height(10.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Plataforma.entries.forEach { plat ->
                        val marcada = marcadas.contains(plat)
                        val corMarca = CorPlataforma[plat.name] ?: Destaque
                        val corContorno = if (plat.name == "EBAY") Color.White else corMarca
                        Box(
                            Modifier
                                .clip(RoundedCornerShape(50))
                                .background(if (marcada) corMarca else Color.Transparent)
                                .border(
                                    width = 1.dp,
                                    color = corContorno,
                                    shape = RoundedCornerShape(50)
                                )
                                .clickable {
                                    marcadas = if (marcada) marcadas - plat else marcadas + plat
                                }
                                .padding(horizontal = 16.dp, vertical = 10.dp)
                        ) {
                            Text(
                                plat.rotulo,
                                style = MaterialTheme.typography.labelLarge,
                                color = if (marcada) {
                                    if (plat.name == "MERCADO_LIVRE") br.com.anunciaai.ui.theme.CorTextoMarca["MERCADO_LIVRE"]!! else Color.White
                                } else CorTexto
                            )
                        }
                    }
                }
                val naoConectadas = Plataforma.entries
                    .filter { marcadas.contains(it) && (if (it.precisaOAuth) !conectadas.contains(it.name) else false) }
                if (naoConectadas.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.WarningAmber, contentDescription = null,
                            tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "${naoConectadas.joinToString { it.rotulo }} sem conexão — conecte em Configurações ou o anúncio vai falhar.",
                            style = MaterialTheme.typography.bodySmall,
                            color = CorTextoSec
                        )
                    }
                }

                erro?.let {
                    Spacer(Modifier.height(10.dp))
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }

                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = { publicar() },
                    enabled = !publicando && !gerando,
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    shape = MaterialTheme.shapes.medium,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Destaque,
                        contentColor = Color(0xFF04150F)
                    )
                ) {
                    Text(if (publicando) "Publicando..." else "Publicar",
                        style = MaterialTheme.typography.titleMedium)
                }
                Spacer(Modifier.height(28.dp))
            }
        }
    }
}
