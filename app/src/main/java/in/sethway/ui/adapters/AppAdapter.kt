package `in`.sethway.ui.adapters

import android.annotation.SuppressLint
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

open class AppAdapterElement
class HeaderElement(val text: String) : AppAdapterElement()
class AppElement(val appName: String, val appIcon: Drawable, val packageName: String) :
  AppAdapterElement()

class AppAdapter(
  private val elements: List<AppAdapterElement>,
  private val toggleCallback: (AppElement, Boolean) -> Unit
) : RecyclerView.Adapter<AppAdapter.ElementContainer>() {

  private val checkboxStates = BooleanArray(elements.size)

  override fun getItemViewType(position: Int): Int {
    return if (elements[position] is HeaderElement) 0 else 1
  }

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ElementContainer {
    return ElementContainer(
      LayoutInflater.from(parent.context)
        .inflate(if (viewType == 0) R.layout.item_header else R.layout.item_app, parent, false)
    )
  }

  override fun onBindViewHolder(
    holder: ElementContainer,
    @SuppressLint("RecyclerView") position: Int
  ) {
    val element = elements[position]
    if (element is HeaderElement) {
      holder.appName.text = element.text
    } else {
      element as AppElement
      holder.appIcon.background = element.appIcon
      holder.appName.text = element.appName
      holder.appPackage.text = element.packageName

      holder.checkApp.setOnCheckedChangeListener(null)
      holder.checkApp.isChecked = checkboxStates[position]

      holder.checkApp.setOnCheckedChangeListener(object : CompoundButton.OnCheckedChangeListener {
        override fun onCheckedChanged(
          buttonView: CompoundButton?, isChecked: Boolean
        ) {
          checkboxStates[position] = isChecked
          toggleCallback(element, isChecked)
        }
      })
    }
  }

  override fun getItemCount(): Int = elements.size

  class ElementContainer(private val view: View) : RecyclerView.ViewHolder(view) {
    val appIcon: ImageView get() = view.findViewById(R.id.appIcon)
    val appName: TextView get() = view.findViewById(R.id.title)
    val appPackage: TextView get() = view.findViewById(R.id.packageName)
    val checkApp: MaterialSwitch get() = view.findViewById(R.id.enableSwitch)
  }
}

class MiniAppAdapter(context: Context, private val apps: List<AppElement>) :
  ArrayAdapter<AppAdapter.ElementContainer>(context, 0) {

  override fun getView(
    position: Int, convertView: View?, parent: ViewGroup
  ): View {
    val appIcon =
      convertView ?: LayoutInflater.from(context).inflate(R.layout.item_mini_app, parent, false)
    appIcon.background = apps[position].appIcon
    return appIcon
  }

  override fun getCount(): Int = apps.size

}