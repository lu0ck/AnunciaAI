package br.com.anunciaai.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.KeyOff
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.unit.dp
import br.com.anunciaai.AnunciaAIApp
import br.com.anunciaai.oauth.CredenciaisML
import br.com.anunciaai.oauth.TokenStore
import br.com.anunciaai.ui.IconePlataforma
import br.com.anunciaai.ui.Plataforma
import br.com.anunciaai.ui.theme.CorTexto
import br.com.anunciaai.ui.theme.CorTextoSec
import br.com.anunciaai.ui.theme.Destaque
import kotlinx.coroutines.launch

/**
 * TELA 4 — CONEXÕES (v12.0, reescrita do zero).
 * Lista de linhas com separador 1px (sem card/sombra — lista simples),
 * badge quadrado na cor real da marca, status por TEXTO ("Conectado" em
 * verde-destaque / "Não conectado" em cinza). Card da IA no topo (aprovado).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConexoesScreen(embutida: Boolean = false) {
    val contexto = LocalContext.current
    val app = contexto.applicationContext as AnunciaAIApp
    val escopo = rememberCoroutineScope()
    val contas by app.repositorio.contas().collectAsState(initial = emptyList())
    val msg by br.com.anunciaai.oauth.EstadoConexao.msg.collectAsState()

    val conectadas = contas.map { it.plataforma }.toSet()
    var menuPlat by remember { mutableStateOf<String?>(null) }

    val conteudo: @Composable () -> Unit = {
        Column(
            Modifier.padding(horizontal = 16.dp).then(
                if (embutida) Modifier else Modifier.verticalScroll(rememberScrollState())
            )
        ) {
            msg?.let {
                Text(
                    it.texto,
                    Modifier.padding(vertical = 8.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (it.ok) Destaque else MaterialTheme.colorScheme.error
                )
            }

            // ── card da IA (estilo aprovado, única superfície elevada da tela) ──
            val temIA = br.com.anunciaai.ia.FabricaIA.nomeAtivo() != null
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceContainer,
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
            ) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.AutoAwesome, contentDescription = null,
                        tint = if (temIA) Destaque else CorTextoSec
                    )
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            "Inteligência artificial",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = CorTexto
                        )
                        Text(
                            if (temIA) "Ativa — a descrição sai pronta quando você fotografa"
                            else "Sem chave neste build — preenchimento manual",
                            style = MaterialTheme.typography.bodySmall,
                            color = CorTextoSec
                        )
                    }
                }
            }

            Text(
                "Conecte uma vez; a sessão fica salva no seu celular.",
                style = MaterialTheme.typography.bodySmall,
                color = CorTextoSec,
                modifier = Modifier.padding(vertical = 4.dp)
            )

            // ── estado das credenciais OAuth do ML (lidas do BuildConfig) ──
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
                        tint = if (temChavesML) Destaque else CorTextoSec
                    )
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            if (temChavesML) "Credenciais do Mercado Livre prontas"
                            else "Faltam as credenciais do Mercado Livre",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = CorTexto
                        )
                        Text(
                            if (temChavesML)
                                "OAuth configurado — toque em Conectar no Mercado Livre abaixo."
                            else "Crie o app no DevCenter (developers.mercadolivre.com.br), copie " +
                                "ANUNCIAAI_ML_CLIENT_ID e ANUNCIAAI_ML_CLIENT_SECRET pro local.properties e reconstrua.",
                            style = MaterialTheme.typography.bodySmall,
                            color = CorTextoSec
                        )
                    }
                }
            }

            // ── lista: separador 1px, badge marca, status por texto ──
            Plataforma.entries.forEach { plat ->
                val conectada = conectadas.contains(plat.name)
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconePlataforma(plat.name, conectada = conectada)
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.padding(start = 0.dp).weight(1f)) {
                        Text(
                            plat.rotulo,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = CorTexto
                        )
                        Text(
                            when {
                                conectada -> "Conectado"
                                plat == Plataforma.SHOPEE -> "Não conectado — via WebView"
                                else -> "Não conectado"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = if (conectada) Destaque else CorTextoSec
                        )
                    }
                    when {
                        conectada -> {
                            Box {
                                IconButton(onClick = { menuPlat = plat.name }) {
                                    Icon(Icons.Default.MoreVert, contentDescription = "Opções", tint = CorTextoSec)
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
                                                TokenStore.apagar(contexto, plat.name)
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
                                    Intent(Intent.ACTION_VIEW, Uri.parse(br.com.anunciaai.publica.mercadolivre.MercadoLivreApi().urlLogin(clientId)))
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

    if (embutida) {
        conteudo()
    } else {
        androidx.compose.material3.Scaffold(
            containerColor = br.com.anunciaai.ui.theme.CorFundo,
            topBar = {
                TopAppBar(
                    title = { Text("Conexões", style = MaterialTheme.typography.headlineSmall, color = CorTexto) }
                )
            }
        ) { pad ->
            Box(Modifier.padding(pad)) { conteudo() }
        }
    }
}
