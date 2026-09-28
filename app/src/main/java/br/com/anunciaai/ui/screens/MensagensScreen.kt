package br.com.anunciaai.ui.screens

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import br.com.anunciaai.AnunciaAIApp
import br.com.anunciaai.publica.mercadolivre.MercadoLivreApi
import br.com.anunciaai.ui.BarraInferior
import br.com.anunciaai.ui.Plataforma
import br.com.anunciaai.ui.Rotas
import br.com.anunciaai.ui.theme.CorPlataforma
import kotlinx.coroutines.launch

/**
 * Mensagens (spec v2 §3): inbox unificado.
 * - ML: conversas REAIS via API (perguntas não respondidas) — responder pelo app
 * - OLX/FB/Enjoei/Shopee: atalho "Ver no site" (sem scraping)
 * Ordenado por data mais recente, indicador de não lida.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MensagensScreen(onNavBottom: (String) -> Unit = {}) {
    val contexto = LocalContext.current
    val app = contexto.applicationContext as AnunciaAIApp
    val escopo = rememberCoroutineScope()
    val contas by app.repositorio.contas().collectAsState(initial = emptyList())
    val itens by app.repositorio.itens().collectAsState(initial = emptyList())

    var perguntas by remember { mutableStateOf<List<br.com.anunciaai.publica.mercadolivre.PerguntaML>>(emptyList()) }
    var carregando by remember { mutableStateOf(false) }
    var erro by remember { mutableStateOf<String?>(null) }
    var responder by remember { mutableStateOf<br.com.anunciaai.publica.mercadolivre.PerguntaML?>(null) }
    var textoResposta by remember { mutableStateOf(androidx.compose.ui.text.input.TextFieldValue("")) }

    // puxa as perguntas reais do ML ao abrir (se conectado)
    LaunchedEffect(contas.size) {
        val conta = contas.firstOrNull { it.plataforma == "MERCADO_LIVRE" }
        if (conta != null) {
            carregando = true
            erro = null
            try {
                val userId = conta.expiraEm?.toString() // userId não persistido; usa /users/me abaixo
                val api = MercadoLivreApi()
                perguntas = api.puxarPerguntas(conta.accessTokenCriptografado, userId ?: "")
            } catch (e: Exception) {
                erro = "ML: ${e.message}"
            }
            carregando = false
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Mensagens") }) },
        bottomBar = { BarraInferior(Rotas.LISTA, onNavBottom) }
    ) { pad ->
        Column(Modifier.padding(pad).padding(16.dp)) {
            if (carregando) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(10.dp))
                    Text("Buscando perguntas...", style = MaterialTheme.typography.bodyMedium)
                }
                Spacer(Modifier.height(12.dp))
            }

            erro?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(10.dp))
            }

            Text("Perguntas do Mercado Livre", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(6.dp))

            if (perguntas.isEmpty() && !carregando) {
                Text(
                    if (contas.any { it.plataforma == "MERCADO_LIVRE" })
                        "Nenhuma pergunta não respondida."
                    else
                        "Conecte o Mercado Livre em Conexões para ver as perguntas aqui.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(perguntas, key = { it.id }) { p ->
                        Card(Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium) {
                            Column(Modifier.padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(Modifier.size(10.dp).background(
                                        CorPlataforma["MERCADO_LIVRE"]!!, CircleShape))
                                    Spacer(Modifier.width(8.dp))
                                    Text(p.deQuem, style = MaterialTheme.typography.titleSmall)
                                    Spacer(Modifier.weight(1f))
                                    Text("não lida", style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.secondary)
                                }
                                Spacer(Modifier.height(6.dp))
                                Text(p.texto, style = MaterialTheme.typography.bodyMedium)
                                Spacer(Modifier.height(8.dp))
                                Button(onClick = { responder = p; textoResposta = androidx.compose.ui.text.input.TextFieldValue("") }) {
                                    Text("Responder")
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
            Text("Mensagens das outras plataformas", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(6.dp))
            Text(
                "Estas plataformas não liberam mensagens por API — o botão abre o chat delas no seu celular.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            listOf(
                "OLX" to "https://chat.olx.com.br/",
                "FACEBOOK_MARKETPLACE" to "https://www.facebook.com/marketplace/inbox/",
                "ENJOEI" to "https://enjoei.com.br/minhas-mensagens",
                "SHOPEE" to "https://chat.seller.shopee.com.br/"
            ).forEach { (plat, url) ->
                OutlinedButton(
                    onClick = {
                        contexto.startActivity(
                            Intent(contexto, br.com.anunciaai.plataformas.webview.LoginWebViewActivity::class.java)
                                .putExtra("somente_login", true).putExtra("url", url)
                        )
                    },
                    Modifier.fillMaxWidth().padding(vertical = 2.dp)
                ) { Text("Ver no site — ${Plataforma.doNome(plat)?.rotulo ?: plat}") }
            }
        }

        // diálogo de resposta
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
                dismissButton = {
                    TextButton(onClick = { responder = null }) { Text("Cancelar") }
                }
            )
        }
    }
}
