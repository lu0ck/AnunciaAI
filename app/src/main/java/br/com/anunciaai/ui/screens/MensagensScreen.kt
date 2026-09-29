package br.com.anunciaai.ui.screens

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Chat
import androidx.compose.material.icons.outlined.Mail
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import br.com.anunciaai.AnunciaAIApp
import br.com.anunciaai.publica.mercadolivre.MercadoLivreApi
import br.com.anunciaai.ui.BottomDockCurvo
import br.com.anunciaai.ui.EstadoVazio
import br.com.anunciaai.ui.IconePlataforma
import br.com.anunciaai.ui.Plataforma
import br.com.anunciaai.ui.Rotas
import br.com.anunciaai.ui.theme.Destaque
import kotlinx.coroutines.launch

/**
 * Mensagens (v5.3 — ITEM 2 da spec visual):
 * - Barra de PILLS roláveis no topo: Todas / Não lidas / Ofertas
 *   (ativo = fundo destaque + texto branco; inativo = cinza-escuro + texto claro)
 * - Inbox unificado: toda linha com badge da marca, remetente, prévia, "Abrir conversa"
 * - Empty state premium centralizado (ITEM 3)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MensagensScreen(onNavBottom: (String) -> Unit = {}) {
    val contexto = LocalContext.current
    val app = contexto.applicationContext as AnunciaAIApp
    val escopo = rememberCoroutineScope()
    val contas by app.repositorio.contas().collectAsState(initial = emptyList())

    var perguntas by remember { mutableStateOf<List<br.com.anunciaai.publica.mercadolivre.PerguntaML>>(emptyList()) }
    var carregando by remember { mutableStateOf(false) }
    var erro by remember { mutableStateOf<String?>(null) }
    var responder by remember { mutableStateOf<br.com.anunciaai.publica.mercadolivre.PerguntaML?>(null) }
    var textoResposta by remember { mutableStateOf(androidx.compose.ui.text.input.TextFieldValue("")) }
    var filtro by remember { mutableStateOf("Todas") }

    val filtros = listOf("Todas", "Não lidas", "Ofertas")

    LaunchedEffect(contas.size) {
        val conta = contas.firstOrNull { it.plataforma == "MERCADO_LIVRE" }
        if (conta != null) {
            carregando = true
            erro = null
            try {
                perguntas = MercadoLivreApi().puxarPerguntas(conta.accessTokenCriptografado, "")
            } catch (e: Exception) {
                erro = "ML: ${e.message}"
            }
            carregando = false
        }
    }

    fun abrirInbox(url: String) {
        contexto.startActivity(
            Intent(contexto, br.com.anunciaai.plataformas.webview.LoginWebViewActivity::class.java)
                .putExtra("somente_login", true).putExtra("url", url)
        )
    }

    // linhas: perguntas do ML + plataformas sem API
    data class LinhaInbox(
        val plataforma: String,
        val conectada: Boolean,
        val titulo: String,
        val previa: String?,
        val viaApi: Boolean,
        val onClick: () -> Unit
    )
    val plataformasWeb = listOf(
        "OLX" to "https://chat.olx.com.br/",
        "FACEBOOK_MARKETPLACE" to "https://www.facebook.com/marketplace/inbox/",
        "ENJOEI" to "https://enjoei.com.br/minhas-mensagens",
        "SHOPEE" to "https://chat.seller.shopee.com.br/"
    )
    val linhas = buildList {
        perguntas.forEach { p ->
            add(
                LinhaInbox(
                    plataforma = "MERCADO_LIVRE",
                    conectada = true,
                    titulo = p.deQuem,
                    previa = p.texto,
                    viaApi = true,
                    onClick = { responder = p; textoResposta = androidx.compose.ui.text.input.TextFieldValue("") }
                )
            )
        }
        plataformasWeb.forEach { (plat, url) ->
            val conectada = contas.any { it.plataforma == plat }
            add(
                LinhaInbox(
                    plataforma = plat,
                    conectada = conectada,
                    titulo = Plataforma.doNome(plat)?.rotulo ?: plat,
                    previa = null,
                    viaApi = false,
                    onClick = { abrirInbox(url) }
                )
            )
        }
    }
    // filtros: "Não lidas" = perguntas do ML; "Ofertas" = linhas sem API; "Todas" = tudo
    val linhasFiltradas = when (filtro) {
        "Não lidas" -> linhas.filter { it.viaApi }
        "Ofertas" -> linhas.filter { !it.viaApi }
        else -> linhas
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Mensagens") }) },
        bottomBar = { BottomDockCurvo(Rotas.MENSAGENS, onNavBottom) }
    ) { pad ->
        Column(Modifier.padding(pad).fillMaxSize()) {
            // ── Pills de filtro (ITEM 2) ──
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filtros) { f ->
                    val ativo = f == filtro
                    androidx.compose.material3.FilterChip(
                        selected = ativo,
                        onClick = { filtro = f },
                        shape = CircleShape,
                        colors = androidx.compose.material3.FilterChipDefaults.filterChipColors(
                            containerColor = androidx.compose.ui.graphics.Color(0xFF23272E), // cinza-escuro
                            labelColor = androidx.compose.ui.graphics.Color(0xFFF2F2F0),
                            selectedContainerColor = Destaque,          // azul/verde-destaque
                            selectedLabelColor = androidx.compose.ui.graphics.Color.White
                        ),
                        border = null,
                        label = { Text(f, style = MaterialTheme.typography.labelLarge) }
                    )
                }
            }

            if (carregando) {
                Row(
                    Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp, color = Destaque)
                    Spacer(Modifier.width(10.dp))
                    Text("Buscando perguntas...", style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            erro?.let {
                Text(
                    it,
                    Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            if (linhasFiltradas.isEmpty() && !carregando) {
                // ── Empty state premium (ITEM 3) ──
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    EstadoVazio(
                        icone = Icons.Outlined.Mail,
                        titulo = "Nada por aqui ainda",
                        subtitulo = "Quando os compradores mandarem mensagens ou perguntas, elas aparecem aqui.",
                        textoBotao = "Conectar plataformas",
                        onBotao = { onNavBottom(Rotas.PERFIL) },
                        modifier = Modifier
                    )
                }
            } else {
                LazyColumn(Modifier.fillMaxSize()) {
                    items(linhasFiltradas, key = { "${it.plataforma}_${it.titulo}" }) { linha ->
                        LinhaMensagem(
                            plataforma = linha.plataforma,
                            conectada = linha.conectada,
                            titulo = linha.titulo,
                            previa = linha.previa,
                            onClick = linha.onClick
                        )
                    }
                }
            }
        }
    }

    // diálogo de resposta (ML via API)
    responder?.let { p ->
        AlertDialog(
            onDismissRequest = { responder = null },
            title = { Text("Responder ${p.deQuem}") },
            text = {
                OutlinedTextField(
                    value = textoResposta,
                    onValueChange = { textoResposta = it },
                    placeholder = { Text("Sua resposta") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = {
                    val conta = contas.firstOrNull { it.plataforma == "MERCADO_LIVRE" }
                    if (conta != null && textoResposta.text.isNotBlank()) {
                        escopo.launch {
                            val ok = MercadoLivreApi().responderPergunta(
                                conta.accessTokenCriptografado, p.id, textoResposta.text.trim()
                            )
                            if (ok) perguntas = perguntas.filter { it.id != p.id }
                            responder = null
                        }
                    } else responder = null
                }) { Text("Enviar") }
            },
            dismissButton = { TextButton(onClick = { responder = null }) { Text("Cancelar") } }
        )
    }
}

// estado de navegação pra Conexões a partir do empty state
private var abrirConexoes by androidx.compose.runtime.mutableStateOf(false)

/** Linha de mensagem: badge da marca + remetente + prévia/Abrir conversa. */
@Composable
private fun LinhaMensagem(
    plataforma: String,
    conectada: Boolean,
    titulo: String,
    previa: String?,
    onClick: () -> Unit
) {
    Column(
        Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconePlataforma(plataforma, conectada = conectada, tamanho = 36.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(titulo, style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    previa ?: "Abrir conversa",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (previa == null) Destaque else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        HorizontalDivider(
            Modifier.padding(top = 12.dp),
            color = MaterialTheme.colorScheme.outlineVariant
        )
    }
}
