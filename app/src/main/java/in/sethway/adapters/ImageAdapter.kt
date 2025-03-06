package `in`.sethway.adapters

import android.content.Context
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import `in`.sethway.R

class ImageAdapter(
  private val context: Context,
  private val images: List<Drawable>
) : RecyclerView.Adapter<ViewHolder>() {

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
    return ViewHolder(
      LayoutInflater.from(context).inflate(R.layout.item_image, parent, false)
    )
  }

  override fun onBindViewHolder(holder: ViewHolder, position: Int) {
    holder.imageView.background = images[position]
  }

  override fun getItemCount() = images.size
}

class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
  val imageView: ImageView = view.findViewById(R.id.imageView)
}