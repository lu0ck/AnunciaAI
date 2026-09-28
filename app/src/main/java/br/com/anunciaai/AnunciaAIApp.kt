package br.com.anunciaai

import android.app.Application
import br.com.anunciaai.dados.AppDatabase
import br.com.anunciaai.dados.Repositorio

class AnunciaAIApp : Application() {
    val banco: AppDatabase by lazy { AppDatabase.obter(this) }
    val repositorio: Repositorio by lazy { Repositorio(banco) }
}
