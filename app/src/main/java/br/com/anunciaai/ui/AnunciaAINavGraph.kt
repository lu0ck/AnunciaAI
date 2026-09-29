package br.com.anunciaai.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.NavType
import br.com.anunciaai.ui.screens.AnaliseScreen
import br.com.anunciaai.ui.screens.ConexoesScreen
import br.com.anunciaai.ui.screens.DetalheItemScreen
import br.com.anunciaai.ui.screens.ListaItensScreen
import br.com.anunciaai.ui.screens.MensagensScreen
import br.com.anunciaai.ui.screens.NovaCapturaScreen
import br.com.anunciaai.ui.screens.RevisaoScreen
import br.com.anunciaai.ui.screens.StatusScreen

object Rotas {
    const val LISTA = "lista"
    const val CAPTURA = "captura"
    const val CONEXOES = "conexoes"
    const val MENSAGENS = "mensagens"
    const val ANALISE = "analise/{itemId}"
    const val REVISAO = "revisao/{itemId}"
    const val STATUS = "status/{itemId}"
    const val DETALHE = "detalhe/{itemId}"

    fun analise(itemId: Long) = "analise/$itemId"
    fun revisao(itemId: Long) = "revisao/$itemId"
    fun status(itemId: Long) = "status/$itemId"
    fun detalhe(itemId: Long) = "detalhe/$itemId"
}

@Composable
fun AnunciaAINavGraph() {
    val nav = rememberNavController()
    val contexto = LocalContext.current

    // navegação da barra inferior (estilo abas, sem empilhar)
    val navBottom: (String) -> Unit = { rota ->
        if (rota != nav.currentDestination?.route) {
            nav.navigate(rota) {
                launchSingleTop = true
                popUpTo(Rotas.LISTA) { saveState = true }
                restoreState = true
            }
        }
    }

    NavHost(navController = nav, startDestination = Rotas.LISTA) {
        composable(Rotas.LISTA) {
            ListaItensScreen(
                onNovoItem = { nav.navigate(Rotas.CAPTURA) },
                onAbrirItem = { nav.navigate(Rotas.detalhe(it)) },
                onNavBottom = navBottom
            )
        }
        composable(Rotas.CONEXOES) {
            ConexoesScreen()
        }
        composable(Rotas.MENSAGENS) {
            MensagensScreen(onNavBottom = navBottom)
        }
        composable(Rotas.CAPTURA) {
            NovaCapturaScreen(
                onVoltar = { nav.popBackStack() },
                onItemCriado = { itemId ->
                    // v4: fotos → tela de análise com pilha de cards → revisão
                    nav.navigate(Rotas.analise(itemId)) {
                        popUpTo(Rotas.CAPTURA) { inclusive = true }
                    }
                },
                onNavBottom = navBottom
            )
        }
        composable(
            Rotas.ANALISE,
            arguments = listOf(navArgument("itemId") { type = NavType.LongType })
        ) { entrada ->
            val itemId = entrada.arguments?.getLong("itemId") ?: 0L
            AnaliseScreen(
                itemId = itemId,
                onVerAnuncio = { id ->
                    nav.navigate(Rotas.revisao(id)) {
                        popUpTo(Rotas.CAPTURA) { inclusive = true }
                    }
                },
                onVoltar = { nav.popBackStack() }
            )
        }
        composable(
            Rotas.REVISAO,
            arguments = listOf(navArgument("itemId") { type = NavType.LongType })
        ) { entrada ->
            val itemId = entrada.arguments?.getLong("itemId") ?: 0L
            RevisaoScreen(
                itemId = itemId,
                onVoltar = { nav.popBackStack() },
                onPublicado = { nav.navigate(Rotas.status(itemId)) { popUpTo(Rotas.LISTA) } },
                onNavBottom = navBottom
            )
        }
        composable(
            Rotas.STATUS,
            arguments = listOf(navArgument("itemId") { type = NavType.LongType })
        ) { entrada ->
            val itemId = entrada.arguments?.getLong("itemId") ?: 0L
            StatusScreen(
                itemId = itemId,
                onVoltar = { nav.popBackStack() },
                onVerDetalhe = { nav.navigate(Rotas.detalhe(itemId)) },
                onNavBottom = navBottom
            )
        }
        composable(
            Rotas.DETALHE,
            arguments = listOf(navArgument("itemId") { type = NavType.LongType })
        ) { entrada ->
            val itemId = entrada.arguments?.getLong("itemId") ?: 0L
            DetalheItemScreen(
                itemId = itemId,
                onVoltar = { nav.popBackStack() }
            )
        }
    }
}
