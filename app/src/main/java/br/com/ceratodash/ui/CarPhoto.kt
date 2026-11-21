package br.com.ceratodash.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.File

/**
 * Foto do carro. Ordem de escolha: a foto escolhida pelo usuário em Ajustes (guardada só no aparelho),
 * depois a que vem no APK (res/drawable-nodpi/car_photo), depois a silhueta desenhada.
 * Sempre reduzida para no máximo 1200 pixels de largura, por causa da memória da central.
 */
object CarPhoto {
    private const val MAX_WIDTH = 1200

    private fun file(c: Context) = File(c.filesDir, "car_photo.png")

    fun hasUserPhoto(c: Context): Boolean = file(c).exists()

    /** Carrega a foto a mostrar, ou null se não há nenhuma (aí vale a silhueta). */
    fun load(c: Context): Bitmap? {
        val f = file(c)
        if (f.exists()) {
            val bmp = decode { o -> BitmapFactory.decodeFile(f.path, o) }
            if (bmp != null) return bmp
        }
        val id = c.resources.getIdentifier("car_photo", "drawable", c.packageName)
        if (id == 0) return null
        return decode { o -> BitmapFactory.decodeResource(c.resources, id, o) }
    }

    /** Copia a imagem escolhida (reduzida) para o armazenamento do app. Devolve false se não conseguiu ler. */
    fun save(c: Context, uri: Uri): Boolean {
        return try {
            val bmp = decode { o -> c.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, o) } }
                ?: return false
            file(c).outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
            bmp.recycle()
            true
        } catch (e: Exception) {
            false
        }
    }

    fun clear(c: Context) {
        file(c).delete()
    }

    /** Decodifica em duas passadas: primeiro só o tamanho, depois com a redução certa. */
    private fun decode(read: (BitmapFactory.Options) -> Bitmap?): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        read(bounds)
        if (bounds.outWidth <= 0) return null
        var sample = 1
        while (bounds.outWidth / sample > MAX_WIDTH) sample *= 2
        return read(BitmapFactory.Options().apply { inSampleSize = sample })
    }
}
