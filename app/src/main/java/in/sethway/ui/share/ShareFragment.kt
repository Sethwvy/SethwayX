package `in`.sethway.ui.share

import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.github.alexzhirkevich.customqrgenerator.QrData
import com.github.alexzhirkevich.customqrgenerator.QrErrorCorrectionLevel
import com.github.alexzhirkevich.customqrgenerator.vector.QrCodeDrawable
import com.github.alexzhirkevich.customqrgenerator.vector.QrVectorOptions
import com.github.alexzhirkevich.customqrgenerator.vector.style.QrVectorBallShape
import com.github.alexzhirkevich.customqrgenerator.vector.style.QrVectorColor
import com.github.alexzhirkevich.customqrgenerator.vector.style.QrVectorColors
import com.github.alexzhirkevich.customqrgenerator.vector.style.QrVectorFrameShape
import com.github.alexzhirkevich.customqrgenerator.vector.style.QrVectorPixelShape
import com.github.alexzhirkevich.customqrgenerator.vector.style.QrVectorShapes
import `in`.sethway.App
import `in`.sethway.R
import `in`.sethway.databinding.FragmentShareBinding
import `in`.sethway.protocol.ServerHandshake
import `in`.sethway.protocol.Query
import `in`.sethway.ui.manage_notif.ManageNotificationPermissionFragment
import java.lang.IllegalStateException
import java.util.concurrent.atomic.AtomicBoolean

class ShareFragment : Fragment() {

  private var _binding: FragmentShareBinding? = null
  private val binding get() = _binding!!

  private val updaterRunning = AtomicBoolean(true)

  private val qrUpdater = object : Runnable {
    override fun run() {
      if (updaterRunning.get()) {
        try {
          displayQrDrawable(prepareQrDrawable(Query.shareSelf(requireContext().contentResolver)))
          Handler(Looper.getMainLooper()).postDelayed(this, 5000)
        } catch (ignored: IllegalStateException) {
          // can be safely ignored, happens when we call requireContext()
          // after the fragment has been destroyed
        }
      }
    }
  }

  private lateinit var serverHandshake: ServerHandshake

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    serverHandshake = ServerHandshake(requireContext().contentResolver) { deviceName: String ->
      Toast.makeText(
        requireContext(),
        "Successfully added $deviceName to receivers list",
        Toast.LENGTH_LONG
      ).show()
      App.mmkv.encode("welcome", true)
      App.mmkv.encode("transmitter", true)
      findNavController().navigate(
        if (ManageNotificationPermissionFragment.canManageNotifications(requireContext())) R.id.share_to_home
        else R.id.manageNotificationPermissionFragment
      )
    }
  }

  override fun onDestroy() {
    super.onDestroy()
    serverHandshake.close()
  }

  override fun onCreateView(
    inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
  ): View {
    _binding = FragmentShareBinding.inflate(inflater)
    return binding.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    binding.btnShareQR.setOnClickListener {
      val contentResolver = requireContext().contentResolver
      ShareUtils.share(
        requireContext(),
        prepareQrDrawable(Query.shareSelf(contentResolver)),
        "Connect to ${Query.deviceName(contentResolver)} by scanning this QR code"
      )
    }

    val drawable = prepareQrDrawable(Query.shareSelf(requireContext().contentResolver))
    displayQrDrawable(drawable)

    Handler(Looper.getMainLooper()).postDelayed(qrUpdater, 5000)
  }

  private fun displayQrDrawable(drawable: Drawable) {
    binding.apply {
      imageQR.visibility = View.VISIBLE
      imageQR.background = drawable
      qrProgress.visibility = View.GONE
    }
  }

  private fun prepareQrDrawable(text: String): Drawable {
    return QrCodeDrawable(
      QrData.Text(text),
      QrVectorOptions.Builder()
        .setColors(
          QrVectorColors(
            dark = QrVectorColor.Solid(getAttr(com.google.android.material.R.attr.colorPrimary)),
          )
        )
        .setShapes(
          QrVectorShapes(
            darkPixel = QrVectorPixelShape.Circle(),
            ball = QrVectorBallShape.Default,
            frame = QrVectorFrameShape.RoundCorners(.2f),
          )
        )
        .setPadding(0f)
        .setErrorCorrectionLevel(QrErrorCorrectionLevel.Low)
        .build()
    )
  }

  private fun getAttr(attrId: Int): Int {
    val typedValue = TypedValue()
    requireContext().theme.resolveAttribute(attrId, typedValue, true)
    return typedValue.data
  }

  override fun onDestroyView() {
    super.onDestroyView()
    _binding = null
    updaterRunning.set(false)
  }

}