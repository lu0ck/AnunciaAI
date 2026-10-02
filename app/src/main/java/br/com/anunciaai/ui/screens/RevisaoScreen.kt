package br.com.anunciaai.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.background
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import br.com.anunciaai.AnunciaAIApp
import br.com.anunciaai.dados.FotoItem
import br.com.anunciaai.oauth.ChavesIA
import br.com.anunciaai.ia.FabricaIA
import br.com.anunciaai.ia.modelo.SugestaoIA
import br.com.anunciaai.plataformas.webview.LoginWebViewActivity
import br.com.anunciaai.publica.OrquestradorDePublicacao
import br.com.anunciaai.publica.webview.SessaoPublicacaoWeb
import br.com.anunciaai.ui.BarraDockComBadge
import br.com.anunciaai.ui.Plataforma
import br.com.anunciaai.ui.Rotas
import br.com.anunciaai.ui.foto.CarrosselFotos
import br.com.anunciaai.ui.foto.FotoUtil
import br.com.anunciaai.ui.theme.CorPlataforma
import br.com.anunciaai.ui.theme.Destaque
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun RevisaoScreen(
    itemId: Long,
    onVoltar: () -> Unit,
    onPublicado: () -> Unit,
    onNavBottom: (String) -> Unit = {}
) {
    val contexto = LocalContext.current
    val app = contexto.applicationContext as AnunciaAIApp
    val escopo = rememberCoroutineScope()

    val item by app.repositorio.item(itemId).collectAsState(initial = null)
    val fotos by app.repositorio.fotosDoItem(itemId).collectAsState(initial = emptyList())
    val contas by app.repositorio.contas().collectAsState(initial = emptyList())

    var titulo by remember { mutableStateOf(TextFieldValue("")) }
    var descricao by remember { mutableStateOf(TextFieldValue("")) }
    var categoria by remember { mutableStateOf(TextFieldValue("")) }
    var preco by remember { mutableStateOf(TextFieldValue("")) }
    var condicao by remember { mutableStateOf("bom estado") }
    var marcadas by remember { mutableStateOf(setOf(Plataforma.MERCADO_LIVRE, Plataforma.OLX)) }
    var gerando by remember { mutableStateOf(false) }
    var publicando by remember { mutableStateOf(false) }
    var erro by remember { mutableStateOf<String?>(null) }
    var preenchidoOnce by remember { mutableStateOf(false) }

    fun preencher(s: SugestaoIA) {
        titulo = TextFieldValue(s.titulo)
        descricao = TextFieldValue(s.descricao)
        categoria = TextFieldValue(s.categoria_sugerida)
        preco = TextFieldValue(if (s.melhorPreco > 0) "%.2f".format(s.melhorPreco) else "")
        // v7: a IA devolve um dos 4 valores exatos — o chip certo já vem marcado
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
                // TODAS as fotos (em ordem) na mesma chamada
                val bytes = fotos.sortedBy { f -> f.ordem }.mapNotNull { f -> FotoUtil.lerBytes(contexto, f.uri) }
                if (bytes.isEmpty()) {
                    erro = "Não consegui ler as fotos."
                } else {
                    servico.gerarAnuncioMulti(bytes)
                        .onSuccess { s -> preencher(s) }
                        .onFailure { e -> erro = "IA: ${e.message}" }
                }
            } catch (e: Exception) {
                erro = "IA: ${e.message}"
            }
            gerando = false
        }
    }

    LaunchedEffect(item?.id, fotos.size) {
        val it = item
        if (it != null && !preenchidoOnce && fotos.isNotEmpty()) {
            preenchidoOnce = true
            if (it.titulo.isBlank()) gerarComIA()
            else preencher(
                SugestaoIA(
                    titulo = it.titulo, descricao = it.descricao,
                    categoria_sugerida = it.categoria,
                    precoSugeridoReais = it.precoFinal.takeIf { p -> p > 0 } ?: it.precoSugerido
                )
            )
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
                        precoFinal = precoNum
                    )
                )

                val viaApi = marcadas.filter { it == Plataforma.MERCADO_LIVRE || it == Plataforma.EBAY }
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

    // adiciona mais fotos via galeria (carrossel)
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
        topBar = {
            TopAppBar(
                title = { Text("Revisar anúncio", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = onVoltar) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                }
            )
        },
        bottomBar = { BarraDockComBadge(Rotas.CAPTURA, onNavBottom) }
    ) { pad ->
        Column(
            Modifier.padding(pad).fillMaxSize().verticalScroll(rememberScrollState())
        ) {
            // ---- momento principal: carrossel de fotos ----
            Column(Modifier.padding(horizontal = 16.dp)) {
                CarrosselFotos(
                    fotos = fotos,
                    onAdicionar = {
                        galeriaMais.launch(
                            androidx.activity.result.PickVisualMediaRequest(
                                androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageOnly
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
                        color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(10.dp))
                    Text("Gerando anúncio...", style = MaterialTheme.typography.bodyMedium)
                }
            }

            Column(Modifier.padding(horizontal = 16.dp)) {
                // v4.1: campos no padrão do app — fundo elevado #1B1F26, sem borda visível
                OutlinedTextField(
                    value = titulo, onValueChange = { titulo = it },
                    label = { Text("Título (até 60 caracteres)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.small,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = androidx.compose.ui.graphics.Color.Transparent,
                        unfocusedBorderColor = androidx.compose.ui.graphics.Color.Transparent,
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer
                    )
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = descricao, onValueChange = { descricao = it },
                    label = { Text("Descrição") },
                    modifier = Modifier.fillMaxWidth(), minLines = 3,
                    shape = MaterialTheme.shapes.small,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = androidx.compose.ui.graphics.Color.Transparent,
                        unfocusedBorderColor = androidx.compose.ui.graphics.Color.Transparent,
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer
                    )
                )
                Spacer(Modifier.height(10.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = preco, onValueChange = { preco = it },
                        label = { Text("Preço R$") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                        shape = MaterialTheme.shapes.small,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = androidx.compose.ui.graphics.Color.Transparent,
                            unfocusedBorderColor = androidx.compose.ui.graphics.Color.Transparent,
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer
                        )
                    )
                    OutlinedTextField(
                        value = categoria, onValueChange = { categoria = it },
                        label = { Text("Categoria") },
                        modifier = Modifier.weight(1f),
                        shape = MaterialTheme.shapes.small,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = androidx.compose.ui.graphics.Color.Transparent,
                            unfocusedBorderColor = androidx.compose.ui.graphics.Color.Transparent,
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer
                        )
                    )
                }
                Spacer(Modifier.height(12.dp))
                // v11: chips PÍLULA (Figma bike) — ativo = lime neon + texto escuro;
                // inativo = border fino cinza + texto cinza. Nada de quadrado.
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    SugestaoIA.CONDICOES.forEach { valor ->
                        val selecionado = condicao == valor
                        Box(
                            Modifier
                                .clip(androidx.compose.foundation.shape.RoundedCornerShape(50))
                                .background(
                                    if (selecionado) br.com.anunciaai.ui.theme.VerdeNeon
                                    else androidx.compose.ui.graphics.Color.Transparent
                                )
                                .border(
                                    width = 1.dp,
                                    color = if (selecionado) br.com.anunciaai.ui.theme.VerdeNeon
                                    else MaterialTheme.colorScheme.outline,
                                    shape = androidx.compose.foundation.shape.RoundedCornerShape(50)
                                )
                                .clickable { condicao = valor }
                                .padding(horizontal = 18.dp, vertical = 10.dp)
                        ) {
                            Text(
                                valor,
                                style = MaterialTheme.typography.labelLarge,
                                color = if (selecionado) androidx.compose.ui.graphics.Color(0xFF101601)
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                // v7: preço comparativo do mercado (quando a IA devolve)
                val comparativo = item?.precoComparativoMercado ?: 0.0
                if (comparativo > 0) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Preço médio no mercado: R$ ${"%.2f".format(comparativo)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                // Ação principal: sólida, cor de destaque única
                Button(
                    onClick = { gerarComIA() },
                    enabled = !gerando && fotos.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Destaque,
                        contentColor = androidx.compose.ui.graphics.Color(0xFF12092B)
                    )
                ) {
                    if (gerando) {
                        CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp,
                            color = androidx.compose.ui.graphics.Color(0xFF12092B))
                        Spacer(Modifier.width(10.dp))
                        Text("Gerando anúncio...", style = MaterialTheme.typography.titleMedium)
                    } else {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Gerar com a IA", style = MaterialTheme.typography.titleMedium)
                    }
                }

                Spacer(Modifier.height(12.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(Modifier.height(12.dp))
                Text("Onde publicar", style = MaterialTheme.typography.titleMedium)

                Spacer(Modifier.height(10.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Plataforma.entries.forEach { plat ->
                        val marcada = marcadas.contains(plat)
                        val corMarca = CorPlataforma[plat.name] ?: Destaque
                        // v4.1: chip com a COR DA MARCA — preenchido quando selecionado,
                        // só contornado na cor da marca quando não. eBay: contorno branco (multicor).
                        val corContorno = if (plat.name == "EBAY") androidx.compose.ui.graphics.Color.White else corMarca
                        FilterChip(
                            selected = marcada,
                            onClick = {
                                marcadas = if (marcada) marcadas - plat else marcadas + plat
                            },
                            border = androidx.compose.foundation.BorderStroke(1.dp, corContorno),
                            colors = androidx.compose.material3.FilterChipDefaults.filterChipColors(
                                containerColor = androidx.compose.ui.graphics.Color.Transparent,
                                selectedContainerColor = corMarca,
                                labelColor = if (plat.name == "MERCADO_LIVRE") androidx.compose.ui.graphics.Color(0xFF2D3277) else androidx.compose.ui.graphics.Color.White,
                                selectedLabelColor = if (plat.name == "MERCADO_LIVRE") androidx.compose.ui.graphics.Color(0xFF2D3277) else androidx.compose.ui.graphics.Color.White
                            ),
                            label = { Text(plat.rotulo) }
                        )
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
                            "${naoConectadas.joinToString { it.rotulo }} sem conexão — conecte na aba Conexões ou o anúncio vai falhar.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
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
                        contentColor = androidx.compose.ui.graphics.Color(0xFF12092B)
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
