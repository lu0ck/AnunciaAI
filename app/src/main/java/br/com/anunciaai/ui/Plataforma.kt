package br.com.anunciaai.ui

enum class Plataforma(
    val rotulo: String,
    val precisaOAuth: Boolean,
    val temApiOficial: Boolean,
    val iniciais: String
) {
    MERCADO_LIVRE("Mercado Livre", true, true, "ML"),
    EBAY("eBay", true, true, "EB"),
    SHOPEE("Shopee", true, true, "SH"),
    OLX("OLX", false, false, "OLX"),
    FACEBOOK_MARKETPLACE("Facebook Marketplace", false, false, "FB"),
    ENJOEI("Enjoei", false, false, "EJ");

    companion object {
        fun doNome(nome: String): Plataforma? = entries.firstOrNull { it.name == nome }
    }
}
