package `in`.sethway.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import `in`.sethway.R
import `in`.sethway.engine.group_old.Group
import org.json.JSONArray

class DevicesAdapter(
  private val peers: JSONArray,
  private val clickListener: (deviceId: String, deviceName: String) -> Unit
) :
  RecyclerView.Adapter<DevicesAdapter.ElementHolder>() {

  class ElementHolder(view: View) : RecyclerView.ViewHolder(view) {
    val deviceName: TextView = view.findViewById(R.id.deviceName)
    val broadcasterLabel: TextView = view.findViewById(R.id.broadcasterLabel)
  }

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ElementHolder {
    return ElementHolder(
      LayoutInflater.from(parent.context)
        .inflate(R.layout.item_device, parent, false)
    )
  }

  override fun getItemCount() = peers.length()

  override fun onBindViewHolder(holder: ElementHolder, position: Int) {
    val entry = peers.getJSONObject(position)
    val deviceName = entry.getString("device_name")
    val uuid = entry.getString("uuid")

    holder.deviceName.text = deviceName
    holder.itemView.setOnClickListener {
      clickListener(uuid, deviceName)
    }
    if (Group.getGroupCreator() == uuid) {
      holder.broadcasterLabel.visibility = View.VISIBLE
    } else {
      holder.broadcasterLabel.visibility = View.GONE
    }
  }

}