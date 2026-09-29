package br.com.anunciaai.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import br.com.anunciaai.publica.mercadolivre.MercadoLivreApi
import br.com.anunciaai.oauth.CredenciaisML
import br.com.anunciaai.ui.theme.CorPlataforma
import br.com.anunciaai.ui.Plataforma
import kotlinx.coroutines.launch

/**
 * Tela de conexão de contas (spec §8.5):
 * - ML: login OAuth real (abre navegador, volta pelo deep link)
 * - eBay: OAuth (quando tiver keys)
 * - Shopee/OLX/FB/Enjoei: WebView de login manual único (sessão fica no CookieManager)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConexoesScreen() {
    val contexto = LocalContext.current
    val app = contexto.applicationContext as AnunciaAIApp
    val escopo = rememberCoroutineScope()
    val contas by app.repositorio.contas().collectAsState(initial = emptyList())
    val msg by br.com.anunciaai.oauth.EstadoConexao.msg.collectAsState()

    val conectadas = contas.map { it.plataforma }.toSet()

    Scaffold(topBar = { TopAppBar(title = { Text("Conexões") }) }) { pad ->
        Column(
            Modifier.padding(pad).padding(16.dp).verticalScroll(rememberScrollState())
        ) {
            msg?.let {
                Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(
                    containerColor = if (it.ok) MaterialTheme.colorScheme.tertiaryContainer
                    else MaterialTheme.colorScheme.errorContainer
                )) {
                    Text(it.texto, Modifier.padding(12.dp), style = MaterialTheme.typography.bodySmall)
                }
                Spacer(Modifier.height(12.dp))
            }

            Text(
                "Conecte suas contas uma vez. A sessão fica salva no próprio celular — nunca enviamos sua senha.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))

            // Cartão da IA: mostra se a chave está embutida NESTE build (versionCode 3+).
            // Se aparecer "manual", o APK foi buildado sem chave — rebuildar com local.properties preenchido.
            Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Column(Modifier.padding(14.dp)) {
                    val provedor = br.com.anunciaai.ia.FabricaIA.nomeAtivo()
                    Text("Inteligência artificial", style = MaterialTheme.typography.titleSmall)
                    Text(
                        if (provedor != null) "✓ IA ativa: $provedor — toque em Publicar item e a descrição sai pronta"
                        else "✗ Sem chave de IA neste build — preenchimento manual (rebuildar com local.properties)",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (provedor != null) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.error
                    )
                }
            }
            Spacer(Modifier.height(8.dp))

            Plataforma.entries.forEach { plat ->
                val conectada = if (plat.precisaOAuth) conectadas.contains(plat.name) else conectadas.contains(plat.name)
                Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Row(
                        Modifier.padding(14.dp).fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // dot da marca
                        Box(
                            Modifier.size(14.dp).background(
                                CorPlataforma[plat.name] ?: MaterialTheme.colorScheme.primary,
                                CircleShape
                            )
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(plat.rotulo, style = MaterialTheme.typography.titleSmall)
                            Text(
                                when {
                                    conectada -> "✓ conectada"
                                    plat.precisaOAuth -> when (plat) {
                                        Plataforma.SHOPEE -> "via WebView (Open API exige aprovação)"
                                        else -> "OAuth — precisa de chaves no local.properties"
                                    }
                                    else -> "login manual no WebView (sessão salva)"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        when {
                            conectada -> TextButton(onClick = {
                                escopo.launch {
                                    app.repositorio.apagarConta(plat.name)
                                    br.com.anunciaai.oauth.TokenStore.apagar(contexto, plat.name)
                                    br.com.anunciaai.oauth.EstadoConexao.emit(
                                        false, "${plat.rotulo} desconectada"
                                    )
                                }
                            }) { Text("Desconectar", color = MaterialTheme.colorScheme.error) }

                            plat == Plataforma.MERCADO_LIVRE -> Button(onClick = {
                                val clientId = CredenciaisML.clientId()
                                if (clientId == null) {
                                    br.com.anunciaai.oauth.EstadoConexao.emit(
                                        false,
                                        "Coloque ANUNCIAAI_ML_CLIENT_ID e ANUNCIAAI_ML_CLIENT_SECRET no local.properties e reconstrua o app."
                                    )
                                } else {
                                    contexto.startActivity(
                                        Intent(Intent.ACTION_VIEW, Uri.parse(MercadoLivreApi().urlLogin(clientId)))
                                    )
                                }
                            }) { Text("Conectar") }

                            else -> OutlinedButton(onClick = {
                                val script = br.com.anunciaai.publica.webview.ScriptsWeb.de(plat.name)
                                if (script != null) {
                                    // abre o WebView só pro usuário logar (sem pedido de publicação)
                                    contexto.startActivity(
                                        Intent(contexto, br.com.anunciaai.plataformas.webview.LoginWebViewActivity::class.java)
                                            .putExtra("somente_login", true)
                                            .putExtra("url", script.url)
                                    )
                                }
                            }) { Text("Logar") }
                        }
                    }
                }
            }
        }
    }
}
