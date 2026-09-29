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
        // v4.1: statusbar e navbar SEMPRE escuras (o app é escuro por design;
        // o default herdava o tema do sistema e deixava o topo claro em "Analisando fotos")
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(CorFundo.toArgb()),
            navigationBarStyle = SystemBarStyle.dark(CorFundo.toArgb())
        )
        setContent {
            AnunciaAITheme {
                AnunciaAINavGraph()
            }
        }
    }
}
