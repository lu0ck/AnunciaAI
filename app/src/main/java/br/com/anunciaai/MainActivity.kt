package br.com.anunciaai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.SystemBarStyle
import androidx.compose.ui.graphics.toArgb
import br.com.anunciaai.ui.AnunciaAINavGraph
import br.com.anunciaai.ui.theme.AnunciaAITheme
import br.com.anunciaai.ui.theme.CorFundo

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // v12: app ESCURO por design — statusbar/navbar escuras (#12151A) sempre
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(
                scrim = CorFundo.toArgb()
            ),
            navigationBarStyle = SystemBarStyle.dark(
                scrim = CorFundo.toArgb()
            )
        )
        setContent {
            AnunciaAITheme {
                AnunciaAINavGraph()
            }
        }
    }
}
