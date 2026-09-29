package br.com.anunciaai.publica

import br.com.anunciaai.dados.Item

/** Dados prontos para publicar em qualquer plataforma. */
data class DadosAnuncio(
    val item: Item,
    val titulo: String,
    val descricao: String,
    val preco: Double,
    val fotosJpeg: List<ByteArray>?
) {
    companion object {
        fun doItem(item: Item, fotosJpeg: List<ByteArray>? = null) = DadosAnuncio(
            item = item,
            titulo = item.titulo,
            descricao = item.descricao,
            preco = item.precoFinal.takeIf { it > 0 } ?: item.precoSugerido,
            fotosJpeg = fotosJpeg
        )
    }
}

/** Resultado da publicação em uma plataforma. */
sealed class ResultadoPublicacao {
    data class Sucesso(val url: String?, val idExterno: String? = null) : ResultadoPublicacao()
    data class Erro(val mensagem: String, val recuperavel: Boolean = true) : ResultadoPublicacao()
}

/**
 * Contrato comum de publicador. Cada plataforma implementa o seu módulo isolado —
 * adicionar/remover uma plataforma não afeta as outras.
 */
interface PublicadorDePlataforma {
    val plataforma: String

    /** Publica o anúncio. Deve ser chamado em coroutine (função suspend). */
    suspend fun publicar(dados: DadosAnuncio): ResultadoPublicacao
}
