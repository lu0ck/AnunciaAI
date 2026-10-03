package br.com.anunciaai.ia

import br.com.anunciaai.ia.modelo.SugestaoIA
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * v11.1 — testes do contrato da condição (fixes #2/#3):
 * 1. valores exatos da IA passam íntegros até o chip (novo → "novo");
 * 2. resposta fora das 4 strings → re-tentativa devolve contrato válido;
 * 3. persistência falha graciosamente quando a IA insiste no erro.
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

    @Test
    fun `condicao exata novo atravessa integro ate o chip`() {
        // IA devolve "novo" → normalização preserva "novo" (chip certo marcado)
        val s = SugestaoIA(titulo = "Mouse Gamer Logitech G502", condicao = "novo")
        assertEquals("novo", s.condicaoNormalizada)
        assertTrue(s.condicaoNormalizada in SugestaoIA.CONDICOES)
    }

    @Test
    fun `ia responde fora do contrato e o retry devolve valor valido`() = runBlocking {
        val ia = IAfake(
            listOf(
                SugestaoIA(titulo = "Mouse sem fio", condicao = "praticamente novo"), // inválida
                SugestaoIA(titulo = "Mouse sem fio", condicao = "como novo")          // válida
            )
        )
        val r = ia.gerarComContrato(listOf(ByteArray(8)))
        assertTrue(r.isSuccess)
        assertEquals("como novo", r.getOrNull()!!.condicaoNormalizada)
        assertEquals("retry aconteceu (2 chamadas)", 2, ia.chamadas)
    }

    @Test
    fun `ia insiste no erro e o contrato falha com mensagem clara`() = runBlocking {
        val ia = IAfake(listOf(SugestaoIA(titulo = "Item", condicao = "regularzinho")))
        val r = ia.gerarComContrato(listOf(ByteArray(8)))
        assertTrue(r.isFailure)
        assertTrue(r.exceptionOrNull()!!.message!!.contains("fora do contrato"))
    }

    @Test
    fun `normalizador tolera variacoes legadas sem quebrar o contrato`() {
        // respostas antigas/variadas da IA ainda caem num dos 4 valores
        listOf(
            "novo" to "novo",
            "como novo" to "como novo",
            "seminovo" to "como novo",
            "usado - bom estado" to "bom estado",
            "com marcas de uso" to "marcas de uso",
            "" to "bom estado"
        ).forEach { (bruta, esperada) ->
            assertEquals(esperada, SugestaoIA.normalizarCondicao(bruta))
        }
    }
}
