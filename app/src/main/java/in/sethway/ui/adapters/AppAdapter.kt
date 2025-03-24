package `in`.sethway.ui.adapters

import android.content.Context
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.CompoundButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.materialswitch.MaterialSwitch
import `in`.sethway.R

class AppInfo(val appName: String, val appIcon: Drawable, val packageName: String)

class AppAdapter(
  private val apps: List<AppInfo>,
  private val checkChangeCallback: (AppInfo, Boolean) -> Unit
) :
  RecyclerView.Adapter<AppAdapter.AppItem>() {

  private val checkboxStates = BooleanArray(apps.size)

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppItem {
    return AppItem(
      LayoutInflater.from(parent.context).inflate(R.layout.item_app, parent, false)
    )
  }

  override fun onBindViewHolder(holder: AppItem, position: Int) {
    val appItem = apps[position]
    holder.appIcon.background = appItem.appIcon
    holder.appNameTextView.text = appItem.appName
    holder.appPackageTextView.text = appItem.packageName

    holder.checkApp.setOnCheckedChangeListener(null)
    holder.checkApp.isChecked = checkboxStates[position]

    holder.checkApp.setOnCheckedChangeListener(object : CompoundButton.OnCheckedChangeListener {
      override fun onCheckedChanged(
        buttonView: CompoundButton?,
        isChecked: Boolean
      ) {
        checkboxStates[holder.bindingAdapterPosition] = isChecked
        checkChangeCallback(appItem, isChecked)
      }
    })
  }

  override fun getItemCount(): Int = apps.size

  class AppItem(view: View) : RecyclerView.ViewHolder(view) {
    val appIcon: ImageView = view.findViewById(R.id.appIcon)
    val appNameTextView: TextView = view.findViewById(R.id.appName)
    val appPackageTextView: TextView = view.findViewById(R.id.packageName)
    val checkApp: MaterialSwitch = view.findViewById(R.id.enableSwitch)
  }
}

class MiniAppAdapter(context: Context, private val apps: List<AppInfo>) :
  ArrayAdapter<AppAdapter.AppItem>(context, 0) {

  override fun getView(
    position: Int,
    convertView: View?,
    parent: ViewGroup
  ): View {
    val appIcon =
      convertView ?: LayoutInflater.from(context).inflate(R.layout.item_mini_app, parent, false)
    appIcon.background = apps[position].appIcon
    return appIcon
  }

  override fun getCount(): Int = apps.size

}