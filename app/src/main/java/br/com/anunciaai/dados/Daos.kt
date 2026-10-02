package br.com.anunciaai.dados

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ItemDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun inserir(item: Item): Long

    @Update
    suspend fun atualizar(item: Item)

    @Query("SELECT * FROM itens ORDER BY dataCriacao DESC")
    fun observeTodos(): Flow<List<Item>>

    @Query("SELECT * FROM itens WHERE id = :id")
    fun observePorId(id: Long): Flow<Item?>

    @Query("SELECT * FROM itens WHERE id = :id")
    suspend fun porId(id: Long): Item?

    @Query("DELETE FROM itens WHERE id = :id")
    suspend fun apagar(id: Long)

    @Query("SELECT * FROM itens WHERE titulo LIKE '%' || :busca || '%' ORDER BY dataCriacao DESC")
    fun buscarPorTitulo(busca: String): Flow<List<Item>>
}

@Dao
interface FotoDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun inserir(foto: FotoItem): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun inserirVarias(fotos: List<FotoItem>)

    @Update
    suspend fun atualizar(foto: FotoItem)

    @Query("SELECT * FROM fotos WHERE itemId = :itemId ORDER BY ordem ASC")
    fun observePorItem(itemId: Long): Flow<List<FotoItem>>

    @Query("SELECT * FROM fotos WHERE itemId = :itemId ORDER BY ordem ASC")
    suspend fun porItem(itemId: Long): List<FotoItem>

    @Query("DELETE FROM fotos WHERE id = :fotoId")
    suspend fun apagar(fotoId: Long)

    @Query("DELETE FROM fotos WHERE itemId = :itemId")
    suspend fun apagarDoItem(itemId: Long)

    @Query("SELECT COALESCE(MAX(ordem), -1) FROM fotos WHERE itemId = :itemId")
    suspend fun maxOrdem(itemId: Long): Int
}

@Dao
interface PublicacaoDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun inserir(pub: PublicacaoPlataforma): Long

    @Update
    suspend fun atualizar(pub: PublicacaoPlataforma)

    @Query("SELECT * FROM publicacoes WHERE itemId = :itemId")
    fun observePorItem(itemId: Long): Flow<List<PublicacaoPlataforma>>

    @Query("SELECT * FROM publicacoes WHERE itemId = :itemId")
    suspend fun porItem(itemId: Long): List<PublicacaoPlataforma>

    @Query("SELECT * FROM publicacoes ORDER BY id DESC")
    fun observeTodas(): Flow<List<PublicacaoPlataforma>>

    @Query("DELETE FROM publicacoes WHERE itemId = :itemId AND plataforma = :plataforma")
    suspend fun apagarPorItemEPlataforma(itemId: Long, plataforma: String)

    @Query("SELECT * FROM publicacoes WHERE itemId = :itemId AND plataforma = :plataforma LIMIT 1")
    suspend fun porItemEPlataforma(itemId: Long, plataforma: String): PublicacaoPlataforma?
}

@Dao
interface ContaDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun inserir(conta: ContaConectada): Long

    @Query("SELECT * FROM contas_conectadas WHERE plataforma = :plataforma LIMIT 1")
    suspend fun porPlataforma(plataforma: String): ContaConectada?

    @Query("SELECT * FROM contas_conectadas")
    fun observeTodas(): Flow<List<ContaConectada>>

    @Query("DELETE FROM contas_conectadas WHERE plataforma = :plataforma")
    suspend fun apagarPorPlataforma(plataforma: String)
}

@Dao
interface PerfilDao {
    @Query("SELECT * FROM perfil_usuario WHERE id = 1")
    fun observe(): Flow<PerfilUsuario?>

    @Query("SELECT * FROM perfil_usuario WHERE id = 1")
    suspend fun porId(): PerfilUsuario?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun salvar(perfil: PerfilUsuario)
}
