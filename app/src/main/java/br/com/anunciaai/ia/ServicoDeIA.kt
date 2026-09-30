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
