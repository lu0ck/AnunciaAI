package br.com.anunciaai.oauth

import br.com.anunciaai.BuildConfig

/**
 * Credenciais OAuth do Mercado Livre — lidas do BuildConfig, que o AGP popula
 * a partir de local.properties (não versionado). Ofuscação: ProGuard/R8 no release.
 *
 * Para pegar as suas: crie uma aplicação em https://developers.mercadolivre.com.br/devcenter
 * com redirect URI br.com.anunciaai://oauth/ml e preencha no local.properties:
 *   ANUNCIAAI_ML_CLIENT_ID=...
 *   ANUNCIAAI_ML_CLIENT_SECRET=...
 */
object CredenciaisML {
    fun clientId(): String? = BuildConfig.ML_CLIENT_ID.takeIf { it.isNotBlank() }
    fun clientSecret(): String? = BuildConfig.ML_CLIENT_SECRET.takeIf { it.isNotBlank() }
}

/** Credenciais eBay (Sell API) — mesma mecânica. */
object CredenciaisEBAY {
    fun clientId(): String? = BuildConfig.EBAY_CLIENT_ID.takeIf { it.isNotBlank() }
    fun clientSecret(): String? = BuildConfig.EBAY_CLIENT_SECRET.takeIf { it.isNotBlank() }
}

/** Chaves de IA — mesma mecânica do local.properties. */
object ChavesIA {
    fun gemini(): String? = BuildConfig.GEMINI_KEY.takeIf { it.isNotBlank() }
    fun nvidia(): String? = BuildConfig.NVIDIA_KEY.takeIf { it.isNotBlank() }
    fun qwen(): String? = BuildConfig.QWEN_API_KEY.takeIf { it.isNotBlank() }
}
