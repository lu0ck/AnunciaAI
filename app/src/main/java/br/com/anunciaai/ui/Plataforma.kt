package br.com.anunciaai.ui

enum class Plataforma(val rotulo: String, val precisaOAuth: Boolean, val temApiOficial: Boolean) {
    MERCADO_LIVRE("Mercado Livre", true, true),
    EBAY("eBay", true, true),
    SHOPEE("Shopee", true, true),
    OLX("OLX", false, false),
    FACEBOOK_MARKETPLACE("Facebook Marketplace", false, false),
    ENJOEI("Enjoei", false, false);

    companion object {
        fun doNome(nome: String): Plataforma? = entries.firstOrNull { it.name == nome }
    }
}
