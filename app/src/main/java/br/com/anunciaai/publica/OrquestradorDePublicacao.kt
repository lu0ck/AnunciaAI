package br.com.anunciaai.publica

import br.com.anunciaai.AnunciaAIApp
import br.com.anunciaai.dados.PublicacaoPlataforma
import br.com.anunciaai.ui.Plataforma
import br.com.anunciaai.ui.foto.FotoUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext

/**
 * Orquestrador: para cada plataforma marcada, chama o publicador correspondente,
 * salva o status em cada uma no Room e devolve o resultado.
 */
class OrquestradorDePublicacao(private val app: AnunciaAIApp) {

    suspend fun publicar(itemId: Long, plataformas: List<Plataforma>): Map<String, ResultadoPublicacao> =
        withContext(Dispatchers.IO) {
            val item = app.repositorio.itemNow(itemId)
                ?: return@withContext mapOf("_" to ResultadoPublicacao.Erro("Item não encontrado", false))

            // v2: TODAS as fotos (em ordem) vão pra publicação
            val fotos = app.repositorio.fotosDoItemNow(itemId)
                .sortedBy { it.ordem }
                .mapNotNull { FotoUtil.lerBytes(app, it.uri) }
            val dados = DadosAnuncio.doItem(item, fotos)

            val resultados = coroutineScope {
                plataformas.map { plat ->
                    async {
                        val publicador = fabricar(plat)
                        val resultado = try {
                            publicador.publicar(dados)
                        } catch (e: Exception) {
                            ResultadoPublicacao.Erro("Falha: ${e.message ?: e.javaClass.simpleName}")
                        }
                        // persiste o status por plataforma
                        app.repositorio.apagarPublicacao(itemId, plat.name)
                        app.repositorio.salvarPublicacao(
                            PublicacaoPlataforma(
                                itemId = itemId,
                                plataforma = plat.name,
                                status = when (resultado) {
                                    is ResultadoPublicacao.Sucesso -> "PUBLICADO"
                                    is ResultadoPublicacao.Erro -> "ERRO"
                                },
                                urlAnuncio = (resultado as? ResultadoPublicacao.Sucesso)?.url,
                                idExterno = (resultado as? ResultadoPublicacao.Sucesso)?.idExterno,
                                mensagemErro = (resultado as? ResultadoPublicacao.Erro)?.mensagem,
                                dataPublicacao = if (resultado is ResultadoPublicacao.Sucesso) System.currentTimeMillis() else null
                            )
                        )
                        plat.name to resultado
                    }
                }.awaitAll().toMap()
            }

            if (item.precoFinal <= 0 && dados.preco > 0) {
                app.repositorio.salvarItem(item.copy(precoFinal = dados.preco))
            }
            resultados
        }

    /** Fábrica: cria o publicador da plataforma. Isolado por módulo.
     *  Plataformas sem API (OLX/FB/Enjoei/Shopee-v1) não passam por aqui —
     *  rodam via LoginWebViewActivity com scripts (PublicadoresWeb.kt). */
    private fun fabricar(plat: Plataforma): PublicadorDePlataforma = when (plat) {
        Plataforma.MERCADO_LIVRE -> br.com.anunciaai.publica.mercadolivre.MercadoLivrePublicador(app)
        Plataforma.EBAY -> br.com.anunciaai.publica.ebay.EbayPublicador(app)
        Plataforma.SHOPEE -> br.com.anunciaai.publica.shopee.ShopeePublicador()
        else -> throw IllegalArgumentException("$plat não tem API oficial na v1")
    }
}
