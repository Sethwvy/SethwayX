package `in`.sethway.ui.devices

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
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

      val clients = Devices.getClients()
      if (clients.length() == 0) {
        clientsHint.setText(R.string.hint_no_devices)
      } else {
        clientsHint.setText(R.string.hint_yes_devices)
        listClients.adapter = DevicesAdapter(clients.toJSONArray())
      }

      val sources = Devices.getSources()
      if (sources.length() == 0) {
        broadcastersHint.setText(R.string.hint_no_devices)
      } else {
        broadcastersHint.setText(R.string.hint_yes_devices)
        listBroadcasters.adapter = DevicesAdapter(sources.toJSONArray())
      }
    }
  }

  private fun JSONObject.toJSONArray(): JSONArray {
    return this.toJSONArray(this.names()) ?: JSONArray()
  }

  override fun onDestroyView() {
    super.onDestroyView()
    _binding = null
  }

}