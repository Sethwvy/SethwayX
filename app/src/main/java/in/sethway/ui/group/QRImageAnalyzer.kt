package `in`.sethway.ui.group

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.io.IOException

class QRImageAnalyzer(
  private val context: Context,
  private val callback: (qrContent: ByteArray) -> Unit
) : ImageAnalysis.Analyzer {

  companion object {
    private const val MIN_CALLBACK_INTERVAL = 5000
  }

  private var lastCallbackInvoked = 0L

  private val scanner = BarcodeScanning.getClient(
    BarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_QR_CODE).build()
  )

  @OptIn(ExperimentalGetImage::class)
  override fun analyze(image: ImageProxy) {
    val inputImage = InputImage.fromMediaImage(image.image!!, image.imageInfo.rotationDegrees)
    analyseInputImage(inputImage) { image.close() }
  }

  private fun analyseInputImage(inputImage: InputImage, onComplete: () -> Unit) {
    scanner.process(inputImage)
      .addOnSuccessListener { barcodes ->
        onBarcodeDecoded(barcodes)
      }
      .addOnFailureListener { exception ->
        Toast.makeText(
          context,
          "Could not decode QR Code ${exception::class.simpleName} ${exception.message}",
          Toast.LENGTH_LONG
        ).show()
      }
      .addOnCompleteListener {
        onComplete()
      }
  }

  private fun onBarcodeDecoded(barcodes: List<Barcode>) {
    val currTime = System.currentTimeMillis()
    if (barcodes.isNotEmpty() && currTime - lastCallbackInvoked > MIN_CALLBACK_INTERVAL) {
      lastCallbackInvoked = currTime
      barcodes[0].rawBytes?.let { callback(it) }
    }
  }


  fun decodeQRFromUri(uri: Uri) {
    val inputImage: InputImage
    try {
      inputImage = InputImage.fromFilePath(context, uri)
    } catch (e: IOException) {
      e.printStackTrace()
      Toast.makeText(context, "Could not open picked image file", Toast.LENGTH_LONG).show()
      return
    }
    analyseInputImage(inputImage) { }
  }
}