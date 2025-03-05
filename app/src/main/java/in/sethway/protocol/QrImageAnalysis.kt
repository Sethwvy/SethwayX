package `in`.sethway.protocol

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage

class QrImageAnalysis(
  private val context: Context, private val callback: (qrContent: String) -> Unit
) : ImageAnalysis.Analyzer {

  companion object {
    private const val MIN_CALLBACK_INTERVAL = 5000
  }

  private val scanner = BarcodeScanning.getClient(
    BarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_QR_CODE).build()
  )

  private var lastCallbackInvoked = 0L

  @OptIn(ExperimentalGetImage::class)
  override fun analyze(p: ImageProxy) {
    val iImage = InputImage.fromMediaImage(p.image!!, p.imageInfo.rotationDegrees)
    scanner.process(iImage).addOnSuccessListener { barcodes ->
      handleBarcodeResult(barcodes)
    }.addOnFailureListener {
      Handler(Looper.getMainLooper()).post {
        Toast.makeText(context, "QR Scanning Failed: ${it.message}", Toast.LENGTH_LONG).show()
      }
    }.addOnCompleteListener {
      p.close()
    }
  }

  private fun handleBarcodeResult(barcodes: List<Barcode>) {
    if (System.currentTimeMillis() - lastCallbackInvoked >= MIN_CALLBACK_INTERVAL
      && barcodes.isNotEmpty()
    ) {
      lastCallbackInvoked
      val qrContent = barcodes[0].rawValue!!
      callback(qrContent)
    }
  }


}