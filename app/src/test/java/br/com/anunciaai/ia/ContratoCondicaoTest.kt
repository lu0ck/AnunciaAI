package br.com.anunciaai.ia

import br.com.anunciaai.ia.modelo.SugestaoIA
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * FASE 1 (spec v6) — contrato da resposta da IA:
 * 1. valores exatos atravessam íntegros até o chip;
 * 2. condição FORA das 4 (sinônimos, acentos, vazia) NÃO derruba a resposta —
 *    é normalizada/estimada e o restante chega inteiro;
 * 3. retry só quando falta o essencial (título/descrição/categoria/preço);
 * 4. falha persistente = erro com botões "Tentar de novo"/"Preencher manualmente".
 */
class ContratoCondicaoTest {

    /** Serviço falso que devolve sugestões em sequência. */
    private class IAfake(private val respostas: List<SugestaoIA>) : ServicoDeIA {
        var chamadas = 0
        override suspend fun gerarAnuncio(fotoJpeg: ByteArray) = gerarAnuncioMulti(listOf(fotoJpeg))
        override suspend fun gerarAnuncioMulti(fotosJpeg: List<ByteArray>): Result<SugestaoIA> {
            val r = respostas[chamadas.coerceAtMost(respostas.size - 1)]
            chamadas++
            return Result.success(r)
        }
    }

    private val completa = SugestaoIA(
        titulo = "Placa de vídeo RTX 3060",
        descricao = "Placa em bom funcionamento.",
        categoria_sugerida = "Eletrônicos > Hardware > Placas de vídeo",
        precoSugerido = 1200.0,
        condicao = "novo"
    )

    @Test
    fun `condicao exata novo atravessa integro ate o chip`() {
        val s = SugestaoIA(titulo = "Mouse Gamer Logitech G502", condicao = "novo")
        assertEquals("novo", s.condicaoNormalizada)
        assertTrue(s.condicaoNormalizada in SugestaoIA.CONDICOES)
        assertFalse(s.condicaoEstimada)
    }

    @Test
    fun `condicao vazia nao derruba a resposta — vira bom estado estimado`() = runBlocking {
        // caso real da placa de vídeo: IA devolve condicao ""
        val r = IAfake(listOf(completa.copy(condicao = ""))).gerarComContrato(listOf(ByteArray(8)))
        assertTrue(r.isSuccess)
        val s = r.getOrNull()!!
        assertEquals("bom estado", s.condicaoNormalizada)
        assertTrue(s.condicaoEstimada)
        assertEquals("Placa de vídeo RTX 3060", s.titulo) // resto intacto
    }

    @Test
    fun `sinonimos e acentos normalizam pro contrato`() {
        listOf(
            "usado - como novo" to "como novo",
            "seminovo" to "como novo",
            "quase novo" to "como novo",
            "Bom Estado" to "bom estado",
            "usado" to "bom estado",
            "usado - bom estado" to "bom estado",
            "com marcas de uso" to "marcas de uso",
            "desgaste" to "marcas de uso",
            "lacrado" to "novo",
            "nunca usado" to "novo",
            "marcas de uso" to "marcas de uso"
        ).forEach { (bruta, esperada) ->
            assertEquals(esperada, SugestaoIA.normalizarCondicao(bruta))
        }
    }

    @Test
    fun `condicao estranha completa a resposta sem retry`() = runBlocking {
        // condição irreconhecível + resto completo → aceita, 1 chamada só
        val ia = IAfake(listOf(completa.copy(condicao = "regularzinho")))
        val r = ia.gerarComContrato(listOf(ByteArray(8)))
        assertTrue(r.isSuccess)
        assertEquals("bom estado", r.getOrNull()!!.condicaoNormalizada) // estimado
        assertTrue(r.getOrNull()!!.condicaoEstimada)
        assertEquals("sem retry (resposta aproveitada)", 1, ia.chamadas)
    }

    @Test
    fun `retry quando falta o essencial e a segunda vem boa`() = runBlocking {
        // 1ª: sem preço (essencial faltando) → retry; 2ª: completa
        val ia = IAfake(
            listOf(
                completa.copy(precoSugerido = 0.0),
                completa
            )
        )
        val r = ia.gerarComContrato(listOf(ByteArray(8)))
        assertTrue(r.isSuccess)
        assertEquals("retry aconteceu (2 chamadas)", 2, ia.chamadas)
        assertEquals("novo", r.getOrNull()!!.condicaoNormalizada)
    }

    @Test
    fun `ia insiste sem o essencial e o contrato falha com mensagem clara`() = runBlocking {
        // sem título/preço → inutilizável nas 2 tentativas → erro
        val ia = IAfake(listOf(completa.copy(titulo = "", precoSugerido = 0.0)))
        val r = ia.gerarComContrato(listOf(ByteArray(8)))
        assertTrue(r.isFailure)
        assertTrue(r.exceptionOrNull()!!.message!!.contains("essencial"))
        assertEquals("2 tentativas esgotadas", 2, ia.chamadas)
    }

    @Test
    fun `normalizador legado continua valido`() {
        listOf(
            "novo" to "novo",
            "como novo" to "como novo",
            "seminovo" to "como novo",
            "" to "bom estado"
        ).forEach { (bruta, esperada) ->
            assertEquals(esperada, SugestaoIA.normalizarCondicao(bruta))
        }
    }
}
