package `in`.sethway.ui.welcome

import android.Manifest
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import `in`.sethway.App
import `in`.sethway.App.Companion.GROUP
import `in`.sethway.R
import `in`.sethway.databinding.DialogPickNameBinding
import `in`.sethway.databinding.FragmentWelcomeBinding
import `in`.sethway.engine.SyncEngineService
import `in`.sethway.engine.group.Group
import `in`.sethway.ui.manage_notif.ManageNotificationPermissionFragment
import io.paperdb.Paper

class WelcomeFragment : Fragment() {

  private var _binding: FragmentWelcomeBinding? = null
  private val binding get() = _binding!!

  private var _dialogBinding: DialogPickNameBinding? = null
  private val dialogBinding get() = _dialogBinding!!

  private var alertDialog: AlertDialog? = null

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    if (GROUP.exists) {
      if (GROUP.amCreator && !ManageNotificationPermissionFragment.canManageNotifications(requireContext())) {
        findNavController().navigate(R.id.manageNotificationPermissionFragment)
      } else {
        findNavController().navigate(R.id.homeFragment)
      }
    }
    val serviceIntent = Intent(requireContext(), SyncEngineService::class.java)
    ContextCompat.startForegroundService(requireContext(), serviceIntent)
  }

  override fun onCreateView(
    inflater: LayoutInflater, container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View {
    _binding = FragmentWelcomeBinding.inflate(inflater)
    _dialogBinding = DialogPickNameBinding.inflate(inflater)
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
//    binding.editDeviceNameButton.apply {
//      text = App.DEVICE_NAME
//      setOnClickListener {
//        showChangeDeviceNameDialog()
//      }
//    }
  }

  private fun showChangeDeviceNameDialog() {
    (dialogBinding.root.parent as ViewGroup?)?.removeView(dialogBinding.root)
    dialogBinding.deviceNameEditText.let {
      it.text?.clear()
      it.addTextChangedListener(object : TextWatcher {
        override fun beforeTextChanged(cs: CharSequence?, x: Int, y: Int, z: Int) {}
        override fun afterTextChanged(s: Editable?) {}

        override fun onTextChanged(
          s: CharSequence,
          start: Int,
          before: Int,
          count: Int
        ) {
          dialogBinding.clearNameButton.visibility = if (s.isNotEmpty()) View.VISIBLE else View.GONE
        }
      })
    }
//    dialogBinding.deviceNameEditText.setText(App.DEVICE_NAME)
//    dialogBinding.continueButton.setOnClickListener {
//      alertDialog?.cancel()
//
//      val newDeviceName = dialogBinding.deviceNameEditText.text.toString()
//      App.setNewDeviceName(newDeviceName)
//      binding.editDeviceNameButton.text = newDeviceName
//    }

    alertDialog = MaterialAlertDialogBuilder(requireContext())
      .setView(dialogBinding.root)
      .show()
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