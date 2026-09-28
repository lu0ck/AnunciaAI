package br.com.anunciaai.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
import br.com.anunciaai.ui.BarraInferior
import br.com.anunciaai.ui.Plataforma
import br.com.anunciaai.ui.Rotas
import br.com.anunciaai.ui.foto.CarrosselFotos
import br.com.anunciaai.ui.foto.FotoUtil
import br.com.anunciaai.ui.theme.CorPlataforma
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
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
    var condicao by remember { mutableStateOf("usado - bom estado") }
    var marcadas by remember { mutableStateOf(setOf(Plataforma.MERCADO_LIVRE, Plataforma.OLX)) }
    var gerando by remember { mutableStateOf(false) }
    var publicando by remember { mutableStateOf(false) }
    var erro by remember { mutableStateOf<String?>(null) }
    var preenchidoOnce by remember { mutableStateOf(false) }

    fun preencher(s: SugestaoIA) {
        titulo = TextFieldValue(s.titulo)
        descricao = TextFieldValue(s.descricao)
        categoria = TextFieldValue(s.categoria_sugerida)
        preco = TextFieldValue(if (s.precoSugeridoReais > 0) "%.2f".format(s.precoSugeridoReais) else "")
        if (s.condicao.isNotBlank()) condicao = s.condicao
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
                title = { Text("Revisar anúncio") },
                navigationIcon = { TextButton(onClick = onVoltar) { Text("←") } }
            )
        },
        bottomBar = { BarraInferior(Rotas.CAPTURA, onNavBottom) }
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
                OutlinedTextField(
                    value = titulo, onValueChange = { titulo = it },
                    label = { Text("Título (até 60 caracteres)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.small
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = descricao, onValueChange = { descricao = it },
                    label = { Text("Descrição") },
                    modifier = Modifier.fillMaxWidth(), minLines = 3,
                    shape = MaterialTheme.shapes.small
                )
                Spacer(Modifier.height(10.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = preco, onValueChange = { preco = it },
                        label = { Text("Preço R$") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                        shape = MaterialTheme.shapes.small
                    )
                    OutlinedTextField(
                        value = categoria, onValueChange = { categoria = it },
                        label = { Text("Categoria") },
                        modifier = Modifier.weight(1f),
                        shape = MaterialTheme.shapes.small
                    )
                }
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = condicao.startsWith("novo", true),
                        onClick = { condicao = "novo" },
                        label = { Text("novo") }
                    )
                    FilterChip(
                        selected = condicao.contains("como novo"),
                        onClick = { condicao = "usado - como novo" },
                        label = { Text("como novo") }
                    )
                    FilterChip(
                        selected = condicao.contains("bom estado"),
                        onClick = { condicao = "usado - bom estado" },
                        label = { Text("bom estado") }
                    )
                    FilterChip(
                        selected = condicao.contains("marcas"),
                        onClick = { condicao = "usado - com marcas de uso" },
                        label = { Text("marcas de uso") }
                    )
                }
                TextButton(onClick = { gerarComIA() }, enabled = !gerando && fotos.isNotEmpty()) {
                    Text(if (gerando) "Gerando..." else "Gerar com a IA")
                }

                Spacer(Modifier.height(12.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                Spacer(Modifier.height(12.dp))
                Text("Onde publicar", style = MaterialTheme.typography.titleMedium)

                Spacer(Modifier.height(6.dp))
                Plataforma.entries.forEach { plat ->
                    val conectada = if (plat.precisaOAuth) conectadas.contains(plat.name) else true
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = marcadas.contains(plat),
                            onCheckedChange = { on ->
                                marcadas = if (on) marcadas + plat else marcadas - plat
                            }
                        )
                        // indicador inline: dot da marca + nome
                        Box(
                            Modifier.size(10.dp).background(
                                CorPlataforma[plat.name] ?: MaterialTheme.colorScheme.primary,
                                CircleShape
                            )
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(plat.rotulo, Modifier.weight(1f))
                        if (!conectada) {
                            Text(
                                "não conectada",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
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
                    shape = MaterialTheme.shapes.medium
                ) {
                    Text(if (publicando) "Publicando..." else "Publicar")
                }
                Spacer(Modifier.height(28.dp))
            }
        }
    }
}
