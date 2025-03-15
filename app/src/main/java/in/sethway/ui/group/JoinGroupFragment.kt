package `in`.sethway.ui.group

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.navigation.fragment.findNavController
import `in`.sethway.R
import `in`.sethway.databinding.FragmentJoinGroupBinding
import `in`.sethway.engine.group.SimpleGroupSync
import org.json.JSONObject


class JoinGroupFragment : Fragment() {

  private var _binding: FragmentJoinGroupBinding? = null
  private val binding get() = _binding!!

  private var provider: ProcessCameraProvider? = null
  private lateinit var qrImageAnalyzer: QRImageAnalyzer

  private lateinit var groupSync: SimpleGroupSync

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    groupSync = SimpleGroupSync(
      weJoined = {
        // Yay :) We have joined the group1
        Toast.makeText(requireContext(), "Successfully joined the group!", Toast.LENGTH_LONG).show()
        findNavController().navigate(R.id.homeFragment)
      },
      someoneJoined = {}
    )

    qrImageAnalyzer = QRImageAnalyzer(requireContext()) { qrContent ->
      groupSync.connect(JSONObject(qrContent))
    }
  }

  override fun onCreateView(
    inflater: LayoutInflater, container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View {
    _binding = FragmentJoinGroupBinding.inflate(inflater)
    return binding.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    binding.scanCardView.setOnClickListener {
      if (canUseCamera && provider == null) {
        startPreview()
      } else {
        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
      }
    }
    binding.pickQRButton.setOnClickListener {
      mediaPicker.launch(PickVisualMediaRequest(PickVisualMedia.ImageOnly))
    }

    if (!attemptStartCamera()) {
      // camera permission not allowed
      cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
    }
  }

  private val mediaPicker =
    registerForActivityResult(PickVisualMedia()) { uri ->
      uri?.let { qrImageAnalyzer.decodeQRFromUri(it) }
    }

  private fun attemptStartCamera(): Boolean {
    binding.apply {
      if (canUseCamera) {
        cameraHint.visibility = View.GONE
        previewView.visibility = View.VISIBLE
        startPreview()
        return true
      } else {
        previewView.visibility = View.GONE
        cameraHint.visibility = View.VISIBLE
        return false
      }
    }
  }

  private fun startPreview() {
    val mainExecutor = ContextCompat.getMainExecutor(requireContext())
    val future = ProcessCameraProvider.getInstance(requireContext())
    future.addListener({
      provider = future.get()
      val preview = Preview.Builder()
        .build()
        .also { it.surfaceProvider = binding.previewView.surfaceProvider }

      try {
        provider?.unbindAll()
        provider?.bindToLifecycle(
          requireActivity(),
          CameraSelector.DEFAULT_BACK_CAMERA,
          preview,
          ImageAnalysis.Builder().build().also {
            it.setAnalyzer(mainExecutor, qrImageAnalyzer)
          }
        )
      } catch (e: Exception) {
        Toast.makeText(
          requireContext(),
          "Could not start camera ${e.javaClass.simpleName} ${e.message}",
          Toast.LENGTH_LONG
        ).show()
      }
    }, mainExecutor)
  }

  private val cameraPermissionLauncher =
    registerForActivityResult(ActivityResultContracts.RequestPermission()) { _ ->
      attemptStartCamera()
    }

  private val canUseCamera
    get() = ContextCompat.checkSelfPermission(
      requireContext(),
      Manifest.permission.CAMERA
    ) == PackageManager.PERMISSION_GRANTED

  override fun onResume() {
    super.onResume()
    attemptStartCamera()
  }

  override fun onDestroyView() {
    super.onDestroyView()
    _binding = null
    groupSync.close()
  }


}