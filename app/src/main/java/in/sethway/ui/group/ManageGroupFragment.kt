package `in`.sethway.ui.group

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import `in`.sethway.App.Companion.GROUP
import `in`.sethway.R
import `in`.sethway.databinding.FragmentManageGroupBinding
import `in`.sethway.ui.Animation.withCardAnimation
import `in`.sethway.ui.adapters.DevicesAdapter
import org.json.JSONArray
import org.json.JSONObject


class ManageGroupFragment : Fragment() {

  private var _binding: FragmentManageGroupBinding? = null
  private val binding get() = _binding!!

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
  }

  override fun onCreateView(
    inflater: LayoutInflater, container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View {
    _binding = FragmentManageGroupBinding.inflate(inflater)
    return binding.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    binding.groupName.text = GROUP.groupId

    val devicesAdapter = DevicesAdapter(GROUP.getEachPeerCommonInfo()) { deviceId, deviceName ->
      // TODO: we need to handle this
    }

    binding.recycleView.apply {
      layoutManager = LinearLayoutManager(requireContext())
      adapter = devicesAdapter
      addItemDecoration(
        DividerItemDecoration(
          requireContext(),
          (layoutManager as LinearLayoutManager).orientation
        )
      )
    }

    withCardAnimation(binding.addDeviceCard) {
      findNavController().navigate(R.id.invitePeerFragment)
    }
  }


}