package br.com.anunciaai.ia

import br.com.anunciaai.ia.modelo.SugestaoIA

/**
 * Contrato do serviço de IA. Isolado numa interface para trocar de provedor
 * sem reesc[rever o resto do app. v2: multi-fotos. v8 (PILAR 4): com EAN.
 */
interface ServicoDeIA {
    /** Compatibilidade v1: uma foto. */
    suspend fun gerarAnuncio(fotoJpeg: ByteArray): Result<SugestaoIA>

    /** v2: todas as fotos do item na mesma chamada → um único título/descrição/preço. */
    suspend fun gerarAnuncioMulti(fotosJpeg: List<ByteArray>): Result<SugestaoIA>

    /**
     * PILAR 4: fotos + código de barras EAN lido pelo ML Kit. A IA recebe o
     * código exato do produto (nunca confunde o modelo) e devolve o anúncio
     * com modelo EXATO e preço de mercado.
     */
    suspend fun gerarAnuncioComEan(
        fotosJpeg: List<ByteArray>,
        codigoEan: String
    ): Result<SugestaoIA> = gerarAnuncioMulti(fotosJpeg) // padrão: ignora o EAN
}

/**
 * v11.1 (fix #3): wrapper com RETRY quando a IA devolve a "condicao" fora das
 * 4 strings exatas ("novo"/"como novo"/"bom estado"/"marcas de uso"). Qualquer
 * resposta fora do contrato é tratada como erro de parsing e a chamada é
 * re-tentada (1 vez a mais) com instrução de correção antes de desistir.
 */
suspend fun ServicoDeIA.gerarComContrato(
    fotos: List<ByteArray>,
    ean: String? = null,
    tentativas: Int = 2
): Result<SugestaoIA> {
    var ultimoErro: Exception? = null
    repeat(tentativas) { indice ->
        val r = if (ean.isNullOrBlank()) gerarAnuncioMulti(fotos) else gerarAnuncioComEan(fotos, ean)
        r.onSuccess { s ->
            if (s.condicao in SugestaoIA.CONDICOES) return Result.success(s)
            // fora do contrato → erro de parsing, re-tenta
            ultimoErro = Exception(
                "condicao \"${s.condicao}\" fora do contrato (esperado: ${SugestaoIA.CONDICOES})"
            )
        }.onFailure { ultimoErro = it as? Exception ?: Exception(it.message) }
    }
    return Result.failure(ultimoErro ?: Exception("IA não devolveu contrato válido"))
}
