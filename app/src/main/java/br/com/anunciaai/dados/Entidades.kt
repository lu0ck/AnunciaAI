package br.com.anunciaai.dados

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "itens")
data class Item(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val titulo: String,
    val descricao: String,
    val categoria: String,
    val precoSugerido: Double,
    val precoFinal: Double,
    val precoComparativoMercado: Double = 0.0,
    // v11.1 (fix #2): a condição da IA agora PERSISTE — antes era descartada na
    // Análise e a Revisão reconstruía a SugestaoIA sem ela (chip caía em "bom estado").
    val condicao: String = "",
    // v11.1 (fix #6): EAN lido pelo scanner persiste no item (robustez do fluxo)
    val ean: String? = null,
    val dataCriacao: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "fotos",
    foreignKeys = [
        ForeignKey(
            entity = Item::class,
            parentColumns = ["id"],
            childColumns = ["itemId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("itemId")]
)
data class FotoItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val itemId: Long,
    val uri: String,
    val ordem: Int
)

@Entity(
    tableName = "publicacoes",
    foreignKeys = [
        ForeignKey(
            entity = Item::class,
            parentColumns = ["id"],
            childColumns = ["itemId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("itemId")]
)
data class PublicacaoPlataforma(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val itemId: Long,
    val plataforma: String,
    val status: String, // PENDENTE | PUBLICADO | ERRO | VENDIDO | ENCERRADO
    val urlAnuncio: String? = null,
    val idExterno: String? = null, // id do anúncio na plataforma (ML item id, eBay offerId)
    val mensagemErro: String? = null,
    val dataPublicacao: Long? = null
)

@Entity(tableName = "contas_conectadas")
data class ContaConectada(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val plataforma: String,
    val accessTokenCriptografado: String,
    val refreshTokenCriptografado: String? = null,
    val expiraEm: Long? = null
)

/** v9 (perfil do usuário): foto, nome, nick, bio e redes sociais — single-user. */
@Entity(tableName = "perfil_usuario")
data class PerfilUsuario(
    @PrimaryKey val id: Int = 1,
    val fotoUri: String? = null,
    val nome: String = "",
    val nick: String = "",
    val bio: String = "",
    val instagram: String = "",
    val whatsapp: String = "",
    val telegram: String = "",
    val tiktok: String = "",
    val atualizadoEm: Long = System.currentTimeMillis()
)
