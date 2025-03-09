package `in`.sethway.ui.devices

import android.content.DialogInterface
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import `in`.sethway.R
import `in`.sethway.adapters.DevicesAdapter
import `in`.sethway.databinding.FragmentLinkedDevicesBinding
import `in`.sethway.protocol.Devices
import org.json.JSONArray
import org.json.JSONObject

class LinkedDevices : Fragment() {

  private var _binding: FragmentLinkedDevicesBinding? = null
  private val binding get() = _binding!!

  override fun onCreateView(
    inflater: LayoutInflater, container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View {
    _binding = FragmentLinkedDevicesBinding.inflate(layoutInflater)
    return binding.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    binding.apply {
      listClients.layoutManager = LinearLayoutManager(requireContext())
      listBroadcasters.layoutManager = LinearLayoutManager(requireContext())

      loadClients()
      loadSources()
    }
  }

  private fun loadClients() {
    val clients = Devices.getClients()
    if (clients.length() == 0) {
      binding.clientsHint.setText(R.string.hint_no_devices)
    } else {
      binding.clientsHint.setText(R.string.hint_yes_devices)
      binding.listClients.adapter =
        DevicesAdapter(clients.toJSONArray()) { deviceId: String, deviceName: String ->
          removeDeviceDialog(deviceName, "clients") { _, _ ->
            Devices.removeClient(deviceId)
            loadClients()
          }
        }
    }
  }

  private fun loadSources() {
    val sources = Devices.getSources()
    if (sources.length() == 0) {
      binding.broadcastersHint.setText(R.string.hint_no_devices)
    } else {
      binding.broadcastersHint.setText(R.string.hint_yes_devices)
      binding.listBroadcasters.adapter =
        DevicesAdapter(sources.toJSONArray()) { deviceId: String, deviceName: String ->
          removeDeviceDialog(deviceName, "broadcasters") { _, _ ->
            Devices.removeSource(deviceId)
            loadSources()
          }
        }
    }
  }

  private fun removeDeviceDialog(
    deviceName: String,
    group: String,
    positiveClick: DialogInterface.OnClickListener
  ) {
    MaterialAlertDialogBuilder(requireContext())
      .setTitle("Remove device")
      .setMessage("$deviceName will be removed from the $group list")
      .setPositiveButton("Yes", positiveClick)
      .setNegativeButton("Cancel") { dialog, _ ->
        dialog.dismiss()
      }
      .show()
  }

  private fun JSONObject.toJSONArray(): JSONArray {
    return this.toJSONArray(this.names()) ?: JSONArray()
  }

  override fun onDestroyView() {
    super.onDestroyView()
    _binding = null
  }

}