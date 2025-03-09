package `in`.sethway.ui.share

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.core.graphics.createBitmap
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

object ShareUtils {

  fun share(context: Context, drawable: Drawable, text: String) {
    val uri = drawable.toBitmap().toUri(context)
    if (uri != null) {
      val shareIntent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_TEXT, text)
        type = "image/png"
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
      }
      context.startActivity(Intent.createChooser(shareIntent, "Share QR Code"))
    }
  }

  private fun Drawable.toBitmap(): Bitmap {
    val bitmap = createBitmap(4096, 4096, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    setBounds(0, 0, canvas.width, canvas.height)
    draw(canvas)
    return bitmap
  }

  private fun Bitmap.toUri(context: Context): Uri? {
    val file = File(context.cacheDir, "temp_qr.png")
    return try {
      FileOutputStream(file).use { outputStream ->
        compress(Bitmap.CompressFormat.PNG, 100, outputStream)
        outputStream.flush()
      }
      FileProvider.getUriForFile(context, "in.sethway.provider", file)
    } catch (e: IOException) {
      e.printStackTrace()
      null
    }
  }
}