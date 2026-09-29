package br.com.anunciaai.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.KeyOff
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import br.com.anunciaai.AnunciaAIApp
import br.com.anunciaai.publica.mercadolivre.MercadoLivreApi
import br.com.anunciaai.oauth.CredenciaisML
import br.com.anunciaai.ui.IconePlataforma
import br.com.anunciaai.ui.Plataforma
import br.com.anunciaai.ui.theme.Destaque
import kotlinx.coroutines.launch

/**
 * Conexões (spec v3): lista de linhas separadas por traço fino de 1px — sem cards
 * flutuando, sem bolinhas decorativas. Estado comunica por TEXTO; a cor da marca
 * só aparece no monograma da plataforma CONECTADA. Só o cartão da IA usa superfície elevada.
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
    var menuPlat by remember { mutableStateOf<String?>(null) }

    Scaffold(topBar = { TopAppBar(title = { Text("Conexões") }) }) { pad ->
        Column(
            Modifier.padding(pad).padding(horizontal = 16.dp).verticalScroll(rememberScrollState())
        ) {
            msg?.let {
                Text(
                    it.texto,
                    Modifier.padding(vertical = 8.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (it.ok) Destaque else MaterialTheme.colorScheme.error
                )
            }

            // ÚNICA superfície elevada da tela: cartão da IA
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceContainer,
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
            ) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.AutoAwesome, contentDescription = null,
                        tint = if (br.com.anunciaai.ia.FabricaIA.nomeAtivo() != null) Destaque
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("Inteligência artificial", style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold)
                        Text(
                            if (br.com.anunciaai.ia.FabricaIA.nomeAtivo() != null)
                                "Ativa — a descrição sai pronta quando você fotografa"
                            else "Sem chave neste build — preenchimento manual",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Text(
                "Conecte uma vez; a sessão fica salva no seu celular.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 4.dp)
            )

            // v5.4 (PASSO 2): estado das credenciais OAuth do ML — interface clara
            // do que falta pra publicação via API funcionar (lidas do local.properties
            // via BuildConfig; nada de segredo em runtime)
            val temChavesML = CredenciaisML.clientId() != null && CredenciaisML.clientSecret() != null
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
            ) {
                Row(
                    Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        if (temChavesML) Icons.Default.Key else Icons.Default.KeyOff,
                        contentDescription = null,
                        tint = if (temChavesML) Destaque else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            if (temChavesML) "Credenciais do Mercado Livre prontas"
                            else "Faltam as credenciais do Mercado Livre",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            if (temChavesML)
                                "OAuth configurado — toque em Conectar no Mercado Livre abaixo."
                            else "Crie o app no DevCenter (developers.mercadolivre.com.br), copie " +
                                "ANUNCIAAI_ML_CLIENT_ID e ANUNCIAAI_ML_CLIENT_SECRET pro local.properties e reconstrua.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Linhas de lista separadas por traço fino de 1px na cor da superfície
            Plataforma.entries.forEachIndexed { idx, plat ->
                val conectada = conectadas.contains(plat.name)
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconePlataforma(plat.name, conectada = conectada)
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(plat.rotulo, style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold)
                        Text(
                            when {
                                conectada -> "Conectado"
                                plat.precisaOAuth && plat == Plataforma.SHOPEE -> "Não conectado — via WebView"
                                else -> "Não conectado"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = if (conectada) Destaque else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    when {
                        conectada -> {
                            Box {
                                IconButton(onClick = { menuPlat = plat.name }) {
                                    Icon(Icons.Default.MoreVert, contentDescription = "Opções")
                                }
                                DropdownMenu(
                                    expanded = menuPlat == plat.name,
                                    onDismissRequest = { menuPlat = null }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Desconectar") },
                                        onClick = {
                                            menuPlat = null
                                            escopo.launch {
                                                app.repositorio.apagarConta(plat.name)
                                                br.com.anunciaai.oauth.TokenStore.apagar(contexto, plat.name)
                                                br.com.anunciaai.oauth.EstadoConexao.emit(
                                                    false, "${plat.rotulo} desconectada"
                                                )
                                            }
                                        }
                                    )
                                }
                            }
                        }
                        plat == Plataforma.MERCADO_LIVRE -> TextButton(onClick = {
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
                        }) { Text("Conectar", color = Destaque) }
                        else -> TextButton(onClick = {
                            val script = br.com.anunciaai.publica.webview.ScriptsWeb.de(plat.name)
                            if (script != null) {
                                contexto.startActivity(
                                    Intent(contexto, br.com.anunciaai.plataformas.webview.LoginWebViewActivity::class.java)
                                        .putExtra("somente_login", true)
                                        .putExtra("url", script.url)
                                )
                            }
                        }) { Text("Conectar", color = Destaque) }
                    }
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(Modifier.height(24.dp))
        }
    }
}
