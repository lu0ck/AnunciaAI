package br.com.anunciaai.publica.mercadolivre

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
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

/** Helpers de JSON compartilhados do módulo ML. */
internal fun jsonStr(s: String): String = kotlinx.serialization.json.JsonPrimitive(s).toString()

/**
 * Cliente da API do Mercado Livre (OAuth 2.0 + REST).
 * Docs: https://developers.mercadolivre.com.br/
 */
class MercadoLivreApi {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private val cliente = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    // ---- OAuth ----

    fun urlLogin(clientId: String): String =
        "https://auth.mercadolivre.com.br/authorization?response_type=code" +
            "&client_id=$clientId&redirect_uri=$REDIRECT_URI"

    /** Troca o code da autorização por access_token + refresh_token. */
    suspend fun trocarCodePorToken(code: String, clientId: String, clientSecret: String): TokenML? =
        withContext(Dispatchers.IO) {
            val corpo = okhttp3.FormBody.Builder()
                .add("grant_type", "authorization_code")
                .add("client_id", clientId)
                .add("client_secret", clientSecret)
                .add("code", code)
                .add("redirect_uri", REDIRECT_URI)
                .build()
            val req = Request.Builder()
                .url("https://api.mercadolibre.com/oauth/token")
                .post(corpo)
                .build()
            cliente.newCall(req).execute().use { resp ->
                val texto = resp.body?.string() ?: return@use null
                if (!resp.isSuccessful) return@use null
                try {
                    val obj = json.parseToJsonElement(texto).jsonObject
                    TokenML(
                        accessToken = obj["access_token"]!!.jsonPrimitive.content,
                        refreshToken = obj["refresh_token"]?.jsonPrimitive?.content,
                        expiraEm = System.currentTimeMillis() +
                            (obj["expires_in"]?.jsonPrimitive?.content?.toLongOrNull() ?: 21600L) * 1000,
                        userId = obj["user_id"]?.jsonPrimitive?.content
                    )
                } catch (e: Exception) { null }
            }
        }

    /** Renova o token quando perto de expirar. */
    suspend fun renovarToken(refreshToken: String, clientId: String, clientSecret: String): TokenML? =
        withContext(Dispatchers.IO) {
            val corpo = okhttp3.FormBody.Builder()
                .add("grant_type", "refresh_token")
                .add("client_id", clientId)
                .add("client_secret", clientSecret)
                .add("refresh_token", refreshToken)
                .build()
            val req = Request.Builder()
                .url("https://api.mercadolibre.com/oauth/token")
                .post(corpo)
                .build()
            cliente.newCall(req).execute().use { resp ->
                val texto = resp.body?.string() ?: return@use null
                if (!resp.isSuccessful) return@use null
                try {
                    val obj = json.parseToJsonElement(texto).jsonObject
                    TokenML(
                        accessToken = obj["access_token"]!!.jsonPrimitive.content,
                        refreshToken = obj["refresh_token"]?.jsonPrimitive?.content,
                        expiraEm = System.currentTimeMillis() +
                            (obj["expires_in"]?.jsonPrimitive?.content?.toLongOrNull() ?: 21600L) * 1000,
                        userId = obj["user_id"]?.jsonPrimitive?.content
                    )
                } catch (e: Exception) { null }
            }
        }

    // ---- Fotos: upload para o /pictures do ML (base64) ----

    /**
     * Sobe uma foto (JPEG) pro ML e devolve as URLs (secure_url) que o anúncio usa.
     * Limite do ML: até 10 fotos por anúncio (categorias gerais).
     */
    suspend fun subirFotos(token: String, fotosJpeg: List<ByteArray>): List<String> =
        withContext(Dispatchers.IO) {
            val urls = mutableListOf<String>()
            for (foto in fotosJpeg.take(10)) {
                val base64 = android.util.Base64.encodeToString(foto, android.util.Base64.NO_WRAP)
                val payload = buildJsonObject { put("image", "data:image/jpeg;base64,$base64") }
                val req = Request.Builder()
                    .url("https://api.mercadolibre.com/pictures")
                    .addHeader("Authorization", "Bearer $token")
                    .addHeader("Content-Type", "application/json")
                    .post(payload.toString().toRequestBody("application/json".toMediaType()))
                    .build()
                try {
                    cliente.newCall(req).execute().use { resp ->
                        val texto = resp.body?.string() ?: ""
                        if (resp.isSuccessful) {
                            val obj = json.parseToJsonElement(texto).jsonObject
                            obj["body"]?.let { corpo ->
                                when (corpo) {
                                    is kotlinx.serialization.json.JsonArray -> {
                                        val u = corpo.firstOrNull()?.jsonObject?.get("secure_url")
                                        u?.jsonPrimitive?.content?.let { urls.add(it) }
                                    }
                                    else -> {
                                        val u = corpo.jsonObject["secure_url"]?.jsonPrimitive?.content
                                        u?.let { urls.add(it) }
                                    }
                                }
                            }
                        } else {
                            Log.e("AnunciaAI", "ML pictures HTTP ${resp.code}: ${texto.take(150)}")
                        }
                    }
                } catch (e: Exception) {
                    Log.e("AnunciaAI", "ML pictures", e)
                }
            }
            urls
        }

    // ---- Publicação (/items) ----

