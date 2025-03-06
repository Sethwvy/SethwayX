package `in`.sethway.ui.receive

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import `in`.sethway.App
import `in`.sethway.R
import `in`.sethway.databinding.FragmentReceiveBinding
import `in`.sethway.protocol.ClientHandshake
import `in`.sethway.protocol.QrImageAnalysis
import org.json.JSONObject

class ReceiveFragment : Fragment() {

  private var _binding: FragmentReceiveBinding? = null
  private val binding get() = _binding!!

  private lateinit var clientHandshake: ClientHandshake

  private lateinit var qrAnalysis: QrImageAnalysis
  private lateinit var provider: ProcessCameraProvider

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    clientHandshake = ClientHandshake(requireContext().contentResolver) { deviceName: String ->
      Toast.makeText(
        requireContext(),
        "$deviceName was connected successfully",
        Toast.LENGTH_LONG
      ).show()
      App.mmkv.encode("welcome", true)
      findNavController().navigate(R.id.receve_to_home)
    }
    qrAnalysis = QrImageAnalysis(requireContext()) { qrContent: String ->
      val json = JSONObject(qrContent)
      clientHandshake.connect(json.getJSONArray("addresses"))
    }
  }

  override fun onCreateView(
    inflater: LayoutInflater, container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View {
    _binding = FragmentReceiveBinding.inflate(inflater)
    return binding.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    binding.scanCard.setOnClickListener {
      if (!canUseCamera) {
        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
      }
    }
    if (!updateCameraLayout()) {
      // camera not allowed
      cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
    }
  }

  private fun updateCameraLayout(): Boolean {
    binding.apply {
      if (canUseCamera) {
        hintCamera.visibility = View.GONE
        cameraView.visibility = View.VISIBLE
        startPreview()
        return true
      } else {
        cameraView.visibility = View.GONE
        hintCamera.visibility = View.VISIBLE
        return false
      }
    }
  }

  private fun startPreview() {
    val future = ProcessCameraProvider.getInstance(requireContext())
    future.addListener({
      provider = future.get()
      val preview = Preview.Builder()
        .build()
        .also { it.surfaceProvider = binding.cameraView.surfaceProvider }

      try {
        provider.unbindAll()
        provider.bindToLifecycle(
          requireActivity(),
          CameraSelector.DEFAULT_BACK_CAMERA,
          preview,
          ImageAnalysis.Builder().build().also {
            it.setAnalyzer(ContextCompat.getMainExecutor(requireContext()), qrAnalysis)
          }
        )
      } catch (e: Exception) {
        Toast
          .makeText(requireContext(), "Camera error: ${e.message}", Toast.LENGTH_LONG)
          .show()
      }

    }, ContextCompat.getMainExecutor(requireContext()))
  }

  private val cameraPermissionLauncher =
    registerForActivityResult(ActivityResultContracts.RequestPermission()) { _ ->
      updateCameraLayout()
    }

  private val canUseCamera
    get() = ContextCompat.checkSelfPermission(
      requireContext(),
      Manifest.permission.CAMERA
    ) == PackageManager.PERMISSION_GRANTED

  override fun onResume() {
    super.onResume()
    updateCameraLayout()
  }

  override fun onDestroyView() {
    super.onDestroyView()
    _binding = null
  }

}