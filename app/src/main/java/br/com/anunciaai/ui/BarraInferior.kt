package br.com.anunciaai.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import br.com.anunciaai.ui.theme.Destaque

/**
 * Barra inferior: Início / Vender / Mensagens / Conexões.
 * v3: aba ativa SEMPRE na cor de destaque única (#00C896) — nunca mostarda.
 */
@Composable
fun BarraInferior(atual: String, onNav: (String) -> Unit) {
    NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainer) {
        NavigationBarItem(
            selected = atual == Rotas.LISTA,
            onClick = { onNav(Rotas.LISTA) },
            icon = { Icon(Icons.Default.Home, contentDescription = "Início") },
            label = { Text("Início") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Destaque,
                selectedTextColor = Destaque,
                indicatorColor = MaterialTheme.colorScheme.primaryContainer
            )
        )
        NavigationBarItem(
            selected = atual == Rotas.CAPTURA,
            onClick = { onNav(Rotas.CAPTURA) },
            icon = { Icon(Icons.Default.AddAPhoto, contentDescription = "Vender") },
            label = { Text("Vender") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Destaque,
                selectedTextColor = Destaque,
                indicatorColor = MaterialTheme.colorScheme.primaryContainer
            )
        )
        NavigationBarItem(
            selected = atual == Rotas.MENSAGENS,
            onClick = { onNav(Rotas.MENSAGENS) },
            icon = { Icon(Icons.Default.ChatBubble, contentDescription = "Mensagens") },
            label = { Text("Mensagens") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Destaque,
                selectedTextColor = Destaque,
                indicatorColor = MaterialTheme.colorScheme.primaryContainer
            )
        )
        NavigationBarItem(
            selected = atual == Rotas.CONEXOES,
            onClick = { onNav(Rotas.CONEXOES) },
            icon = { Icon(Icons.Default.Link, contentDescription = "Conexões") },
            label = { Text("Conexões") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Destaque,
                selectedTextColor = Destaque,
                indicatorColor = MaterialTheme.colorScheme.primaryContainer
            )
        )
    }
}
