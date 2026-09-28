package br.com.anunciaai.publica.ebay

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.FormBody
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.Base64
import java.util.concurrent.TimeUnit

/**
 * Cliente eBay Sell API (OAuth 2.0 client-credentials para user tokens via authorization code).
 * Docs: https://developer.ebay.com/api-docs/sell/inventory/overview.html
 *
 * Fluxo v1 (uso pessoal): usuário cria app em https://developer.ebay.com/signin (Production keys),
 * redirect_uri = br.com.anunciaai://oauth/ebay, preenche no local.properties:
 *   ANUNCIAAI_EBAY_CLIENT_ID=...  (App ID / Client ID)
 *   ANUNCIAAI_EBAY_CLIENT_SECRET=... (Cert ID)
 *
 * Publicação: Inventory API (createOrReplaceInventoryItem + createOffer + publishOffer).
 * Moeda USD/fit — v1 publica em USD com preço convertido ou BRL se o site for Janeiro do eBay BR.
 */
class EbayApi {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private val cliente = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    fun urlLogin(clientId: String, redirectUri: String = REDIRECT_URI): String =
        "https://auth.ebay.com/oauth2/authorize?client_id=$clientId" +
            "&response_type=code&redirect_uri=$redirectUri" +
            "&scope=https://api.ebay.com/oauth/api_scope " +
            "https://api.ebay.com/oauth/api_scope/sell.inventory " +
            "https://api.ebay.com/oauth/api_scope/sell.fulfillment"

    suspend fun trocarCodePorToken(
        code: String, clientId: String, clientSecret: String,
        redirectUri: String = REDIRECT_URI
    ): TokenEbay? = withContext(Dispatchers.IO) {
        val basic = Base64.getEncoder().encodeToString("$clientId:$clientSecret".toByteArray())
        val corpo = FormBody.Builder()
            .add("grant_type", "authorization_code")
            .add("code", code)
            .add("redirect_uri", redirectUri)
            .build()
        val req = Request.Builder()
            .url("https://api.ebay.com/identity/v1/oauth2/token")
            .addHeader("Authorization", "Basic $basic")
            .addHeader("Content-Type", "application/x-www-form-urlencoded")
            .post(corpo)
            .build()
        cliente.newCall(req).execute().use { resp ->
            val texto = resp.body?.string() ?: return@use null
            if (!resp.isSuccessful) return@use null
            try {
                val obj = json.parseToJsonElement(texto).jsonObject
                TokenEbay(
                    accessToken = obj["access_token"]!!.jsonPrimitive.content,
                    refreshToken = obj["refresh_token"]?.jsonPrimitive?.content,
                    expiraEm = System.currentTimeMillis() +
                        (obj["expires_in"]?.jsonPrimitive?.content?.toLongOrNull() ?: 7200L) * 1000
                )
            } catch (e: Exception) { null }
        }
    }

    /** Publica via Inventory API: item → offer → publish. Retorna (ok, offerId/url ou erro). */
    suspend fun publicarItem(
        token: String,
        titulo: String,
        descricao: String,
        preco: Double,
        condicao: String,
        sku: String
    ): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        val descEsc = kotlinx.serialization.json.JsonPrimitive(descricao).toString()
        val titEsc = kotlinx.serialization.json.JsonPrimitive(titulo.take(80)).toString()
        val condicaoEbay = when {
            condicao.startsWith("novo", ignoreCase = true) -> "NEW"
            condicao.contains("como novo", ignoreCase = true) -> "LIKE_NEW"
            condicao.contains("marcas", ignoreCase = true) -> "GOOD"
            else -> "GOOD"
        }
        // 1) cria/substitui inventory item
        val payloadItem = """
            {
              "sku": "$sku",
              "product": {"title": $titEsc, "description": $descEsc},
              "condition": "$condicaoEbay",
              "availability": {"shipToLocationAvailability": {"quantity": 1}}
            }
        """.trimIndent()
        val reqItem = Request.Builder()
            .url("https://api.ebay.com/sell/inventory/v1/inventory_item/$sku")
            .addHeader("Authorization", "Bearer $token")
            .addHeader("Content-Type", "application/json")
            .put(payloadItem.toRequestBody("application/json".toMediaType()))
            .build()
        cliente.newCall(reqItem).execute().use { resp ->
            if (!resp.isSuccessful) {
                return@withContext false to "inventory_item HTTP ${resp.code}: ${(resp.body?.string() ?: "").take(200)}"
            }
        }

        // 2) cria offer
        val payloadOffer = """
            {
              "sku": "$sku",
              "marketplaceId": "EBAY_US",
              "format": "FIXED_PRICE",
              "pricingSummary": {"price": {"value": "$preco", "currency": "USD"}},
              "listingPolicies": {},
              "categoryId": "9355"
            }
        """.trimIndent()
        val reqOffer = Request.Builder()
            .url("https://api.ebay.com/sell/inventory/v1/offer")
            .addHeader("Authorization", "Bearer $token")
            .addHeader("Content-Type", "application/json")
            .post(payloadOffer.toRequestBody("application/json".toMediaType()))
            .build()
        val offerId: String = cliente.newCall(reqOffer).execute().use { resp ->
            val texto = resp.body?.string() ?: ""
            if (!resp.isSuccessful) {
                return@withContext false to "offer HTTP ${resp.code}: ${texto.take(200)}"
            }
            try { json.parseToJsonElement(texto).jsonObject["offerId"]?.jsonPrimitive?.content ?: "" }
            catch (e: Exception) { "" }
        }
        if (offerId.isEmpty()) return@withContext false to "offer sem offerId"

        // 3) publica
        val reqPub = Request.Builder()
            .url("https://api.ebay.com/sell/inventory/v1/offer/$offerId/publish")
            .addHeader("Authorization", "Bearer $token")
            .addHeader("Content-Type", "application/json")
            .post("{}".toRequestBody("application/json".toMediaType()))
            .build()
        cliente.newCall(reqPub).execute().use { resp ->
            val texto = resp.body?.string() ?: ""
            if (resp.isSuccessful) true to offerId
            else false to "publish HTTP ${resp.code}: ${texto.take(200)}"
        }
    }

    companion object {
        const val REDIRECT_URI = "br.com.anunciaai://oauth/ebay"
    }
}

data class TokenEbay(
    val accessToken: String,
    val refreshToken: String?,
    val expiraEm: Long
)
