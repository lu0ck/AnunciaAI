package br.com.anunciaai.oauth

import android.net.Uri
import android.util.Log
import br.com.anunciaai.dados.ContaConectada
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Recebe o callback OAuth (br.com.anunciaai://oauth/ml?code=...&state=...)
 * e dispara a troca code→token.
 */
object OAuthCallbackHandler {

    /**
     * Trata a URI do callback. Retorna mensagem para a UI, ou null se não for callback.
     */
    fun tratar(
        contexto: android.content.Context,
        uri: Uri?,
        aoConectar: (plataforma: String, ok: Boolean, msg: String) -> Unit
    ): Boolean {
        if (uri == null || uri.scheme != "br.com.anunciaai" || uri.host != "oauth") return false

        val erro = uri.getQueryParameter("error")
        if (erro != null) {
            aoConectar("OAUTH", false, "Autorização negada: $erro")
            return true
        }

        val code = uri.getQueryParameter("code") ?: run {
            aoConectar("OAUTH", false, "Callback sem code")
            return true
        }
        val host = uri.host ?: "oauth"

        CoroutineScope(Dispatchers.IO).launch {
            val resultado = when (host) {
                "ml" -> trocarTokenML(contexto, code)
                else -> Result(false, "Plataforma de OAuth desconhecida: $host")
            }
            aoConectar("MERCADO_LIVRE", resultado.ok, resultado.msg)
        }
        return true
    }

    private suspend fun trocarTokenML(contexto: android.content.Context, code: String): Result {
        return try {
            val clientId = CredenciaisML.clientId()
                ?: return Result(false, "ML: Client ID não configurado")
            val secret = CredenciaisML.clientSecret()
                ?: return Result(false, "ML: Client Secret não configurado")
            val token = br.com.anunciaai.publica.mercadolivre.MercadoLivreApi()
                .trocarCodePorToken(code, clientId, secret)
            if (token == null) {
                Result(false, "ML: falha na troca do code por token")
            } else {
                TokenStore.salvar(contexto, "MERCADO_LIVRE", token.accessToken, token.refreshToken)
                val app = contexto.applicationContext as? br.com.anunciaai.AnunciaAIApp
                app?.repositorio?.salvarConta(
                    ContaConectada(
                        plataforma = "MERCADO_LIVRE",
                        accessTokenCriptografado = token.accessToken,
                        refreshTokenCriptografado = token.refreshToken,
                        expiraEm = token.expiraEm
                    )
                )
                Result(true, "Mercado Livre conectado!")
            }
        } catch (e: Exception) {
            Log.e("AnunciaAI", "OAuth ML", e)
            Result(false, "Erro: ${e.message}")
        }
    }

    data class Result(val ok: Boolean, val msg: String)
}
