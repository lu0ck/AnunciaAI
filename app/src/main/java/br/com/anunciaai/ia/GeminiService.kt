package br.com.anunciaai.ia

import br.com.anunciaai.ia.modelo.SugestaoIA
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

/**
 * Gemini 2.5 Flash (Google AI Studio, chave AIza...). v2: multi-fotos.
 */
class GeminiService(
    private val apiKey: String,
    private val modelo: String = "gemini-2.5-flash"
) : ServicoDeIA {

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
                val partes = mutableListOf<Map<String, Any?>>()
                fotosJpeg.forEach { foto ->
                    val base64 = android.util.Base64.encodeToString(foto, android.util.Base64.NO_WRAP)
                    partes.add(mapOf("inline_data" to mapOf("mime_type" to "image/jpeg", "data" to base64)))
                }
                partes.add(mapOf("text" to NvidiaVLService.PROMPT_MULTI))

                val payload = json.encodeToString(
                    kotlinx.serialization.json.JsonObject.serializer(),
                    kotlinx.serialization.json.buildJsonObject {
                        put("contents", kotlinx.serialization.json.buildJsonArray {
                            add(kotlinx.serialization.json.buildJsonObject {
                                put("role", kotlinx.serialization.json.JsonPrimitive("user"))
                                put("parts", kotlinx.serialization.json.JsonArray(partes.map { p ->
                                    kotlinx.serialization.json.buildJsonObject {
                                        if (p.containsKey("inline_data")) {
                                            val d = p["inline_data"] as Map<*, *>
                                            put("inline_data", kotlinx.serialization.json.buildJsonObject {
                                                put("mime_type", kotlinx.serialization.json.JsonPrimitive(d["mime_type"].toString()))
                                                put("data", kotlinx.serialization.json.JsonPrimitive(d["data"].toString()))
                                            })
                                        } else {
                                            put("text", kotlinx.serialization.json.JsonPrimitive(p["text"].toString()))
                                        }
                                    }
                                }))
                            })
                        })
                        put("generationConfig", kotlinx.serialization.json.buildJsonObject {
                            put("temperature", kotlinx.serialization.json.JsonPrimitive(0.3))
                            put("maxOutputTokens", kotlinx.serialization.json.JsonPrimitive(800))
                        })
                    }
                )

                val req = Request.Builder()
                    .url("https://generativelanguage.googleapis.com/v1beta/models/$modelo:generateContent")
                    .addHeader("x-goog-api-key", apiKey)
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
                        json.parseToJsonElement(corpo)
                            .jsonObject["candidates"]?.jsonArray?.firstOrNull()
                            ?.jsonObject?.get("content")?.jsonObject
                            ?.get("parts")?.jsonArray
                            ?.joinToString("") { p ->
                                runCatching { p.jsonObject["text"]?.jsonPrimitive?.content ?: "" }
                                    .getOrDefault("")
                            } ?: ""
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
}