    /**
     * Publica o anúncio com fotos já hospedadas (urls do /pictures).
     * price entre 12 e 9999999; título até 60 chars; condition: new | used | refurbished.
     */
    suspend fun publicarItem(
        token: String,
        titulo: String,
        descricao: String,
        preco: Double,
        categoriaId: String,
        condicao: String,
        urlsFotos: List<String>
    ): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        val descEscapada = jsonStr(descricao)
        val tituloEscapado = jsonStr(titulo.take(60))
        val condicaoMl = when {
            condicao.startsWith("novo", ignoreCase = true) -> "new"
            else -> "used" // ML não tem "like new"
        }
        val imagens = urlsFotos.take(10).joinToString(",") { jsonStr(it) }
        val payload = """
            {
              "title": $tituloEscapado,
              "description": {"plain_text": $descEscapada},
              "category_id": "$categoriaId",
              "price": $preco,
              "currency_id": "BRL",
              "available_quantity": 1,
              "buying_mode": "buy_it_now",
              "listing_type_id": "gold_pro",
              "condition": "$condicaoMl",
              "pictures": [$imagens]
            }
        """.trimIndent()

        val req = Request.Builder()
            .url("https://api.mercadolibre.com/items")
            .addHeader("Authorization", "Bearer $token")
            .addHeader("Content-Type", "application/json")
            .post(payload.toRequestBody("application/json".toMediaType()))
            .build()

        cliente.newCall(req).execute().use { resp ->
            val texto = resp.body?.string() ?: ""
            if (resp.isSuccessful) {
                try {
                    val obj = json.parseToJsonElement(texto).jsonObject
                    // retorna "id|permalink" (id externo usado p/ encerrar/editar depois)
                    val idMl = obj["id"]?.jsonPrimitive?.content ?: ""
                    val permalink = obj["permalink"]?.jsonPrimitive?.content ?: ""
                    return@use true to "$idMl|$permalink"
                } catch (e: Exception) {
                    return@use true to "|"
                }
            }
            false to "HTTP ${resp.code}: ${texto.take(200)}"
        }
    }

    /** Busca uma categoria pela árvore do ML (predictor). */
    suspend fun preverCategoria(token: String, titulo: String): String? =
        withContext(Dispatchers.IO) {
            val req = Request.Builder()
                .url("https://api.mercadolibre.com/sites/MLB/domain_discovery/search?q=" +
                    java.net.URLEncoder.encode(titulo, "UTF-8") + "&limit=1")
                .addHeader("Authorization", "Bearer $token")
                .get()
                .build()
            cliente.newCall(req).execute().use { resp ->
                val texto = resp.body?.string() ?: return@use null
                if (!resp.isSuccessful) return@use null
                try {
                    val arr = json.parseToJsonElement(texto).jsonArray
                    (arr.firstOrNull()?.jsonObject?.get("category_id"))?.jsonPrimitive?.content
                } catch (e: Exception) { null }
            }
        }

    // ---- Perguntas/mensagens pós-venda (inbox real, spec v2 §3) ----

    /**
     * Puxa as perguntas não respondidas dos anúncios do usuário.
     * Retorna lista de (pergunta_id, item_id, texto, de_quem, data).
     */
    suspend fun puxarPerguntas(token: String, userId: String): List<PerguntaML> =
        withContext(Dispatchers.IO) {
            val req = Request.Builder()
                .url("https://api.mercadolibre.com/questions/search?user_id=$userId&filter=unanswered&sort=date_desc&limit=20")
                .addHeader("Authorization", "Bearer $token")
                .get()
                .build()
            cliente.newCall(req).execute().use { resp ->
                val texto = resp.body?.string() ?: return@use emptyList()
                if (!resp.isSuccessful) return@use emptyList()
                try {
                    val arr = json.parseToJsonElement(texto).jsonObject["questions"]?.jsonArray
                    arr?.mapNotNull { q ->
                        val obj = q.jsonObject
                        val from = obj["from"]?.jsonObject
                        PerguntaML(
                            id = obj["id"]?.jsonPrimitive?.content ?: return@mapNotNull null,
                            itemId = obj["item_id"]?.jsonPrimitive?.content ?: "",
                            texto = obj["text"]?.jsonPrimitive?.content ?: "",
                            deQuem = from?.get("nickname")?.jsonPrimitive?.content ?: "",
                            data = obj["date_created"]?.jsonPrimitive?.content ?: "",
                            respondida = false
                        )
                    } ?: emptyList()
                } catch (e: Exception) { emptyList() }
            }
        }

    /** Responde uma pergunta pela API. */
    suspend fun responderPergunta(token: String, perguntaId: String, resposta: String): Boolean =
        withContext(Dispatchers.IO) {
            val payload = buildJsonObject { put("text", resposta) }
            val req = Request.Builder()
                .url("https://api.mercadolibre.com/answers/$perguntaId")
                .addHeader("Authorization", "Bearer $token")
                .addHeader("Content-Type", "application/json")
                .post(payload.toString().toRequestBody("application/json".toMediaType()))
                .build()
            cliente.newCall(req).execute().use { resp -> resp.isSuccessful }
        }

    /** Encerra o anúncio (marcar vendido / tirar do ar). */
    suspend fun encerrarAnuncio(token: String, itemIdMl: String): Boolean =
        withContext(Dispatchers.IO) {
            val payload = buildJsonObject { put("status", "closed") }
            val req = Request.Builder()
                .url("https://api.mercadolibre.com/items/$itemIdMl")
                .addHeader("Authorization", "Bearer $token")
                .addHeader("Content-Type", "application/json")
                .put(payload.toString().toRequestBody("application/json".toMediaType()))
                .build()
            cliente.newCall(req).execute().use { resp -> resp.isSuccessful }
        }

    companion object {
        const val REDIRECT_URI = "br.com.anunciaai://oauth/ml"
    }
}

/** Token retornado pelo OAuth do ML. */
data class TokenML(
    val accessToken: String,
    val refreshToken: String?,
    val expiraEm: Long,
    val userId: String?
)

/** Pergunta de comprador (ML). */
data class PerguntaML(
    val id: String,
    val itemId: String,
    val texto: String,
    val deQuem: String,
    val data: String,
    val respondida: Boolean
)
