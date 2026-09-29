package br.com.anunciaai.ui.foto

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.exifinterface.media.ExifInterface
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object FotoUtil {
    private const val TAG = "AnunciaAI"

    /** Cria arquivo novo para a foto tirada pela câmera (via FileProvider). */
    fun novoArquivoFoto(context: Context): File {
        val dir = File(context.cacheDir, "fotos").apply { mkdirs() }
        val nome = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date()) + ".jpg"
        return File(dir, nome)
    }

    /** Copia a foto (câmera ou galeria) para o armazenamento interno do app. */
    fun copiarParaInterno(context: Context, origem: Uri): String? {
        return try {
            val dir = File(context.filesDir, "imagens").apply { mkdirs() }
            val nome = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date()) + ".jpg"
            val destino = File(dir, nome)
            context.contentResolver.openInputStream(origem)?.use { entrada ->
                FileOutputStream(destino).use { saida -> entrada.copyTo(saida) }
            } ?: return null
            Uri.fromFile(destino).toString()
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao copiar foto", e)
            null
        }
    }

    /** Lê os bytes da foto para envio à IA (limitado a ~2 MB reencodando se preciso). */
    fun lerBytes(context: Context, fotoUri: String, maxBytes: Int = 2_000_000): ByteArray? {
        return try {
            val uri = Uri.parse(fotoUri)
            val bytes = when (uri.scheme) {
                "file" -> File(uri.path!!).readBytes()
                "content" -> context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                else -> null
            } ?: return null
            if (bytes.size <= maxBytes) bytes else reencodar(context, uri, maxBytes) ?: bytes
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao ler foto", e)
            null
        }
    }

    private fun reencodar(context: Context, uri: Uri, maxBytes: Int): ByteArray? {
        return try {
            val bitmap = android.graphics.BitmapFactory.decodeStream(
                when (uri.scheme) {
                    "file" -> java.io.FileInputStream(uri.path!!)
                    else -> context.contentResolver.openInputStream(uri)
                }
            ) ?: return null
            var qualidade = 85
            var saida: ByteArray
            do {
                val bos = java.io.ByteArrayOutputStream()
                bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, qualidade, bos)
                saida = bos.toByteArray()
                qualidade -= 10
            } while (saida.size > maxBytes && qualidade > 30)
            saida
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao reencodar foto", e)
            null
        }
    }

    /** Corrige rotação da foto tirada pela câmera (EXIF). */
    fun corrigirRotacao(context: Context, arquivo: File) {
        try {
            val exif = ExifInterface(arquivo.absolutePath)
            val rotacao = when (
                exif.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
                )
            ) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> 0f
            }
            if (rotacao != 0f) {
                val bmp = android.graphics.BitmapFactory.decodeFile(arquivo.absolutePath)
                val m = android.graphics.Matrix().apply { postRotate(rotacao) }
                val corrigido = android.graphics.Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, m, true)
                FileOutputStream(arquivo).use { corrigido.compress(android.graphics.Bitmap.CompressFormat.JPEG, 90, it) }
            }
        } catch (e: Exception) {
            Log.e(TAG, "EXIF: sem rotação para corrigir", e)
        }
    }
}
