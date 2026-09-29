package br.com.anunciaai.oauth

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import br.com.anunciaai.ui.theme.AnunciaAITheme

/**
 * Activity que recebe o redirect OAuth (deep link br.com.anunciaai://oauth/...),
 * mostra o resultado na tela e volta.
 */
class OAuthCallbackActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        var mensagem by mutableStateOf("Processando autorização...")
        var ok by mutableStateOf(false)

        OAuthCallbackHandler.tratar(this, intent?.data) { _, sucesso, msg ->
            mensagem = msg
            ok = sucesso
        }

        setContent {
            AnunciaAITheme {
                Surface(Modifier.fillMaxSize()) {
                    Column(
                        Modifier.fillMaxSize().padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(if (ok) "✓" else "⚠", style = MaterialTheme.typography.displayMedium)
                        Spacer(Modifier.height(12.dp))
                        Text(mensagem, style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(24.dp))
                        Button(onClick = { finish() }) { Text("Voltar") }
                    }
                }
            }
        }
    }
}
