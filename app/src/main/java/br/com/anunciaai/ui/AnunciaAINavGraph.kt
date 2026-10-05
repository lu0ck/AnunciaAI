package br.com.anunciaai.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.core.tween
import br.com.anunciaai.ui.screens.AnaliseScreen
import br.com.anunciaai.ui.screens.ConfigScreen
import br.com.anunciaai.ui.screens.ConexoesScreen
import br.com.anunciaai.ui.screens.DetalheItemScreen
import br.com.anunciaai.ui.screens.ListaItensScreen
import br.com.anunciaai.ui.screens.MensagensScreen
import br.com.anunciaai.ui.screens.NovaCapturaScreen
import br.com.anunciaai.ui.screens.PerfilScreen
import br.com.anunciaai.ui.screens.RevisaoScreen
import br.com.anunciaai.ui.screens.StatusScreen
import br.com.anunciaai.ui.screens.VitrineScreen

object Rotas {
    const val LISTA = "lista"
    const val CAPTURA = "captura"
    const val VITRINE = "vitrine"
    const val PERFIL = "perfil"
    const val PERFIL_EDIT = "perfilEdit"
    const val CONEXOES = "conexoes"
    const val CONFIG = "config"
    const val MENSAGENS = "mensagens"
    const val ANALISE = "analise/{itemId}?ean={ean}"
    const val REVISAO = "revisao/{itemId}"
    const val STATUS = "status/{itemId}"
    const val DETALHE = "detalhe/{itemId}"

    fun analise(itemId: Long, ean: String? = null) =
        "analise/$itemId" + (if (!ean.isNullOrBlank()) "?ean=$ean" else "")

    fun revisao(itemId: Long) = "revisao/$itemId"
    fun status(itemId: Long) = "status/$itemId"
    fun detalhe(itemId: Long) = "detalhe/$itemId"
}

/**
 * v12.0 — ESTRUTURA DE NAVEGAÇÃO RAIZ.
 * A cápsula + FAB único moram AQUI (um único Box raiz), nunca
 * redeclarados por tela. Telas são content puro, sem bottomBar.
 * Telas de fluxo (captura/análise/revisão/status/detalhe/conexões/perfil)
 * abrem SEM a cápsula — tela cheia.
 */
@Composable
fun AnunciaAINavGraph() {
    val nav = rememberNavController()
    val entradaAtual by nav.currentBackStackEntryAsState()
    val rotaAtual = entradaAtual?.destination?.route ?: Rotas.LISTA

    // abas com cápsula visível; fluxo roda em tela cheia
    val abas = setOf(Rotas.LISTA, Rotas.VITRINE, Rotas.MENSAGENS, Rotas.CONFIG)
    val mostrarCapsula = rotaAtual in abas

    val navBottom: (String) -> Unit = { rota ->
        if (rota != nav.currentDestination?.route) {
            nav.navigate(rota) {
                launchSingleTop = true
                popUpTo(Rotas.LISTA) { saveState = true }
                restoreState = true
            }
        }
    }

    Box(Modifier.fillMaxSize()) {
        NavHost(
            navController = nav,
            startDestination = Rotas.LISTA,
            enterTransition = { fadeIn(tween(220)) + slideInHorizontally(tween(220)) { it / 4 } },
            exitTransition = { fadeOut(tween(180)) },
            popEnterTransition = { fadeIn(tween(220)) + slideInHorizontally(tween(220)) { -it / 4 } },
            popExitTransition = { fadeOut(tween(180)) + slideOutHorizontally(tween(220)) { it / 4 } },
            modifier = Modifier.fillMaxSize()
        ) {
            composable(Rotas.LISTA) {
                ListaItensScreen(
                    onNovoItem = { nav.navigate(Rotas.CAPTURA) },
                    onAbrirItem = { nav.navigate(Rotas.detalhe(it)) }
                )
            }
            composable(Rotas.VITRINE) {
                VitrineScreen(
                    onAbrirItem = { nav.navigate(Rotas.detalhe(it)) },
                    onNovoItem = { nav.navigate(Rotas.CAPTURA) }
                )
            }
            composable(Rotas.PERFIL) {
                PerfilScreen(onVoltar = { nav.popBackStack() })
            }
            composable(Rotas.PERFIL_EDIT) {
                PerfilScreen(onVoltar = { nav.popBackStack() })
            }
            composable(Rotas.CONEXOES) {
                ConexoesScreen(embutida = false)
            }
            composable(Rotas.CONFIG) {
                ConfigScreen(
                    onAbrirPerfil = { nav.navigate(Rotas.PERFIL_EDIT) },
                    onAbrirConexoes = { nav.navigate(Rotas.CONEXOES) }
                )
            }
            composable(Rotas.MENSAGENS) {
                MensagensScreen()
            }
            composable(Rotas.CAPTURA) {
                NovaCapturaScreen(
                    onVoltar = { nav.popBackStack() },
                    onItemCriado = { itemId, ean ->
                        nav.navigate(Rotas.analise(itemId, ean)) {
                            popUpTo(Rotas.CAPTURA) { inclusive = true }
                        }
                    }
                )
            }
            composable(
                Rotas.ANALISE,
                arguments = listOf(
                    navArgument("itemId") { type = NavType.LongType },
                    navArgument("ean") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    }
                )
            ) { entrada ->
                val itemId = entrada.arguments?.getLong("itemId") ?: 0L
                val ean = entrada.arguments?.getString("ean")
                AnaliseScreen(
                    itemId = itemId,
                    ean = ean,
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
                    onPublicado = { nav.navigate(Rotas.status(itemId)) { popUpTo(Rotas.LISTA) } }
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
                    onVerDetalhe = { nav.navigate(Rotas.detalhe(itemId)) }
                )
            }
            composable(
                Rotas.DETALHE,
                arguments = listOf(navArgument("itemId") { type = NavType.LongType })
            ) { entrada ->
                val itemId = entrada.arguments?.getLong("itemId") ?: 0L
                DetalheItemScreen(itemId = itemId, onVoltar = { nav.popBackStack() })
            }
        }

        // ── cápsula + FAB único, flutuando sobre o conteúdo (RAIZ) ──
        if (mostrarCapsula) {
            NavCapsula(
                atual = rotaAtual,
                onNav = navBottom,
                onVender = { nav.navigate(Rotas.CAPTURA) },
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}
