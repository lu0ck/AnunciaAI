package br.com.anunciaai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import br.com.anunciaai.ui.AnunciaAINavGraph
import br.com.anunciaai.ui.theme.AnunciaAITheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AnunciaAITheme {
                AnunciaAINavGraph()
            }
        }
    }
}
