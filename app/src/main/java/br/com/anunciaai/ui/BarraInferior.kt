package br.com.anunciaai.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

/** Barra inferior: Início / Vender (foto) / Mensagens / Conexões. */
@Composable
fun BarraInferior(atual: String, onNav: (String) -> Unit) {
    NavigationBar {
        NavigationBarItem(
            selected = atual == Rotas.LISTA,
            onClick = { onNav(Rotas.LISTA) },
            icon = { Icon(Icons.Default.Home, contentDescription = "Início") },
            label = { Text("Início") }
        )
        NavigationBarItem(
            selected = atual == Rotas.CAPTURA,
            onClick = { onNav(Rotas.CAPTURA) },
            icon = { Icon(Icons.Default.AddAPhoto, contentDescription = "Vender") },
            label = { Text("Vender") }
        )
        NavigationBarItem(
            selected = atual == Rotas.MENSAGENS,
            onClick = { onNav(Rotas.MENSAGENS) },
            icon = { Icon(Icons.Default.ChatBubble, contentDescription = "Mensagens") },
            label = { Text("Mensagens") }
        )
        NavigationBarItem(
            selected = atual == Rotas.CONEXOES,
            onClick = { onNav(Rotas.CONEXOES) },
            icon = { Icon(Icons.Default.Link, contentDescription = "Conexões") },
            label = { Text("Conexões") }
        )
    }
}
