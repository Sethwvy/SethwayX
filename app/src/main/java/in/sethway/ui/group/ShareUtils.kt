package `in`.sethway.ui.group

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Rect
import android.net.Uri
import android.view.View
import androidx.core.content.FileProvider
import androidx.core.graphics.createBitmap
import com.google.android.material.card.MaterialCardView
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

object ShareUtils {

  fun cardViewToBitmap(cardView: MaterialCardView): Bitmap {
    val rect = Rect(0, 0, cardView.width, cardView.height)
    if (rect.width() == 0 || rect.height() == 0) {
      cardView.measure(
        View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
        View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
      )
      rect.set(0, 0, cardView.measuredWidth, cardView.measuredHeight)
    }
    val bitmap = createBitmap(rect.width(), rect.height())
    cardView.draw(Canvas(bitmap))
    return bitmap
  }

  fun share(context: Context, bitmap: Bitmap, text: String) {
    val uri = bitmap.toUri(context)
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