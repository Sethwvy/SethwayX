package `in`.sethway

import android.app.Application
import com.github.f4b6a3.uuid.UuidCreator
import com.google.android.material.color.DynamicColors
import `in`.sethway.engine.Group
import io.paperdb.Book
import io.paperdb.Paper

class App : Application() {

  companion object {
    lateinit var ID: String
    lateinit var BOOK: Book

    // The UI must not directly change internal commits!
    // That should be done via commits. This object must only be used for
    // retrieving simple static properties
    lateinit var GROUP: Group
  }

  override fun onCreate() {
    super.onCreate()
    DynamicColors.applyToActivitiesIfAvailable(this)

    Paper.init(this)
    BOOK = Paper.book()

    var id: String? = BOOK.read("id")
    if (id == null) {
      id = UuidCreator.getTimeOrderedEpoch().toString()
      BOOK.write("id", id)
    }
    ID = id

    GROUP = Group(ID)
  }
}