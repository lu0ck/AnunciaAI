package br.com.anunciaai.oauth

import kotlinx.coroutines.flow.MutableStateFlow

/** Estado de conexão compartilhado (mensagem da tela de Conexões / callback OAuth). */
object EstadoConexao {
    data class Msg(val ok: Boolean, val texto: String)
    val msg = MutableStateFlow<Msg?>(null)

    fun emit(ok: Boolean, texto: String) {
        msg.value = Msg(ok, texto)
    }

    fun consumir() {
        msg.value = null
    }
}
