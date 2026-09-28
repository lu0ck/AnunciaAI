package br.com.anunciaai.plataformas.webview

import android.annotation.SuppressLint
import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import br.com.anunciaai.AnunciaAIApp
import br.com.anunciaai.publica.webview.SessaoPublicacaoWeb
import br.com.anunciaai.ui.theme.AnunciaAITheme
import kotlinx.serialization.json.JsonPrimitive

/**
 * WebView de "criar anúncio" para plataformas sem API (OLX / Facebook / Enjoei / Shopee).
 * Abre a página real com a sessão logada do usuário (CookieManager do Android),
 * injeta os dados gerados pela IA e o script preenche os campos e clica em publicar.
 * Nenhuma senha passa por aqui — o login foi feito uma vez pelo próprio usuário.
 */
class LoginWebViewActivity : ComponentActivity() {
    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = applicationContext as AnunciaAIApp
        val somenteLogin = intent?.getBooleanExtra("somente_login", false) ?: false
        val urlLogin = intent?.getStringExtra("url") ?: ""
        val pedido = if (somenteLogin) null else SessaoPublicacaoWeb.atual

        setContent {
            AnunciaAITheme {
                if (pedido == null) {
                    LoginTela(app, somenteLogin, urlLogin)
                } else {
                    WebViewTela(app, pedido)
                }
            }
        }
    }

    /** Modo login (Conexões): só abre a plataforma pro usuário logar; sem script. */
    @Composable
    private fun LoginTela(app: AnunciaAIApp, somenteLogin: Boolean, urlLogin: String) {
        Surface(Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxSize()) {
                Row(
                    Modifier.fillMaxWidth().padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        if (somenteLogin) "Faça login — a sessão fica salva no celular" 
                        else "Nenhuma publicação pendente.",
                        Modifier.weight(1f),
                        style = MaterialTheme.typography.titleSmall
                    )
                    TextButton(onClick = { finish() }) { Text("Fechar") }
                }
                if (somenteLogin && urlLogin.isNotEmpty()) {
                    AndroidView(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        factory = { contexto ->
                            WebView(contexto).apply {
                                settings.javaScriptEnabled = true
                                settings.domStorageEnabled = true
                                settings.userAgentString =
                                    settings.userAgentString.replace("; wv", "")
                                loadUrl(urlLogin)
                            }
                        }
                    )
                }
            }
        }
    }

    @Composable
    private fun WebViewTela(app: AnunciaAIApp, pedido: SessaoPublicacaoWeb.Pedido) {
        var tentativas by remember { mutableIntStateOf(0) }
        var status by remember { mutableStateOf("Carregando a página da plataforma...") }

        Column(Modifier.fillMaxSize()) {
            // Barra superior: status + botão de erro manual
            Row(
                Modifier.fillMaxWidth().padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    status,
                    Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium
                )
                TextButton(onClick = {
                    SessaoPublicacaoWeb.reportar(app, false, "cancelado pelo usuário")
                    finish()
                }) { Text("Não consegui") }
            }
            AndroidView(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                factory = { contexto ->
                    WebView(contexto).apply {
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        // user agent de navegador normal (remove o "; wv" do WebView)
                        settings.userAgentString =
                            settings.userAgentString.replace("; wv", "")
                        webViewClient = object : WebViewClient() {
                            override fun onPageFinished(view: WebView, url: String) {
                                status = "Preenchendo o anúncio..."
                                agendarTentativas(view, app, pedido, { tentativas = it }, { status = it })
                            }
                        }
                        loadUrl(pedido.url)
                    }
                }
            )
        }
    }

    /**
     * Tenta preencher e publicar até 10 vezes (3s de intervalo) — a página da
     * plataforma pode carregar os campos com atraso (React/SPA).
     */
    private fun agendarTentativas(
        view: WebView,
        app: AnunciaAIApp,
        pedido: SessaoPublicacaoWeb.Pedido,
        atualizarTentativas: (Int) -> Unit,
        atualizarStatus: (String) -> Unit
    ) {
        val prefixo =
            "window.ANUNCIAAI_TITULO=${JsonPrimitive(pedido.titulo)};" +
                "window.ANUNCIAAI_PRECO=${JsonPrimitive(pedido.preco)};" +
                "window.ANUNCIAAI_DESC=${JsonPrimitive(pedido.descricao)};"
        var n = 0
        fun tentar() {
            n++
            atualizarTentativas(n)
            view.evaluateJavascript(prefixo + pedido.js) { r ->
                when {
                    r.contains("\"ok\":true") -> {
                        atualizarStatus("Publicado!")
                        SessaoPublicacaoWeb.reportar(app, true, "preenchido e publicado via WebView")
                        finish()
                    }
                    n < 10 -> view.postDelayed({ tentar() }, 3000)
                    else -> {
                        atualizarStatus("Não achei os campos do formulário.")
                        SessaoPublicacaoWeb.reportar(
                            app, false,
                            "script não achou os campos (página mudou?): ${r.take(120)}"
                        )
                        finish()
                    }
                }
            }
        }
        view.postDelayed({ tentar() }, 2500)
    }
}
