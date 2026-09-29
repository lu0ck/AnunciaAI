package br.com.anunciaai.ia

import br.com.anunciaai.BuildConfig

/**
 * Fábrica do serviço de IA: escolhe o provedor pelas chaves disponíveis no BuildConfig
 * (local.properties). Sem chave, retorna null — a tela de revisão deixa o preenchimento manual.
 *
 * Ordem automática: NVIDIA NIM (testado 27/09: llama-3.2-11b-vision, 3.8s — RÁPIDO)
 *                   → GEMINI (só funciona com chave AIza... do AI Studio; chave "AQ." do Vertex NÃO serve)
 *                   → QWEN DashScope (pago).
 * Override opcional: ANUNCIAAI_AI_PROVIDER=gemini|nvidia|qwen no local.properties.
 *
 * NOTA: a API do DeepSeek é só texto (não aceita foto) — por isso não entra aqui.
 * O deepseek-vl deles é open-source para rodar em GPU própria, não há API de visão.
 */
object FabricaIA {

    fun criar(): ServicoDeIA? {
        val override = BuildConfig.AI_PROVIDER.takeIf { it.isNotBlank() }?.lowercase()

        fun chave(nome: String): String? = when (nome) {
            "gemini" -> BuildConfig.GEMINI_KEY.takeIf { it.isNotBlank() }
            "nvidia" -> BuildConfig.NVIDIA_KEY.takeIf { it.isNotBlank() }
            "qwen" -> BuildConfig.QWEN_API_KEY.takeIf { it.isNotBlank() }
            else -> null
        }

        val ordem = if (override != null && chave(override) != null) listOf(override)
        else listOf("nvidia", "gemini", "qwen")

        for (nome in ordem) {
            val k = chave(nome) ?: continue
            return when (nome) {
                "gemini" -> GeminiService(k)
                "nvidia" -> NvidiaVLService(k)
                else -> QwenVLService(k)
            }
        }
        return null
    }

    /** Nome do provedor que será usado (para mostrar na UI). */
    fun nomeAtivo(): String? {
        val override = BuildConfig.AI_PROVIDER.takeIf { it.isNotBlank() }?.lowercase()
        fun tem(nome: String): Boolean = when (nome) {
            "gemini" -> BuildConfig.GEMINI_KEY.isNotBlank()
            "nvidia" -> BuildConfig.NVIDIA_KEY.isNotBlank()
            "qwen" -> BuildConfig.QWEN_API_KEY.isNotBlank()
            else -> false
        }
        if (override != null && tem(override)) return override
        return listOf("nvidia", "gemini", "qwen").firstOrNull { tem(it) }
    }
}
