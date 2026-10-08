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

    /**
     * FASE 1 (spec v6): true quando a IA devolveu condição VAZIA ou irreconhecível
     * e o app está usando o padrão "bom estado" — a UI mostra "condição estimada,
     * confira". Nunca descarta a resposta por causa disso.
     */
    val condicaoEstimada: Boolean
        get() {
            if (condicao.isBlank()) return true
            if (condicaoNormalizada != "bom estado") return false
            // caiu em "bom estado" sem reconhecer nada → foi estimado
            val c = semAcentos(condicao)
            return "bom" !in c && c !in CONDICOES && c != "usado"
        }

    companion object {
        val CONDICOES = listOf("novo", "como novo", "bom estado", "marcas de uso")

        /** minúsculas + sem acentos (FASE 1: "Bom Estado"/"CONDIÇÃO" caem no contrato). */
        private fun semAcentos(s: String) = java.text.Normalizer
            .normalize(s.trim().lowercase(), java.text.Normalizer.Form.NFD)
            .replace(Regex("\\p{Mn}+"), "")

        /** Converte qualquer variação da IA pra um dos 4 valores exatos (FASE 1: sinônimos da spec). */
        fun normalizarCondicao(bruta: String): String {
            val c = semAcentos(bruta)
            return when {
                c.isEmpty() -> "bom estado"
                "nao identificado" in c -> "bom estado"
                "lacrado" in c || "nunca usado" in c || "nao usado" in c -> "novo"
                // "novo" exato/derivados, mas não "como novo"/"seminovo"/"quase novo"
                c == "novo" || c.startsWith("novo -") || c.startsWith("novo,") -> "novo"
                "como novo" in c || "seminovo" in c || "quase novo" in c || "praticamente novo" in c -> "como novo"
                "marcas" in c || "desgast" in c || "defeito" in c || "ruim" in c -> "marcas de uso"
                c == "usado" -> "bom estado"
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
