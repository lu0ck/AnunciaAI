package br.com.anunciaai.publica.mercadolivre

import br.com.anunciaai.AnunciaAIApp
import br.com.anunciaai.publica.DadosAnuncio
import br.com.anunciaai.publica.PublicadorDePlataforma
import br.com.anunciaai.publica.ResultadoPublicacao
import br.com.anunciaai.dados.ContaConectada
import android.util.Log

/**
 * Publicador do Mercado Livre via API oficial.
 * Fluxo: token de ContaConectada (renova se expirado) → prevê categoria → POST /items.
 */
class MercadoLivrePublicador(private val app: AnunciaAIApp) : PublicadorDePlataforma {

    override val plataforma = "MERCADO_LIVRE"
    private val api = MercadoLivreApi()

    override suspend fun publicar(dados: DadosAnuncio): ResultadoPublicacao {
        val cred = br.com.anunciaai.oauth.CredenciaisML
        val clientId = cred.clientId()
            ?: return ResultadoPublicacao.Erro("ML: Client ID não configurado (edite local.properties e reconstrua)", false)

        val conta = app.repositorio.conta(plataforma)
            ?: return ResultadoPublicacao.Erro("ML: conta não conectada. Conecte em Conexões primeiro.", false)

        // Renova o token se falta menos de 10 min
        var token = conta.accessTokenCriptografado
        if ((conta.expiraEm ?: 0) - System.currentTimeMillis() < 600_000) {
            val refresh = conta.refreshTokenCriptografado
            if (refresh == null) {
                return ResultadoPublicacao.Erro("ML: sessão expirada. Reconecte a conta.", false)
            }
            val secret = cred.clientSecret()
                ?: return ResultadoPublicacao.Erro("ML: Client Secret não configurado", false)
            val novo = api.renovarToken(refresh, clientId, secret)
            if (novo == null) {
                return ResultadoPublicacao.Erro("ML: falha ao renovar token. Reconecte a conta.", false)
            }
            token = novo.accessToken
            app.repositorio.salvarConta(
                ContaConectada(
                    plataforma = plataforma,
                    accessTokenCriptografado = token,
                    refreshTokenCriptografado = novo.refreshToken,
                    expiraEm = novo.expiraEm
                )
            )
        }

        // Categoria: predictor do ML com o título
        val categoria = api.preverCategoria(token, dados.titulo)
        if (categoria == null) {
            return ResultadoPublicacao.Erro("ML: não achei categoria para o título", false)
        }

        // v2: sobe TODAS as fotos (limite ML: 10) e publica com as URLs do /pictures
        val urlsFotos = if (!dados.fotosJpeg.isNullOrEmpty()) {
            api.subirFotos(token, dados.fotosJpeg)
        } else emptyList()

        val (ok, resposta) = api.publicarItem(
            token = token,
            titulo = dados.titulo,
            descricao = dados.descricao,
            preco = dados.preco,
            categoriaId = categoria,
            condicao = "usado",
            urlsFotos = urlsFotos
        )
        return if (ok) {
            val (idMl, permalink) = resposta.split("|", limit = 2).let { it[0] to it[1] }
            ResultadoPublicacao.Sucesso(
                url = permalink.ifEmpty { null },
                idExterno = idMl.ifEmpty { null }
            )
        } else ResultadoPublicacao.Erro("ML: $resposta")
    }
}
