package br.com.anunciaai.publica.webview

import br.com.anunciaai.AnunciaAIApp
import br.com.anunciaai.dados.Item
import br.com.anunciaai.dados.PublicacaoPlataforma
import br.com.anunciaai.ui.Plataforma
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

/**
 * Sessão de publicação via WebView: fila de plataformas a publicar, pedido atual
 * que a LoginWebViewActivity executa, e resultado devolvido via StateFlow.
 * A persistência no Room (status PENDENTE/PUBLICADO/ERRO) acontece aqui.
 */
object SessaoPublicacaoWeb {

    data class Pedido(
        val itemId: Long,
        val plataforma: String,
        val url: String,
        val js: String,
        val titulo: String,
        val preco: String,
        val descricao: String
    )

    data class ResultadoWeb(val plataforma: String, val ok: Boolean, val msg: String)

    val resultado = MutableStateFlow<ResultadoWeb?>(null)
    private val fila = ArrayDeque<Pedido>()
    private val escopo = CoroutineScope(Dispatchers.IO)

    var atual: Pedido? = null
        private set

    val temMais: Boolean get() = fila.isNotEmpty()

    /** Enfileira as plataformas WebView marcadas e marca PENDENTE no Room. */
    fun enfileirar(app: AnunciaAIApp, itemId: Long, plataformas: List<Plataforma>, item: Item) {
        fila.clear()
        for (p in plataformas) {
            val script = ScriptsWeb.de(p.name) ?: continue
            val precoFmt = "%.2f".format(item.precoFinal.takeIf { it > 0 } ?: item.precoSugerido)
            fila.add(
                Pedido(
                    itemId = itemId,
                    plataforma = p.name,
                    url = script.url,
                    js = script.js + ScriptsWeb.JsPublicar,
                    titulo = item.titulo,
                    preco = precoFmt,
                    descricao = item.descricao
                )
            )
            escopo.launch {
                app.repositorio.apagarPublicacao(itemId, p.name)
                app.repositorio.salvarPublicacao(
                    PublicacaoPlataforma(itemId = itemId, plataforma = p.name, status = "PENDENTE")
                )
            }
        }
    }

    /** Devolve o próximo pedido da fila (e o define como atual). */
    fun proximo(): Pedido? {
        atual = fila.removeFirstOrNull()
        return atual
    }

    /** A activity reporta o resultado do pedido atual. Persiste e notifica. */
    fun reportar(app: AnunciaAIApp, ok: Boolean, msg: String) {
        val p = atual ?: return
        escopo.launch {
            app.repositorio.apagarPublicacao(p.itemId, p.plataforma)
            app.repositorio.salvarPublicacao(
                PublicacaoPlataforma(
                    itemId = p.itemId,
                    plataforma = p.plataforma,
                    status = if (ok) "PUBLICADO" else "ERRO",
                    mensagemErro = if (ok) null else msg
                )
            )
        }
        resultado.value = ResultadoWeb(p.plataforma, ok, if (ok) "Publicado" else msg)
        atual = null
    }

    /** Consome o resultado (para não reprocessar em novo collect). */
    fun consumir() {
        resultado.value = null
    }
}
