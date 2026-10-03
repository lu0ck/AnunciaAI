package br.com.anunciaai.ia

import br.com.anunciaai.ia.modelo.SugestaoIA
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

/**
 * Qwen-VL Plus (DashScope, modo OpenAI-compatible). Pago — só usado se a chave existir.
 * v2: multi-fotos (mesma chamada, um único anúncio).
 */
class QwenVLService(private val apiKey: String) : ServicoDeIA {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private val cliente = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .build()

    override suspend fun gerarAnuncio(fotoJpeg: ByteArray): Result<SugestaoIA> =
        gerarAnuncioMulti(listOf(fotoJpeg))

    override suspend fun gerarAnuncioMulti(fotosJpeg: List<ByteArray>): Result<SugestaoIA> =
        withContext(Dispatchers.IO) {
            try {
                if (fotosJpeg.isEmpty()) {
                    return@withContext Result.failure(Exception("Nenhuma foto para analisar"))
                }
                val payload = json.encodeToString(
                    kotlinx.serialization.json.JsonObject.serializer(),
                    buildJsonObject {
                        put("model", "qwen-vl-plus")
                        put("temperature", 0.3)
                        put("max_tokens", 800)
                        put("messages", buildJsonArray {
                            add(buildJsonObject {
                                put("role", "system")
                                put("content", buildJsonArray {
                                    add(buildJsonObject {
                                        put("type", "text")
                                        put("text", PROMPT_SISTEMA)
                                    })
                                })
                            })
                            add(buildJsonObject {
                                put("role", "user")
                                put("content", buildJsonArray {
                                    fotosJpeg.forEach { foto ->
                                        val b64 = android.util.Base64.encodeToString(
                                            foto, android.util.Base64.NO_WRAP
                                        )
                                        add(buildJsonObject {
                                            put("type", "image_url")
                                            put("image_url", buildJsonObject {
                                                put("url", "data:image/jpeg;base64,$b64")
                                            })
                                        })
                                    }
                                    add(buildJsonObject {
                                        put("type", "text")
                                        put("text", "Analise as fotos e gere o anúncio.")
                                    })
                                })
                            })
                        })
                    }
                )

                val req = Request.Builder()
                    .url("https://dashscope-intl.aliyuncs.com/compatible-mode/v1/chat/completions")
                    .addHeader("Authorization", "Bearer $apiKey")
                    .addHeader("Content-Type", "application/json")
                    .post(payload.toRequestBody("application/json".toMediaType()))
                    .build()

                cliente.newCall(req).execute().use { resp ->
                    val corpo = resp.body?.string() ?: ""
                    if (!resp.isSuccessful) {
                        return@withContext Result.failure(
                            Exception("IA HTTP ${resp.code}: ${corpo.take(300)}")
                        )
                    }
                    val texto = try {
                        val c = json.parseToJsonElement(corpo)
                            .jsonObject["choices"]?.jsonArray?.firstOrNull()
                            ?.jsonObject?.get("message")?.jsonObject?.get("content")
                        when (c) {
                            is JsonArray -> c.joinToString("") { p ->
                                runCatching { p.jsonObject["text"]?.jsonPrimitive?.content ?: "" }.getOrDefault("")
                            }
                            is kotlinx.serialization.json.JsonElement ->
                                runCatching { c.jsonPrimitive.content }.getOrDefault("")
                            else -> ""
                        }
                    } catch (e: Exception) { "" }
                    if (texto.isBlank()) {
                        Result.failure(Exception("IA respondeu vazio: ${corpo.take(200)}"))
                    } else {
                        Result.success(SugestaoIA.extrair(texto))
                    }
                }
            } catch (e: Exception) {
                Result.failure(Exception("Falha na IA: ${e.message}", e))
            }
        }

    companion object {
        val PROMPT_SISTEMA = """
            Você é um assistente de precificação e catalogação para revenda de produtos usados no Brasil.
            As imagens mostram o MESMO produto de vários ângulos/detalhes. Analise o CONJUNTO
            (se uma foto mostra defeito ou desgaste, isso deve refletir na descrição e no preço).
            Retorne APENAS um JSON válido, sem texto adicional, no formato:

            {
              "titulo": "string, até 60 caracteres, no estilo usado em anúncios de marketplace",
              "descricao": "string, 2 a 4 frases, destacando estado de conservação aparente e principais características, mencionando defeitos visíveis",
              "categoria_sugerida": "taxonomia EXATA com os níveis separados por ' > ', do departamento ao tipo específico (exemplo: 'Eletrônicos > Hardware > Periféricos > Mouses'). Use no máximo 4 níveis, baseada na taxonomia de marketplaces brasileiros (Mercado Livre/OLX).",
              "preco_sugerido": 0.00,
              "preco_comparativo_mercado": 0.00,
              "condicao": "novo"
            }

            REGRAS DA CONDIÇÃO — a chave "condicao" DEVE retornar EXCLUSIVAMENTE uma destas 4 strings, nada mais:
            - "novo" | "como novo" | "bom estado" | "marcas de uso"

            REGRAS ANTI-CONTRADIÇÃO:
            - NUNCA descreva o mesmo produto com atributos tecnicamente contraditórios
              (ex.: com fio E sem fio, novo E usado). Se não tiver certeza de um
              atributo específico, OMITA-o da descrição em vez de arriscar contradição.
            - A descrição deve ser coerente com a "condicao" escolhida.

            Baseie o preço sugerido em produtos semelhantes usados/seminovos no mercado brasileiro. Se não conseguir identificar o produto com confiança, retorne "titulo": "PRODUTO NÃO IDENTIFICADO" e os demais campos vazios.
        """.trimIndent()
    }
}
