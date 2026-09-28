package br.com.anunciaai.ia.modelo

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Modelo da resposta da IA (v2: aceita multi-fotos). */
@Serializable
data class SugestaoIA(
    val titulo: String = "",
    val descricao: String = "",
    val categoria_sugerida: String = "",
    @kotlinx.serialization.SerialName("preco_sugerido_reais") val precoSugeridoReais: Double = 0.0,
    val condicao: String = ""
) {
    companion object {
        fun naoIdentificado() = SugestaoIA(titulo = "PRODUTO NÃO IDENTIFICADO")

        /**
         * Extrai o JSON da resposta da IA. Tolerante a texto ao redor (fence ```json`,
         * frases antes/depois).
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
    }
}

/** Aceita preço vindo como número (12.5) ou como string ("12,50"). */
object DoubleAdapter : kotlinx.serialization.KSerializer<Double> {
    override val descriptor = kotlinx.serialization.descriptors.PrimitiveSerialDescriptor(
        "PrecoFlexivel", kotlinx.serialization.descriptors.PrimitiveKind.STRING
    )

    override fun deserialize(decoder: kotlinx.serialization.encoding.Decoder): Double {
        val jd = decoder as? kotlinx.serialization.json.JsonDecoder
        return if (jd != null) {
            when (val el = jd.decodeJsonElement()) {
                is kotlinx.serialization.json.JsonPrimitive ->
                    el.content.trim().replace(",", ".").toDoubleOrNull() ?: 0.0
                else -> 0.0
            }
        } else {
            decoder.decodeDouble()
        }
    }

    override fun serialize(encoder: kotlinx.serialization.encoding.Encoder, value: Double) {
        encoder.encodeString(value.toString())
    }
}
