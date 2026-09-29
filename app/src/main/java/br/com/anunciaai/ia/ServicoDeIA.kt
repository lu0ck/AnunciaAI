package br.com.anunciaai.ia

import br.com.anunciaai.ia.modelo.SugestaoIA

/**
 * Contrato do serviço de IA. Isolado numa interface para trocar de provedor
 * sem reescrever o resto do app. v2: multi-fotos.
 */
interface ServicoDeIA {
    /** Compatibilidade v1: uma foto. */
    suspend fun gerarAnuncio(fotoJpeg: ByteArray): Result<SugestaoIA>

    /** v2: todas as fotos do item na mesma chamada → um único título/descrição/preço. */
    suspend fun gerarAnuncioMulti(fotosJpeg: List<ByteArray>): Result<SugestaoIA>
}
