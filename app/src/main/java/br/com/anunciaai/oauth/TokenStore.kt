package br.com.anunciaai.oauth

import android.content.Context
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Cofre de tokens OAuth: EncryptedSharedPreferences (AES-256 GCM via MasterKey do Keystore).
 * Nada de token em texto puro, nada em log/console.
 */
object TokenStore {
    private const val ARQ = "anunciaai_tokens"

    private fun prefs(context: Context) = EncryptedSharedPreferences.create(
        context,
        ARQ,
        MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun salvar(context: Context, plataforma: String, accessToken: String, refreshToken: String? = null) {
        try {
            prefs(context).edit()
                .putString("${plataforma}_at", accessToken)
                .putString("${plataforma}_rt", refreshToken)
                .apply()
        } catch (e: Exception) {
            Log.e("AnunciaAI", "TokenStore: falha ao salvar", e)
        }
    }

    fun accessToken(context: Context, plataforma: String): String? = try {
        prefs(context).getString("${plataforma}_at", null)
    } catch (e: Exception) {
        Log.e("AnunciaAI", "TokenStore: falha ao ler", e)
        null
    }

    fun refreshToken(context: Context, plataforma: String): String? = try {
        prefs(context).getString("${plataforma}_rt", null)
    } catch (e: Exception) { null }

    fun apagar(context: Context, plataforma: String) {
        try {
            prefs(context).edit()
                .remove("${plataforma}_at")
                .remove("${plataforma}_rt")
                .apply()
        } catch (e: Exception) {
            Log.e("AnunciaAI", "TokenStore: falha ao apagar", e)
        }
    }
}
