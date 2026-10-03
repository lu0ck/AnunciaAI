package br.com.anunciaai.plataformas.webview

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.os.Bundle
import android.view.View
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebResourceError
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import br.com.anunciaai.AnunciaAIApp
import br.com.anunciaai.R
import br.com.anunciaai.publica.webview.SessaoPublicacaoWeb
import br.com.anunciaai.ui.theme.AnunciaAITheme
import kotlinx.serialization.json.JsonPrimitive

/**
 * WebView de "criar anúncio" para plataformas sem API (OLX / Facebook / Enjoei / Shopee).
 * v4.1: user-agent de Chrome Android REAL (sites anti-bot bloqueiam o UA default do WebView),
 * domStorage + database + cookies de terceiros persistidos, e TELA DE ERRO amigável com
 * "Tentar novamente" quando a plataforma devolve HTTP de erro (ex.: OLX bloqueando).
 */
class LoginWebViewActivity : ComponentActivity() {

    companion object {
        // Chrome Android real, ATUALIZADO v11.1 (fix #5): o UA default do WebView
        // contém "; wv" e é bloqueado por anti-bot (OLX). Aplicado em
        // configurarWebView() ANTES de qualquer loadUrl().
        private const val UA_CHROME =
            "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 " +
                "(KHTML, like Gecko) Chrome/141.0.0.0 Mobile Safari/537.36"
    }

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

    /** Configuração comum do WebView (UA de Chrome real + storage + cookies). */
    private fun configurarWebView(view: WebView) {
        view.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            userAgentString = UA_CHROME
            loadsImagesAutomatically = true
            mediaPlaybackRequiresUserGesture = false
            cacheMode = WebSettings.LOAD_DEFAULT
        }
        CookieManager.getInstance().apply {
            setAcceptCookie(true)
            setAcceptThirdPartyCookies(view, true)
        }
        // sessão de navegação "normal" antes da página de anúncio ajuda a passar anti-bot
        view.loadUrl("https://www.google.com/")
    }

    /** Modo login (Conexões): só abre a plataforma pro usuário logar; sem script. */
    @Composable
    private fun LoginTela(app: AnunciaAIApp, somenteLogin: Boolean, urlLogin: String) {
        var erro by remember { mutableStateOf<String?>(null) }
        var recarregar by remember { mutableStateOf(0) }

        Surface(Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxSize()) {
                Row(
                    Modifier.fillMaxWidth().padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { finish() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar")
                    }
                    Text(
                        if (somenteLogin) "Faça login — a sessão fica salva no celular"
                        else "Nenhuma publicação pendente.",
                        Modifier.weight(1f),
                        style = MaterialTheme.typography.titleSmall
                    )
                    TextButton(onClick = { finish() }) { Text("Fechar") }
                }
                if (somenteLogin && urlLogin.isNotEmpty()) {
                    Box(Modifier.weight(1f).fillMaxWidth()) {
                        AndroidView(
                            modifier = Modifier.fillMaxSize(),
                            factory = { contexto ->
                                WebView(contexto).apply {
                                    configurarWebView(this)
                                    webViewClient = object : WebViewClient() {
                                        override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
                                            erro = null
                                        }
                                        // PILAR 5: captura HTTP de erro (ex.: 403 da OLX anti-bot)
                                        override fun onReceivedHttpError(
                                            view: WebView,
                                            request: WebResourceRequest,
                                            errorResponse: WebResourceResponse
                                        ) {
                                            if (request.isForMainFrame) {
                                                erro = "A plataforma respondeu HTTP ${errorResponse.statusCode}" +
                                                    " — pode estar bloqueando automação."
                                            }
                                        }
                                        override fun onReceivedError(
                                            view: WebView, request: WebResourceRequest, error: WebResourceError
                                        ) {
                                            if (request.isForMainFrame) {
                                                erro = "A página não carregou (${error.description})"
                                            }
                                        }
                                    }
                                    // visita o Google primeiro (deixa cookie de navegação), depois vai pro login
                                    postDelayed({ loadUrl(urlLogin) }, 1200)
                                }
                            },
                            update = { if (recarregar > 0) it.loadUrl(urlLogin) },
                        )
                        erro?.let { msg ->
                            ErroPlataforma(
                                msg = msg,
                                onTentarNovamente = { recarregar++; erro = null },
                                modifier = Modifier.align(Alignment.Center)
                            )
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun WebViewTela(app: AnunciaAIApp, pedido: SessaoPublicacaoWeb.Pedido) {
        var tentativas by remember { mutableIntStateOf(0) }
        var status by remember { mutableStateOf("Carregando a página da plataforma...") }
        var erro by remember { mutableStateOf<String?>(null) }
        var recarregar by remember { mutableStateOf(0) }

        Column(Modifier.fillMaxSize()) {
            Row(
                Modifier.fillMaxWidth().padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(status, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                TextButton(onClick = {
                    SessaoPublicacaoWeb.reportar(app, false, "cancelado pelo usuário")
                    finish()
                }) { Text("Não consegui") }
            }
            Box(Modifier.weight(1f).fillMaxWidth()) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { contexto ->
                        WebView(contexto).apply {
                            configurarWebView(this)
                            webViewClient = object : WebViewClient() {
                                override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
                                    erro = null
                                    status = "Carregando a página da plataforma..."
                                }
                                override fun onPageFinished(view: WebView, url: String) {
                                    if (erro == null) {
                                        status = "Preenchendo o anúncio..."
                                        agendarTentativas(view, app, pedido, { tentativas = it }, { status = it })
                                    }
                                }
                                // PILAR 5: HTTP de erro no main frame (anti-bot da OLX)
                                override fun onReceivedHttpError(
                                    view: WebView,
                                    request: WebResourceRequest,
                                    errorResponse: WebResourceResponse
                                ) {
                                    if (request.isForMainFrame) {
                                        erro = "A plataforma respondeu HTTP ${errorResponse.statusCode}" +
                                            " — pode estar bloqueando automação."
                                    }
                                }
                                override fun onReceivedError(
                                    view: WebView, request: WebResourceRequest, error: WebResourceError
                                ) {
                                    if (request.isForMainFrame) {
                                        erro = "A página não carregou (${error.description})."
                                    }
                                }
                            }
                            // visita o Google primeiro, depois abre a página de anúncio
                            postDelayed({ loadUrl(pedido.url) }, 1200)
                        }
                    },
                    update = { if (recarregar > 0) it.loadUrl(pedido.url) }
                )
                erro?.let { msg ->
                    ErroPlataforma(
                        msg = msg,
                        onTentarNovamente = { recarregar++; erro = null },
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }
        }
    }

    /** Tela de erro amigável dentro do app (não o erro cru do WebView). */
    @Composable
    private fun ErroPlataforma(
        msg: String,
        onTentarNovamente: () -> Unit,
        modifier: Modifier = Modifier
    ) {
        Card(
            modifier = modifier.padding(24.dp).fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
        ) {
            Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Default.WarningAmber, contentDescription = null,
                    tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(36.dp)
                )
                Spacer(Modifier.height(12.dp))
                Text("Não consegui abrir a plataforma", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(6.dp))
                Text(
                    msg + "\n\nA plataforma pode estar bloqueando automação. Se persistir, publique manualmente dessa vez.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(16.dp))
                Button(onClick = onTentarNovamente) {
                    Icon(Icons.Default.Refresh, contentDescription = null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Tentar novamente")
                }
            }
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
        // evita duplicar agendamentos a cada onPageFinished
        if (view.getTag(R.id.tag_tentativas) == true) return
        view.setTag(R.id.tag_tentativas, true)
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
