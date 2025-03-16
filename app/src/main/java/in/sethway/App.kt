package `in`.sethway

import android.app.Application
import com.github.f4b6a3.uuid.UuidCreator
import com.google.android.material.color.DynamicColors
import io.paperdb.Paper

class App : Application() {

  companion object {
    lateinit var ID: String
  }

  override fun onCreate() {
    super.onCreate()
    DynamicColors.applyToActivitiesIfAvailable(this)

    Paper.init(this)
    val book = Paper.book()

    var id: String? = book.read("id")
    if (id == null) {
      id = UuidCreator.getTimeOrderedEpoch().toString()
      book.write("id", id)
    }
    ID = id
  }
}