package `in`.sethway.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import `in`.sethway.R
import org.json.JSONArray

class DevicesAdapter(private val entries: JSONArray) :
  RecyclerView.Adapter<DevicesAdapter.ElementHolder>() {

  class ElementHolder(view: View) : RecyclerView.ViewHolder(view) {
    val deviceName: TextView = view.findViewById(R.id.deviceName)
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
    println("Setting device name $deviceName")
    holder.deviceName.text = deviceName
  }

}