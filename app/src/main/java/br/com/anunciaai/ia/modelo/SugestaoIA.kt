package br.com.anunciaai.ia.modelo

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Modelo da resposta da IA (v7: preco_comparativo_mercado + condição normalizada). */
@Serializable
data class SugestaoIA(
    val titulo: String = "",
    val descricao: String = "",
    val categoria_sugerida: String = "",
    @kotlinx.serialization.SerialName("preco_sugerido_reais") val precoSugeridoReais: Double = 0.0,
    @kotlinx.serialization.SerialName("preco_sugerido") val precoSugerido: Double = 0.0,
    @kotlinx.serialization.SerialName("preco_comparativo_mercado") val precoComparativoMercado: Double = 0.0,
    val condicao: String = ""
) {
    /** Melhor preço sugerido disponível (novo ou legado). */
    val melhorPreco: Double get() = when {
        precoSugerido > 0 -> precoSugerido
        precoSugeridoReais > 0 -> precoSugeridoReais
        else -> 0.0
    }

    /** Condição NORMALIZADA — sempre uma das 4 strings exatas dos chips. */
    val condicaoNormalizada: String
        get() = normalizarCondicao(condicao)

    companion object {
        val CONDICOES = listOf("novo", "como novo", "bom estado", "marcas de uso")

        /** Converte qualquer variação da IA pra um dos 4 valores exatos. */
        fun normalizarCondicao(bruta: String): String {
            val c = bruta.trim().lowercase()
            return when {
                c.isEmpty() -> "bom estado"
                "não identificado" in c -> "bom estado"
                // "novo" exato, mas não "como novo" nem "seminovo"
                c == "novo" || c.startsWith("novo -") || c.startsWith("novo,") -> "novo"
                "como novo" in c || "seminovo" in c -> "como novo"
                "marcas" in c || "desgast" in c || "defeito" in c || "ruim" in c -> "marcas de uso"
                "bom" in c -> "bom estado"
                else -> CONDICOES.firstOrNull { it == c } ?: "bom estado"
            }
        }

        /**
         * Extrai o JSON da resposta da IA. Tolerante a texto ao redor (fence ```json,
         * frases antes/depois) e a chaves legadas (preco_sugerido_reais).
         */
        fun extrair(texto: String): SugestaoIA {
            var t = texto.trim()
            Regex("```(?:json)?\\s*([\\s\\S]*?)```").find(t)?.let { t = it.groupValues[1].trim() }
            val i0 = t.indexOf('{')
            val i1 = t.lastIndexOf('}')
            if (i0 >= 0 && i1 > i0) t = t.substring(i0, i1 + 1)
            return try {
                val s = Json { ignoreUnknownKeys = true; isLenient = true }
                    .decodeFromString(serializer(), t)
                if (s.titulo.isBlank() || s.titulo.uppercase().contains("NÃO IDENTIFICADO"))
                    naoIdentificado() else s
            } catch (e: Exception) {
                naoIdentificado()
            }
        }

        fun naoIdentificado() = SugestaoIA(titulo = "PRODUTO NÃO IDENTIFICADO")
    }
}
