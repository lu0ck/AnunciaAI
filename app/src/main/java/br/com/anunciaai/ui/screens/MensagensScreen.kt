package br.com.anunciaai.ui.screens

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Mail
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import br.com.anunciaai.AnunciaAIApp
import br.com.anunciaai.publica.mercadolivre.MercadoLivreApi
import br.com.anunciaai.publica.mercadolivre.PerguntaML
import br.com.anunciaai.ui.EstadoVazio
import br.com.anunciaai.ui.IconePlataforma
import br.com.anunciaai.ui.Plataforma
import br.com.anunciaai.ui.theme.CorTexto
import br.com.anunciaai.ui.theme.CorTextoSec
import br.com.anunciaai.ui.theme.Destaque
import kotlinx.coroutines.launch

/** v9 — Estado de não-lidas do inbox (badge da cápsula). MensagensScreen publica; cápsula lê. */
object EstadoInbox {
    val naoLidas = kotlinx.coroutines.flow.MutableStateFlow(0)
}

/**
 * TELA 5 — MENSAGENS (v12.0, reescrita do zero).
 * Mesma estrutura visual da Conexões (badge + nome + texto, separador 1px,
 * sem sombra): prévia da última mensagem + HORÁRIO quando vier de API real
 * (Mercado Livre); "Abrir conversa" nas plataformas sem API.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MensagensScreen() {
    val contexto = LocalContext.current
    val app = contexto.applicationContext as AnunciaAIApp
    val escopo = rememberCoroutineScope()
    val contas by app.repositorio.contas().collectAsState(initial = emptyList())

    var perguntas by remember { mutableStateOf<List<PerguntaML>>(emptyList()) }
    var carregando by remember { mutableStateOf(false) }
    var erro by remember { mutableStateOf<String?>(null) }
    var responder by remember { mutableStateOf<PerguntaML?>(null) }
    var textoResposta by remember { mutableStateOf(TextFieldValue("")) }

    LaunchedEffect(contas.size) {
        val conta = contas.firstOrNull { it.plataforma == "MERCADO_LIVRE" }
        if (conta != null) {
            carregando = true
            erro = null
            try {
                perguntas = MercadoLivreApi().puxarPerguntas(conta.accessTokenCriptografado, "")
                    .also { EstadoInbox.naoLidas.value = it.size }
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

    // linhas do inbox: perguntas do ML (API real) + plataformas sem API
    data class LinhaInbox(
        val plataforma: String,
        val conectada: Boolean,
        val titulo: String,
        val previa: String?,
        val horario: String?,
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
                    horario = p.data.take(10),
                    viaApi = true,
                    onClick = { responder = p; textoResposta = TextFieldValue("") }
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
                    horario = null,
                    viaApi = false,
                    onClick = { abrirInbox(url) }
                )
            )
        }
    }

    Scaffold(
        containerColor = br.com.anunciaai.ui.theme.CorFundo,
        topBar = {
            TopAppBar(
                title = { Text("Mensagens", style = MaterialTheme.typography.headlineSmall, color = CorTexto) }
            )
        }
    ) { pad ->
        Column(Modifier.padding(pad).fillMaxSize()) {
            if (carregando) {
                Row(
                    Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp, color = Destaque)
                    Spacer(Modifier.width(10.dp))
                    Text(
                        "Buscando perguntas...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = CorTextoSec
                    )
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

            if (linhas.isEmpty() && !carregando) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    EstadoVazio(
                        icone = Icons.Outlined.Mail,
                        titulo = "Nada por aqui ainda",
                        subtitulo = "Quando os compradores mandarem mensagens ou perguntas, elas aparecem aqui."
                    )
                }
            } else {
                LazyColumn(Modifier.fillMaxSize()) {
                    items(linhas, key = { "${it.plataforma}_${it.titulo}" }) { linha ->
                        LinhaMensagem(
                            plataforma = linha.plataforma,
                            conectada = linha.conectada,
                            titulo = linha.titulo,
                            previa = linha.previa,
                            horario = linha.horario,
                            viaApi = linha.viaApi,
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

/** Linha da mensagem: badge da marca + remetente + prévia + horário/"Abrir conversa". */
@Composable
private fun LinhaMensagem(
    plataforma: String,
    conectada: Boolean,
    titulo: String,
    previa: String?,
    horario: String?,
    viaApi: Boolean,
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
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    titulo, style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = CorTexto,
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
                Text(
                    previa ?: "Abrir conversa",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (previa == null) Destaque else CorTextoSec,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            // horário quando vem de API real
            if (viaApi && horario != null) {
                Text(
                    horario,
                    style = MaterialTheme.typography.labelSmall,
                    color = CorTextoSec
                )
            }
        }
        HorizontalDivider(
            Modifier.padding(top = 12.dp),
            color = MaterialTheme.colorScheme.outlineVariant
        )
    }
}
