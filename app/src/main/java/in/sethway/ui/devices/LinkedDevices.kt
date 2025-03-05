package `in`.sethway.ui.devices

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import `in`.sethway.adapters.DevicesAdapter
import `in`.sethway.databinding.FragmentLinkedDevicesBinding
import `in`.sethway.protocol.Devices

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

    val clients = Devices.getClients()
    if (clients.length() == 0) {
      binding.emptyClientsHint.visibility = View.VISIBLE
      binding.listClients.adapter = DevicesAdapter(clients)
    } else {
      binding.emptyClientsHint.visibility = View.GONE
    }

    val sources = Devices.getSources()
    if (sources.length() == 0) {
      binding.emptyBroadcastersHint.visibility = View.VISIBLE
      binding.listBroadcasters.adapter = DevicesAdapter(sources)
    } else {
      binding.emptyBroadcastersHint.visibility = View.GONE
    }
  }

  override fun onDestroyView() {
    super.onDestroyView()
    _binding = null
  }

}