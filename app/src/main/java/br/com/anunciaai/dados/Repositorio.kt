package br.com.anunciaai.dados

import kotlinx.coroutines.flow.Flow

class Repositorio(private val banco: AppDatabase) {
    private val itemDao = banco.itemDao()
    private val fotoDao = banco.fotoDao()
    private val pubDao = banco.publicacaoDao()
    private val contaDao = banco.contaDao()
    private val perfilDao = banco.perfilDao()

    // Itens
    fun itens(): Flow<List<Item>> = itemDao.observeTodos()
    fun itensComBusca(busca: String): Flow<List<Item>> =
        if (busca.isBlank()) itemDao.observeTodos() else itemDao.buscarPorTitulo(busca)
    fun item(id: Long): Flow<Item?> = itemDao.observePorId(id)
    suspend fun itemNow(id: Long): Item? = itemDao.porId(id)
    suspend fun salvarItem(item: Item): Long =
        if (item.id == 0L) itemDao.inserir(item) else { itemDao.atualizar(item); item.id }
    suspend fun apagarItem(id: Long) = itemDao.apagar(id)

    // Fotos (até 10 por item)
    fun fotosDoItem(id: Long): Flow<List<FotoItem>> = fotoDao.observePorItem(id)
    suspend fun fotosDoItemNow(id: Long): List<FotoItem> = fotoDao.porItem(id)
    suspend fun adicionarFoto(itemId: Long, uri: String): Boolean {
        val atual = fotoDao.porItem(itemId)
        if (atual.size >= 10) return false
        val ordem = (fotoDao.maxOrdem(itemId)) + 1
        fotoDao.inserir(FotoItem(itemId = itemId, uri = uri, ordem = ordem))
        return true
    }
    suspend fun removerFoto(fotoId: Long) = fotoDao.apagar(fotoId)
    suspend fun reordenarFotos(fotos: List<FotoItem>) {
        fotos.forEachIndexed { i, f -> fotoDao.atualizar(f.copy(ordem = i)) }
    }

    // Publicações
    fun publicacoesDoItem(id: Long): Flow<List<PublicacaoPlataforma>> = pubDao.observePorItem(id)
    fun publicacoes(): Flow<List<PublicacaoPlataforma>> = pubDao.observeTodas()
    suspend fun publicacoesDoItemNow(id: Long): List<PublicacaoPlataforma> = pubDao.porItem(id)
    suspend fun publicacaoDoItemEPlataformaNow(itemId: Long, plataforma: String): PublicacaoPlataforma? =
        pubDao.porItemEPlataforma(itemId, plataforma)
    suspend fun salvarPublicacao(pub: PublicacaoPlataforma): Long = pubDao.inserir(pub)
    suspend fun atualizarPublicacao(pub: PublicacaoPlataforma) = pubDao.atualizar(pub)
    suspend fun apagarPublicacao(itemId: Long, plataforma: String) =
        pubDao.apagarPorItemEPlataforma(itemId, plataforma)

    // Contas conectadas
    fun contas(): Flow<List<ContaConectada>> = contaDao.observeTodas()
    suspend fun conta(plataforma: String): ContaConectada? = contaDao.porPlataforma(plataforma)
    suspend fun salvarConta(conta: ContaConectada): Long = contaDao.inserir(conta)
    suspend fun apagarConta(plataforma: String) = contaDao.apagarPorPlataforma(plataforma)

    // Perfil do usuário (v9)
    fun perfil(): Flow<PerfilUsuario?> = perfilDao.observe()
    suspend fun perfilNow(): PerfilUsuario? = perfilDao.porId()
    suspend fun salvarPerfil(p: PerfilUsuario) = perfilDao.salvar(p)
}
