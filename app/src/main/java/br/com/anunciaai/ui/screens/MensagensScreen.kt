package br.com.anunciaai.ui.screens

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import br.com.anunciaai.ui.BarraInferior
import br.com.anunciaai.ui.IconePlataforma
import br.com.anunciaai.ui.Plataforma
import br.com.anunciaai.ui.Rotas
import br.com.anunciaai.ui.theme.Destaque
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date

/**
 * Mensagens (spec v3): inbox unificado — TODAS as linhas com a MESMA estrutura:
 * monograma da plataforma, remetente, prévia, horário. Plataformas sem API
 * mostram "Abrir conversa" no lugar da prévia — mesma estrutura visual, sem lista separada.
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

    Scaffold(
        topBar = { TopAppBar(title = { Text("Mensagens") }) },
        bottomBar = { BarraInferior(Rotas.MENSAGENS, onNavBottom) }
    ) { pad ->
        Column(Modifier.padding(pad).fillMaxSize()) {
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

            LazyColumn(Modifier.fillMaxSize()) {
                // Perguntas reais do ML (API)
                items(perguntas, key = { "ml_${it.id}" }) { p ->
                    LinhaMensagem(
                        plataforma = "MERCADO_LIVRE",
                        conectada = true,
                        titulo = p.deQuem,
                        previa = p.texto,
                        horario = "",
                        onClick = { responder = p; textoResposta = androidx.compose.ui.text.input.TextFieldValue("") }
                    )
                }
                // Plataformas sem API — MESMA estrutura de linha, rótulo "Abrir conversa"
                item {
                    val semApi = listOf(
                        "OLX" to "https://chat.olx.com.br/",
                        "FACEBOOK_MARKETPLACE" to "https://www.facebook.com/marketplace/inbox/",
                        "ENJOEI" to "https://enjoei.com.br/minhas-mensagens",
                        "SHOPEE" to "https://chat.seller.shopee.com.br/"
                    )
                    semApi.forEach { (plat, url) ->
                        val conectada = contas.any { it.plataforma == plat }
                        LinhaMensagem(
                            plataforma = plat,
                            conectada = conectada,
                            titulo = Plataforma.doNome(plat)?.rotulo ?: plat,
                            previa = null,
                            horario = "",
                            onClick = { abrirInbox(url) }
                        )
                    }
                }
            }

            if (perguntas.isEmpty() && !carregando && contas.none { it.plataforma == "MERCADO_LIVRE" }) {
                Box(Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                    Text(
                        "Conecte o Mercado Livre em Conexões para ver as perguntas dos compradores.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
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

/** Linha de mensagem: monograma + título + prévia + horário — mesma estrutura pra todas. */
@Composable
private fun LinhaMensagem(
    plataforma: String,
    conectada: Boolean,
    titulo: String,
    previa: String?,
    horario: String?,
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
            horario?.takeIf { it.isNotBlank() }?.let {
                Text(it, style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        HorizontalDivider(
            Modifier.padding(top = 12.dp),
            color = MaterialTheme.colorScheme.outlineVariant
        )
    }
}
