package br.com.anunciaai.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import br.com.anunciaai.AnunciaAIApp
import br.com.anunciaai.publica.webview.SessaoPublicacaoWeb
import br.com.anunciaai.publica.webview.ScriptsWeb
import br.com.anunciaai.ui.BarraInferior
import br.com.anunciaai.ui.Plataforma
import br.com.anunciaai.ui.Rotas
import br.com.anunciaai.ui.foto.CarrosselFotos
import br.com.anunciaai.ui.foto.FotoMini
import br.com.anunciaai.ui.theme.CorPlataforma
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetalheItemScreen(itemId: Long, onVoltar: () -> Unit) {
    val contexto = LocalContext.current
    val app = contexto.applicationContext as AnunciaAIApp
    val escopo = rememberCoroutineScope()

    val item by app.repositorio.item(itemId).collectAsState(initial = null)
    val fotos by app.repositorio.fotosDoItem(itemId).collectAsState(initial = emptyList())
    val pubs by app.repositorio.publicacoesDoItem(itemId).collectAsState(initial = emptyList())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Detalhe") },
                navigationIcon = { TextButton(onClick = onVoltar) { Text("←") } }
            )
        }
    ) { pad ->
        val it = item
        if (it == null) {
            Column(Modifier.padding(pad).padding(24.dp)) { Text("Item não encontrado.") }
        } else {
            Column(
                Modifier.padding(pad).fillMaxSize().verticalScroll(rememberScrollState())
            ) {
                // carrossel das fotos
                Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    CarrosselFotos(
                        fotos = fotos,
                        onAdicionar = {},
                        onRemover = { foto -> escopo.launch { app.repositorio.removerFoto(foto.id) } },
                        onMover = { de, para ->
                            val lista = fotos.toMutableList()
                            val f = lista.removeAt(de)
                            lista.add(para, f)
                            escopo.launch { app.repositorio.reordenarFotos(lista) }
                        }
                    )
                }

                Column(Modifier.padding(horizontal = 16.dp)) {
                    Text(it.titulo.ifEmpty { "(sem título)" }, style = MaterialTheme.typography.headlineSmall)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "R$ ${"%.2f".format(it.precoFinal.takeIf { p -> p > 0 } ?: it.precoSugerido)}",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (it.categoria.isNotEmpty()) {
                        Text(it.categoria, style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(it.descricao.ifEmpty { "(sem descrição)" }, style = MaterialTheme.typography.bodyMedium)

                    Spacer(Modifier.height(16.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                    Spacer(Modifier.height(12.dp))
                    Text("Publicações", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))

                    if (pubs.isEmpty()) {
                        Text("Nunca publicado.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        pubs.forEach { pub ->
                            val (cor, estado) = quando(pub.status)
                            Row(
                                Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(Modifier.size(10.dp).background(cor, CircleShape))
                                Spacer(Modifier.width(10.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        "${Plataforma.doNome(pub.plataforma)?.rotulo ?: pub.plataforma} — $estado",
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                    pub.urlAnuncio?.let {
                                        Text(it, style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                                    }
                                    pub.mensagemErro?.let {
                                        Text(it, style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.error)
                                    }
                                }
                                // encerrar (marca vendido e encerra no ML)
                                if (pub.status == "PUBLICADO") {
                                    TextButton(onClick = {
                                        escopo.launch {
                                            encerrarEm(app, itemId, pub.plataforma, pub.idExterno)
                                        }
                                    }) { Text("Vendi") }
                                }
                                // ver mensagens (WebView nas plataformas sem API)
                                if (pub.status == "PUBLICADO") {
                                    TextButton(onClick = {
                                        val url = inboxDe(pub.plataforma)
                                        if (url != null) {
                                            contexto.startActivity(
                                                android.content.Intent(
                                                    contexto,
                                                    br.com.anunciaai.plataformas.webview.LoginWebViewActivity::class.java
                                                ).putExtra("somente_login", true).putExtra("url", url)
                                            )
                                        }
                                    }) { Text("Mensagens") }
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = {
                            escopo.launch {
                                app.repositorio.apagarItem(itemId)
                                onVoltar()
                            }
                        },
                        Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) { Text("Apagar item") }

                    Spacer(Modifier.height(28.dp))
                }
            }
        }
    }
}

/** Marca vendido na plataforma e encerra o anúncio lá (ML via API). */
private suspend fun encerrarEm(
    app: AnunciaAIApp,
    itemId: Long,
    plataforma: String,
    idExterno: String?
) {
    val pub = app.repositorio.publicacaoDoItemEPlataformaNow(itemId, plataforma) ?: return
    if (plataforma == "MERCADO_LIVRE" && idExterno != null) {
        val conta = app.repositorio.conta(plataforma) ?: return
        try {
            br.com.anunciaai.publica.mercadolivre.MercadoLivreApi()
                .encerrarAnuncio(conta.accessTokenCriptografado, idExterno)
        } catch (e: Exception) { /* segue com o status local */ }
    }
    app.repositorio.atualizarPublicacao(pub.copy(status = "VENDIDO"))
}

/** URL do inbox de mensagens da plataforma (sem scraping — só abre a página real). */
private fun inboxDe(plataforma: String): String? = when (plataforma) {
    "OLX" -> "https://chat.olx.com.br/"
    "FACEBOOK_MARKETPLACE" -> "https://www.facebook.com/marketplace/inbox/"
    "ENJOEI" -> "https://enjoei.com.br/minhas-mensagens"
    "SHOPEE" -> "https://chat.seller.shopee.com.br/"
    else -> null
}
