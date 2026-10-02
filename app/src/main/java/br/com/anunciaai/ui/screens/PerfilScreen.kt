package br.com.anunciaai.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import br.com.anunciaai.AnunciaAIApp
import br.com.anunciaai.dados.PerfilUsuario
import br.com.anunciaai.ui.foto.FotoMini
import br.com.anunciaai.ui.foto.FotoUtil
import br.com.anunciaai.ui.theme.CorSuperficie
import br.com.anunciaai.ui.theme.Destaque
import kotlinx.coroutines.launch

/**
 * v9 — Perfil editável: foto (galeria), nome, nick, bio e redes sociais.
 * Salva no Room (perfil_usuario). Avatar com anel verde + micro-press.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PerfilScreen(
    onVoltar: () -> Unit = {},
    onNavBottom: (String) -> Unit = {}
) {
    val contexto = LocalContext.current
    val app = contexto.applicationContext as AnunciaAIApp
    val escopo = rememberCoroutineScope()
    val perfil by app.repositorio.perfil().collectAsState(initial = null)

    var nome by remember(perfil?.id) { mutableStateOf(perfil?.nome ?: "") }
    var nick by remember(perfil?.id) { mutableStateOf(perfil?.nick ?: "") }
    var bio by remember(perfil?.id) { mutableStateOf(perfil?.bio ?: "") }
    var instagram by remember(perfil?.id) { mutableStateOf(perfil?.instagram ?: "") }
    var whatsapp by remember(perfil?.id) { mutableStateOf(perfil?.whatsapp ?: "") }
    var telegram by remember(perfil?.id) { mutableStateOf(perfil?.id.let { "" }) }
    var tiktok by remember(perfil?.id) { mutableStateOf("") }
    var fotoPerfil by remember(perfil?.id) { mutableStateOf(perfil?.fotoUri) }
    var salvo by remember { mutableStateOf(false) }

    val galeria = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let {
            val copiada = FotoUtil.copiarParaInterno(contexto, it)
            if (copiada != null) fotoPerfil = copiada
        }
    }

    fun salvar() {
        escopo.launch {
            app.repositorio.salvarPerfil(
                PerfilUsuario(
                    nome = nome.trim(), nick = nick.trim(), bio = bio.trim(),
                    instagram = instagram.trim(), whatsapp = whatsapp.trim(),
                    telegram = telegram.trim(), tiktok = tiktok.trim(),
                    fotoUri = fotoPerfil
                )
            )
            salvo = true
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Perfil") },
                navigationIcon = {
                    TextButton(onClick = onVoltar) { Text("Voltar", color = Destaque) }
                }
            )
        }
    ) { pad ->
        Column(
            Modifier.padding(pad).fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(16.dp))
            // Avatar com anel de destaque + badge de editar
            Box(contentAlignment = Alignment.BottomEnd) {
                val escala by animateFloatAsState(1f, spring(), label = "avatar")
                Box(
                    Modifier
                        .size(116.dp)
                        .scale(escala)
                        .clip(CircleShape)
                        .background(Destaque.copy(alpha = 0.25f)),
                    contentAlignment = Alignment.Center
                ) {
                    if (fotoPerfil != null) {
                        FotoMini(fotoPerfil, tamanho = 104)
                    } else {
                        Icon(Icons.Default.Person, contentDescription = null,
                            modifier = Modifier.size(52.dp), tint = Destaque)
                    }
                }
                SmallFloatingActionButton(
                    onClick = {
                        galeria.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                    containerColor = Destaque,
                    contentColor = Color(0xFF06231B),
                    shape = CircleShape,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = "Trocar foto", modifier = Modifier.size(18.dp))
                }
            }
            Spacer(Modifier.height(18.dp))
            Text(nick.ifBlank { "Seu perfil" }, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
            Text(nome, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (bio.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(bio, style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 3)
            }
            Spacer(Modifier.height(22.dp))

            // Campos de edição
            CampoPerfil("Nome", nome) { nome = it }
            Spacer(Modifier.height(10.dp))
            CampoPerfil("Nick", nick) { nick = it }
            Spacer(Modifier.height(10.dp))
            CampoPerfil("Bio", bio, minLinhas = 2) { bio = it }
            Spacer(Modifier.height(18.dp))
            Text("Redes sociais", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.align(Alignment.Start))
            Spacer(Modifier.height(10.dp))
            CampoPerfil("Instagram", instagram) { instagram = it }
            Spacer(Modifier.height(10.dp))
            CampoPerfil("WhatsApp (DDD+número)", whatsapp) { whatsapp = it }
            Spacer(Modifier.height(10.dp))
            CampoPerfil("Telegram", telegram) { telegram = it }
            Spacer(Modifier.height(10.dp))
            CampoPerfil("TikTok", tiktok) { tiktok = it }
            Spacer(Modifier.height(22.dp))

            Button(
                onClick = { salvar() },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Destaque, contentColor = Color(0xFF06231B))
            ) { Text("Salvar perfil", style = MaterialTheme.typography.titleMedium) }

            androidx.compose.animation.AnimatedVisibility(visible = salvo) {
                Text("Salvo ✓", color = Destaque, style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(top = 8.dp))
            }
            Spacer(Modifier.height(28.dp))
        }
    }
}

@Composable
private fun CampoPerfil(rotulo: String, valor: String, minLinhas: Int = 1, onMuda: (String) -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        Text(rotulo, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(4.dp))
        OutlinedTextField(
            value = valor,
            onValueChange = onMuda,
            modifier = Modifier.fillMaxWidth(),
            minLines = minLinhas,
            shape = MaterialTheme.shapes.small,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color.Transparent,
                unfocusedBorderColor = Color.Transparent,
                focusedContainerColor = CorSuperficie,
                unfocusedContainerColor = CorSuperficie
            )
        )
    }
}
