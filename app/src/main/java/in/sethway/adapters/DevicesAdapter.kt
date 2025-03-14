package `in`.sethway.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import `in`.sethway.R
import org.json.JSONArray

class DevicesAdapter(
  private val entries: JSONArray,
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

  override fun getItemCount() = entries.length()

  override fun onBindViewHolder(holder: ElementHolder, position: Int) {
    val entry = entries.getJSONObject(position)
    val deviceName = entry.getString("device_name")
    holder.deviceName.text = deviceName
    holder.itemView.setOnClickListener {
      clickListener(entry.getString("id"), deviceName)
    }
    if (entry.getBoolean("broadcaster")) {
      holder.broadcasterLabel.visibility = View.VISIBLE
    } else {
      holder.broadcasterLabel.visibility = View.GONE
    }
  }

}