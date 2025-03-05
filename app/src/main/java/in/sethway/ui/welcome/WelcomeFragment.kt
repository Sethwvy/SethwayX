package `in`.sethway.ui.welcome

import android.Manifest
import android.animation.ValueAnimator
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import `in`.sethway.R
import `in`.sethway.databinding.FragmentWelcomeBinding

class WelcomeFragment : Fragment() {

  private var _binding: FragmentWelcomeBinding? = null
  private val binding get() = _binding!!

  override fun onCreateView(
    inflater: LayoutInflater, container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View {
    _binding = FragmentWelcomeBinding.inflate(inflater)
    return binding.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    binding.startButton.setOnClickListener {
      if (canPostNotifications) {
        findNavController().navigate(R.id.pairFragment)
      } else {
        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
      }
    }
  }

  private val notificationPermissionLauncher =
    registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
      if (granted) {
        findNavController().navigate(R.id.pairFragment)
      } else {
        convinceUser()
      }
    }

  private fun convinceUser() {
    // noinspection InlinedApi
    val deniedByUser = ActivityCompat.shouldShowRequestPermissionRationale(
      requireActivity(), Manifest.permission.POST_NOTIFICATIONS
    )
    if (deniedByUser) {
      MaterialAlertDialogBuilder(requireContext())
        .setTitle("Notification permission")
        .setMessage("As part of the app's core functionality, please allow the sending of notifications")
        .setCancelable(false)
        .setPositiveButton("Sure") { _, _ ->
          // noinspection InlinedApi
          notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        .show()
    } else {
      MaterialAlertDialogBuilder(requireContext())
        .setTitle("Notification permission")
        .setMessage("The permission has been denied by the system, please enable it in settings")
        .setCancelable(false)
        .setPositiveButton("Sure") { _, _ ->
          // noinspection InlinedApi
          notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
          startActivity(
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
              putExtra(Settings.EXTRA_APP_PACKAGE, requireContext().packageName)
            }
          )
        }
        .show()
    }
  }

  private val canPostNotifications
    get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      ContextCompat.checkSelfPermission(
        requireContext(),
        Manifest.permission.POST_NOTIFICATIONS
      ) == PackageManager.PERMISSION_GRANTED
    } else true

  override fun onDestroyView() {
    super.onDestroyView()
    _binding = null
  }

}