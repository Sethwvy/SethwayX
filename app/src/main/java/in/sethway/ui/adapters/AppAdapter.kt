package `in`.sethway.ui.adapters

import android.content.Context
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.CheckBox
import android.widget.CompoundButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import `in`.sethway.R
import me.zhanghai.android.fastscroll.PopupTextProvider

class AppInfo(val appName: String, val appIcon: Drawable, val packageName: String)

class AppAdapter(
  private val apps: List<AppInfo>,
  private val checkChangeCallback: (AppInfo, Boolean) -> Unit
) :
  RecyclerView.Adapter<AppAdapter.AppItem>(),
  PopupTextProvider {

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

    holder.checkApp.setOnCheckedChangeListener(object : CompoundButton.OnCheckedChangeListener {
      override fun onCheckedChanged(
        buttonView: CompoundButton?,
        isChecked: Boolean
      ) {
        checkChangeCallback(appItem, isChecked)
      }

    })
  }

  override fun getItemCount(): Int = apps.size

  override fun getPopupText(view: View, position: Int): CharSequence {
    val appName = apps[position].appName
    if (appName.length == 1) return appName
    val appNameChars = appName.substring(0, 2).uppercase().toCharArray()
    appNameChars[1] = appNameChars[1].lowercase()[0]
    return String(appNameChars)
  }

  class AppItem(view: View) : RecyclerView.ViewHolder(view) {
    val appIcon: ImageView = view.findViewById(R.id.appIcon)
    val appNameTextView: TextView = view.findViewById(R.id.appName)
    val appPackageTextView: TextView = view.findViewById(R.id.packageName)
    val checkApp: CheckBox = view.findViewById(R.id.checkApp)
  }
}

class AppIconAdapter(context: Context, private val apps: List<AppInfo>) :
  ArrayAdapter<AppAdapter.AppItem>(context, 0) {

  override fun getView(
    position: Int,
    convertView: View?,
    parent: ViewGroup
  ): View {
    val view =
      convertView ?: LayoutInflater.from(context).inflate(R.layout.item_app_icon, parent, false)
    view.findViewById<ImageView>(R.id.appIcon).background = apps[position].appIcon
    return view
  }

  override fun getCount(): Int = apps.size

}