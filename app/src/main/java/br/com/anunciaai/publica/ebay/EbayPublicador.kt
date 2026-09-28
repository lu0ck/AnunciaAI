package br.com.anunciaai.publica.ebay

import br.com.anunciaai.AnunciaAIApp
import br.com.anunciaai.publica.DadosAnuncio
import br.com.anunciaai.publica.PublicadorDePlataforma
import br.com.anunciaai.publica.ResultadoPublicacao

/**
 * Publicador eBay via Sell API (Inventory API).
 * Igual ao ML: token de ContaConectada, publicação direta sem navegador.
 * Na v1, token expirado pede reconexão manual (refresh token do eBay exige reauth completa).
 */
class EbayPublicador(private val app: AnunciaAIApp) : PublicadorDePlataforma {

    override val plataforma = "EBAY"

    override suspend fun publicar(dados: DadosAnuncio): ResultadoPublicacao {
        val clientId = br.com.anunciaai.oauth.CredenciaisEBAY.clientId()
            ?: return ResultadoPublicacao.Erro("eBay: Client ID não configurado (local.properties + rebuild)", false)

        val conta = app.repositorio.conta(plataforma)
            ?: return ResultadoPublicacao.Erro("eBay: conta não conectada. Conecte em Conexões primeiro.", false)

        if ((conta.expiraEm ?: 0) - System.currentTimeMillis() < 600_000) {
            return ResultadoPublicacao.Erro("eBay: sessão expirada. Reconecte a conta.", false)
        }
        val token = conta.accessTokenCriptografado

        val condicao = if (dados.descricao.contains("usado", ignoreCase = true)) "usado - bom estado"
        else "novo"

        val sku = "anunciaai-${dados.item.id}"
        val (ok, resposta) = EbayApi().publicarItem(
            token = token,
            titulo = dados.titulo,
            descricao = dados.descricao,
            preco = dados.preco / 5.0, // BRL→USD aproximado (taxa fixa na v1)
            condicao = condicao,
            sku = sku
        )
        return if (ok) ResultadoPublicacao.Sucesso(null)
        else ResultadoPublicacao.Erro("eBay: $resposta")
    }
}
