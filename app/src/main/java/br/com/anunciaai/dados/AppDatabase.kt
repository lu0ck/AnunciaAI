package br.com.anunciaai.dados

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [Item::class, FotoItem::class, PublicacaoPlataforma::class, ContaConectada::class, PerfilUsuario::class],
    version = 5,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun itemDao(): ItemDao
    abstract fun fotoDao(): FotoDao
    abstract fun publicacaoDao(): PublicacaoDao
    abstract fun contaDao(): ContaDao
    abstract fun perfilDao(): PerfilDao

    companion object {
        @Volatile
        private var instancia: AppDatabase? = null

        fun obter(context: Context): AppDatabase =
            instancia ?: synchronized(this) {
                instancia ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "anunciaai.db"
                )
                    // v1 nunca foi publicada (não há dados reais) — migração destrutiva 1→2
                    .fallbackToDestructiveMigration()
                    .build().also { instancia = it }
            }
    }
}
