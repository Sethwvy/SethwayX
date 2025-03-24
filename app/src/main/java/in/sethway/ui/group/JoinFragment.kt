package `in`.sethway.ui.group

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.IBinder
import android.util.Log
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
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import `in`.sethway.R
import `in`.sethway.databinding.FragmentJoinBinding
import `in`.sethway.engine.SyncEngineService
import inx.sethway.IGroupCallback
import inx.sethway.IIPCEngine

class JoinFragment : Fragment(), ServiceConnection {

  companion object {
    private const val TAG = "JoinGroupFragment"
  }

  private var _binding: FragmentJoinBinding? = null
  private val binding get() = _binding!!

  private var provider: ProcessCameraProvider? = null
  private lateinit var qrImageAnalyzer: QRImageAnalyzer

  private var engineBinder: IIPCEngine? = null

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    qrImageAnalyzer = QRImageAnalyzer(requireContext()) { qrContent ->
      engineBinder?.acceptInvite(qrContent)
    }
  }

  override fun onCreateView(
    inflater: LayoutInflater, container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View {
    _binding = FragmentJoinBinding.inflate(inflater)
    return binding.root
  }

  override fun onStart() {
    super.onStart()
    val serviceIntent = Intent(requireContext(), SyncEngineService::class.java)
    requireContext().bindService(serviceIntent, this, Context.BIND_AUTO_CREATE)
  }

  override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
    Log.d(TAG, "onServiceConnected")
    engineBinder = IIPCEngine.Stub.asInterface(service)
    engineBinder?.registerGroupCallback(groupCallback)
  }

  private val groupCallback = object : IGroupCallback.Stub() {
    override fun onGroupJoinSuccess() {
      // We've successfully joined the group! Hurray!
      Log.d(TAG, "We've successfully joined the group!")
      requireActivity().runOnUiThread { findNavController().navigate(R.id.homeFragment) }
    }

    override fun onNewPeerConnected(commonInfo: String) {
      // This isn't for us
    }
  }

  override fun onServiceDisconnected(name: ComponentName?) {
    engineBinder = null
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
  }

  override fun onStop() {
    super.onStop()
    engineBinder?.unregisterGroupCallback()
    requireContext().unbindService(this)
  }
}