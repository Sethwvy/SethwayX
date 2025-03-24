package `in`.sethway.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import `in`.sethway.R
import org.json.JSONObject

class DevicesAdapter(
  private val peers: JSONObject,
  private val clickListener: (deviceId: String, deviceName: String) -> Unit
) :
  RecyclerView.Adapter<DevicesAdapter.ElementHolder>() {


  private val peerIds = peers.keys().asSequence().toList()

  class ElementHolder(view: View) : RecyclerView.ViewHolder(view) {
    val deviceName: TextView = view.findViewById(R.id.deviceName)
    val deviceSubtitle: TextView = view.findViewById(R.id.deviceSubtitle)
    val broadcasterIcon: TextView = view.findViewById(R.id.starIcon)
    val settingsButton: MaterialButton = view.findViewById(R.id.settingsButton)
  }

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ElementHolder {
    return ElementHolder(
      LayoutInflater.from(parent.context)
        .inflate(R.layout.item_device, parent, false)
    )
  }

  override fun getItemCount() = peers.length()

  override fun onBindViewHolder(holder: ElementHolder, position: Int) {
    val entry = JSONObject(peers.getString(peerIds[position]))
    val deviceName = entry.getString("display_name")
    val uuid = entry.getString("id")
    val broadcaster = entry.getBoolean("broadcaster")

    holder.deviceName.text = deviceName
    holder.deviceSubtitle.text = uuid
    holder.broadcasterIcon.visibility = if (broadcaster) View.VISIBLE else View.GONE

    holder.settingsButton.setOnClickListener {
      clickListener(uuid, deviceName)
    }
  }

}