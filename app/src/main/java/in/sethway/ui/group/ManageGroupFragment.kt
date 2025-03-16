package `in`.sethway.ui.group

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import `in`.sethway.ui.adapters.DevicesAdapter
import `in`.sethway.databinding.FragmentManageGroupBinding
import `in`.sethway.engine.group_old.Group
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
    binding.groupName.text = Group.getGroupId()

    val testAdapter = DevicesAdapter(Group.getPeers().toArray()) { deviceId, deviceName ->
      // TODO: we need to handle this
    }

    binding.recycleView.apply {
      layoutManager = LinearLayoutManager(requireContext())
      adapter = testAdapter
      addItemDecoration(
        DividerItemDecoration(
          requireContext(),
          (layoutManager as LinearLayoutManager).orientation
        )
      )
    }
  }

  private fun JSONObject.toArray(): JSONArray {
    val array = JSONArray()
    for (key in keys()) {
      array.put(getJSONObject(key))
    }
    return array
  }

}