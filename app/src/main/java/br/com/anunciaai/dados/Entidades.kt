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
