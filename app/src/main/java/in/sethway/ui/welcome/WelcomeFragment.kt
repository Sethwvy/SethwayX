package `in`.sethway.ui.welcome

import android.Manifest
import android.content.DialogInterface
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
import `in`.sethway.App
import `in`.sethway.R
import `in`.sethway.databinding.FragmentWelcomeBinding
import `in`.sethway.ui.manage_notif.ManageNotificationPermissionFragment

class WelcomeFragment : Fragment() {

  private var _binding: FragmentWelcomeBinding? = null
  private val binding get() = _binding!!

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    if (App.mmkv.getBoolean("welcome", false)) {
      if (App.mmkv.getBoolean("transmitter", false)
        && !ManageNotificationPermissionFragment.canManageNotifications(requireContext())
      ) {
        findNavController().navigate(R.id.manageNotificationPermissionFragment)
      } else {
        findNavController().navigate(R.id.homeFragment)
      }
    }
  }

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
    val title = "Notification permission"
    val message: String
    val onPositiveButton: DialogInterface.OnClickListener
    if (deniedByUser) {
      message = "As part of the app's core functionality, please allow the sending of notifications"
      onPositiveButton = DialogInterface.OnClickListener { _, _ ->
        // noinspection InlinedApi
        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
      }
    } else {
      message = "The permission has been denied by the system, please enable it in settings"
      onPositiveButton = DialogInterface.OnClickListener { _, _ ->
        // noinspection InlinedApi
        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        startActivity(
          Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
            putExtra(Settings.EXTRA_APP_PACKAGE, requireContext().packageName)
          }
        )
      }
    }
    MaterialAlertDialogBuilder(requireContext())
      .setTitle(title)
      .setMessage(message)
      .setCancelable(false)
      .setPositiveButton("Sure", onPositiveButton)
      .show()
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