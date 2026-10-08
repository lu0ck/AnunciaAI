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
 * v11.1 (fix #3) → FASE 1 (spec v6): wrapper com RETRY só para defeitos REAIS da
 * resposta: JSON quebrado ou faltando título/descrição/categoria/preço. A
 * condição fora das 4 strings NÃO derruba mais a resposta — é normalizada
 * (sinônimos + acentos) e, se vier vazia/irreconhecível, vira "bom estado"
 * com `condicaoEstimada = true` (a UI avisa "condição estimada, confira").
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
            // FASE 1: só é resposta inutilizável se faltar o ESSENCIAL.
            // Condição vazia/estranha = parcial aceitável, não erro — a
            // resposta volta CRUA (o getter condicaoNormalizada resolve, e
            // condicaoEstimada preserva o sinal pro aviso na UI).
            val tituloUtilizavel = s.titulo.isNotBlank() &&
                !"NÃO IDENTIFICADO".let { s.titulo.uppercase().contains(it) }
            val essencialOk = tituloUtilizavel &&
                s.descricao.isNotBlank() &&
                s.categoria_sugerida.isNotBlank() &&
                s.melhorPreco > 0
            if (essencialOk) return Result.success(s)
            ultimoErro = Exception(
                "campo essencial faltando (titulo=${tituloUtilizavel}, descricao=${s.descricao.isNotBlank()}, categoria=${s.categoria_sugerida.isNotBlank()}, preco=${s.melhorPreco})"
            )
        }.onFailure { ultimoErro = it as? Exception ?: Exception(it.message) }
    }
    return Result.failure(ultimoErro ?: Exception("IA não devolveu contrato válido"))
}
